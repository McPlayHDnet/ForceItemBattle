package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Locator;
import forceitembattle.util.BiomeSearch;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

public class LocatorManager implements Manager {

    private static final int STRUCTURE_SEARCH_RADIUS = 20;  // chunks

    private final ForceItemBattle plugin;
    private final Map<String, Locator> locators;
    private final Map<String, Location> locatedStructures;
    private final Map<UUID, Map<String, ActiveLocator>> activeLocators;

    public LocatorManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.locators = new HashMap<>();
        this.locatedStructures = new HashMap<>();
        this.activeLocators = new HashMap<>();

        this.addLocator(new Locator("fib:antimatter_depths", "Antimatter", CustomMaterials.ANTIMATTER_LOCATOR, Locator.Type.STRUCTURE,
                Color.PURPLE, "#B314A8:#E775C3"));
        this.addLocator(new Locator("trial_chambers", "Trial Chambers", CustomMaterials.TRIAL_LOCATOR, Locator.Type.STRUCTURE,
                Color.fromRGB(0x4F, 0xB4, 0x93), "#2E7D68:#7FD8BC"));
        this.addLocator(new Locator("sulfur_caves", "Sulfur Cave", CustomMaterials.SULFUR_LOCATOR, Locator.Type.BIOME,
                Color.YELLOW, "#C7A500:#FFF27E"));
    }

    @Override
    public void disable() {
        synchronized (this.activeLocators) {
            this.activeLocators.forEach((playerId, byStructure) -> {
                Player player = this.plugin.getServer().getPlayer(playerId);
                byStructure.values().forEach(active -> active.cancelAndHide(player));
            });
            this.activeLocators.clear();
        }
    }

    private void addLocator(Locator locator) {
        this.locators.put(locator.getStructureId(), locator);
    }

    public void locate(String structureId, ForceItemPlayer forceItemPlayer) {
        Locator locator = this.locators.get(structureId);
        if (locator == null) {
            return;
        }

        Player player = forceItemPlayer.player();

        if (!Dimension.isOverworld(player)) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<red>There is no <dark_aqua>" + locator.getStructureName()
                    + " <red>in the " + Dimension.of(player).coloredName() + "<red>."));
            return;
        }

        Location targetLocation = switch (locator.getType()) {
            case STRUCTURE -> this.locateStructure(locator, player);
            case BIOME -> this.locateBiome(locator, player);
        };

        if (targetLocation == null) {
            return; // the locate* helpers already messaged the player
        }

        this.reveal(locator, player, targetLocation);
    }

    @Nullable
    private Location locateStructure(Locator locator, Player player) {
        @Nullable Structure structure = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.STRUCTURE)
                .get(this.getNamespacedKey(locator.getStructureId()));

        if (structure == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return null;
        }

        StructureSearchResult result = player.getWorld().locateNearestStructure(
                player.getLocation(),
                structure,
                STRUCTURE_SEARCH_RADIUS,
                false
        );

        if (result == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found."));
            return null;
        }

        return result.getLocation();
    }

    @Nullable
    private Location locateBiome(Locator locator, Player player) {
        Biome biome = BiomeSearch.resolve(this.getNamespacedKey(locator.getStructureId()));

        if (biome == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return null;
        }

        Location targetLocation = BiomeSearch.nearest(player.getLocation(), biome);

        if (targetLocation == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found nearby."));
            return null;
        }

        return targetLocation;
    }

    private void reveal(Locator locator, Player player, Location targetLocation) {
        // Resolved here on the main thread; the async session task must not touch the world.
        Location digSpot = this.surfaceDigSpot(targetLocation);

        if (!this.isAlreadyRevealed(locator.getStructureId(), targetLocation)) {
            this.destroyLocator(player, locator.getLocatorMaterial());
            player.playSound(player, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2, 1);
            this.startLocatorSession(locator, player, targetLocation, digSpot);
        }

        this.plugin.getPositionManager().playParticleLine(player, targetLocation, locator.getLineColor());
        this.plugin.getPositionManager().playSurfaceMarker(player, digSpot, locator.getLineColor());
        player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <gray>located at "
                + LocationFormat.xz(targetLocation)
                + LocationFormat.distance(player.getLocation(), targetLocation)));
        this.locatedStructures.put(locator.getStructureId(), targetLocation);
    }

    // The surface block above the target, i.e. where the player has to dig down.
    private Location surfaceDigSpot(Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null) {
            return targetLocation;
        }
        return new Location(world,
                targetLocation.getBlockX() + 0.5,
                world.getHighestBlockYAt(targetLocation.getBlockX(), targetLocation.getBlockZ()) + 1,
                targetLocation.getBlockZ() + 0.5);
    }

    private void startLocatorSession(Locator locator, Player player, Location targetLocation, Location digSpot) {
        UUID playerId = player.getUniqueId();

        // Drop any previous session for this exact locator before opening a new one.
        this.clearLocator(playerId, locator.getStructureId());

        BossBar bar = BossBar.bossBar(Text.of(""), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);

        BukkitRunnable task = new BukkitRunnable() {
            /** Task runs between particle line redraws; the boss bar itself updates every run. */
            private static final int RUNS_PER_LINE = 30;
            private int runs = 0;

            @Override
            public void run() {
                String bossBarTitle = "<gradient:" + locator.getBossBarGradient() + "><b>" + locator.getStructureName() + " <reset><dark_gray>» "
                        + LocationFormat.xz(targetLocation)
                        + LocationFormat.distance(player.getLocation(), targetLocation);
                bar.name(Text.of(bossBarTitle));
                player.showBossBar(bar);

                if (this.runs++ % RUNS_PER_LINE == 0) {
                    LocatorManager.this.plugin.getPositionManager().playParticleLine(player, targetLocation, locator.getLineColor());
                }

                if (player.getWorld() == targetLocation.getWorld()
                        && player.getLocation().distance(targetLocation) <= 50) {
                    LocatorManager.this.clearLocator(playerId, locator.getStructureId());
                }
            }
        };

        // Persistent dig-spot marker, cancelled together with the session.
        BukkitRunnable markerTask = this.plugin.getPositionManager().startSurfaceMarker(player, digSpot, locator.getLineColor());

        synchronized (this.activeLocators) {
            this.activeLocators
                    .computeIfAbsent(playerId, uuid -> new LinkedHashMap<>())
                    .put(locator.getStructureId(), new ActiveLocator(bar, task, markerTask));
        }

        task.runTaskTimerAsynchronously(this.plugin, 0L, 10L);
    }

    // Removes a session from the tracking map without touching the boss bar.
    @Nullable
    private ActiveLocator removeSession(UUID playerId, String structureId) {
        synchronized (this.activeLocators) {
            Map<String, ActiveLocator> byStructure = this.activeLocators.get(playerId);
            if (byStructure == null) {
                return null;
            }
            ActiveLocator active = byStructure.remove(structureId);
            if (byStructure.isEmpty()) {
                this.activeLocators.remove(playerId);
            }
            return active;
        }
    }

    // Cancels and hides one active locator session for a player, if present.
    private void clearLocator(UUID playerId, String structureId) {
        ActiveLocator active = this.removeSession(playerId, structureId);
        if (active != null) {
            active.cancelAndHide(this.plugin.getServer().getPlayer(playerId));
        }
    }

    public boolean dismiss(Player player, String structureId) {
        ActiveLocator active = this.removeSession(player.getUniqueId(), structureId);
        if (active == null) {
            return false;
        }
        active.cancelAndHide(player);
        return true;
    }

    // Returns the player's active locators, keyed by structure id, in the order they were located.
    public Map<String, Locator> getActiveLocators(Player player) {
        Map<String, Locator> result = new LinkedHashMap<>();
        synchronized (this.activeLocators) {
            Map<String, ActiveLocator> byStructure = this.activeLocators.get(player.getUniqueId());
            if (byStructure == null) {
                return result;
            }
            for (String structureId : byStructure.keySet()) {
                Locator locator = this.locators.get(structureId);
                if (locator != null) {
                    result.put(structureId, locator);
                }
            }
        }
        return result;
    }

    public int dismissAll(Player player) {
        Map<String, ActiveLocator> byStructure;
        synchronized (this.activeLocators) {
            byStructure = this.activeLocators.remove(player.getUniqueId());
        }
        if (byStructure == null || byStructure.isEmpty()) {
            return 0;
        }
        byStructure.values().forEach(active -> active.cancelAndHide(player));
        return byStructure.size();
    }

    public Locator getLocatorByMaterial(Material material) {
        return this.locators.values().stream()
                .filter(locator -> locator.getLocatorMaterial() == material)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public Locator randomLocator() {
        List<Locator> all = List.copyOf(this.locators.values());
        return all.isEmpty() ? null : all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    private NamespacedKey getNamespacedKey(String structureId) {
        return structureId.contains("fib:") ? NamespacedKey.fromString(structureId) : NamespacedKey.minecraft(structureId);
    }

    private boolean isAlreadyRevealed(String structureId, Location location) {
        return location.equals(locatedStructures.get(structureId));
    }

    private void destroyLocator(Player player, Material material) {
        if (player.getInventory().getItemInMainHand().getType() != material) return;
        player.getInventory().setItemInMainHand(null);
    }

    private record ActiveLocator(BossBar bossBar, BukkitRunnable task, @Nullable BukkitRunnable markerTask) {

        void cancelAndHide(@Nullable Player player) {
            this.task.cancel();
            if (this.markerTask != null && !this.markerTask.isCancelled()) {
                this.markerTask.cancel();
            }
            if (player != null) {
                player.hideBossBar(this.bossBar);
            }
        }
    }
}
