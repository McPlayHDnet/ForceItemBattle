package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Locator;
import forceitembattle.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

public class LocatorManager implements Manager {

    private static final String prefix = "<dark_gray>» <dark_purple>Locator <dark_gray>┃ ";
    private static final int STRUCTURE_SEARCH_RADIUS = 20;  // chunks
    private static final int BIOME_SEARCH_RADIUS = 6400;    // blocks, matches vanilla /locate biome

    private final ForceItemBattle plugin;
    private final Map<String, Locator> locators;
    private final Map<String, Location> locatedStructures;
    private final Map<UUID, Map<String, ActiveLocator>> activeLocators;

    public LocatorManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.locators = new HashMap<>();
        this.locatedStructures = new HashMap<>();
        this.activeLocators = new HashMap<>();

        this.addLocator(new Locator("fib:antimatter_depths", "Antimatter", Material.KNOWLEDGE_BOOK, Locator.Type.STRUCTURE));
        this.addLocator(new Locator("trial_chambers", "Trial Chambers", Material.WITHER_ROSE, Locator.Type.STRUCTURE));
        this.addLocator(new Locator("sulfur_caves", "Sulfur Cave", Material.MUSIC_DISC_CHIRP, Locator.Type.BIOME));
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

        if (!this.isInOverworld(player)) {
            player.sendMessage(Text.of(prefix + "<red>There is no <dark_aqua>" + locator.getStructureName() + " <red>in the " + this.getCurrentWorld(player) + "<red>."));
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
            player.sendMessage(Text.of(prefix + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return null;
        }

        StructureSearchResult result = player.getWorld().locateNearestStructure(
                player.getLocation(),
                structure,
                STRUCTURE_SEARCH_RADIUS,
                false
        );

        if (result == null) {
            player.sendMessage(Text.of(prefix + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found."));
            return null;
        }

        return result.getLocation();
    }

    @Nullable
    private Location locateBiome(Locator locator, Player player) {
        @Nullable Biome biome = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .get(this.getNamespacedKey(locator.getStructureId()));

        if (biome == null) {
            player.sendMessage(Text.of(prefix + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return null;
        }

        BiomeSearchResult result = player.getWorld().locateNearestBiome(
                player.getLocation(),
                BIOME_SEARCH_RADIUS,
                biome
        );

        if (result == null) {
            player.sendMessage(Text.of(prefix + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found nearby."));
            return null;
        }

        return result.getLocation();
    }

    private void reveal(Locator locator, Player player, Location targetLocation) {
        if (!this.isAlreadyRevealed(locator.getStructureId(), targetLocation)) {
            this.destroyLocator(player, locator.getLocatorMaterial());
            player.playSound(player, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2, 1);
            this.startLocatorSession(locator, player, targetLocation);
        }

        this.plugin.getPositionManager().playParticleLine(player, targetLocation, Color.PURPLE);
        player.sendMessage(Text.of(prefix + "<dark_aqua>" + locator.getStructureName() + " <gray>located at " + locationToString(targetLocation) + distance(player.getLocation(), targetLocation)));
        this.locatedStructures.put(locator.getStructureId(), targetLocation);
    }

    private void startLocatorSession(Locator locator, Player player, Location targetLocation) {
        UUID playerId = player.getUniqueId();

        // Drop any previous session for this exact locator before opening a new one.
        this.clearLocator(playerId, locator.getStructureId());

        BossBar bar = BossBar.bossBar(Text.of(""), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                String bossBarTitle = "<gradient:#B314A8:#E775C3><b>" + locator.getStructureName() + " <reset><dark_gray>» " + locationToString(targetLocation) + distance(player.getLocation(), targetLocation);
                bar.name(Text.of(bossBarTitle));
                player.showBossBar(bar);
                LocatorManager.this.plugin.getPositionManager().playParticleLine(player, targetLocation, Color.PURPLE);

                if (player.getWorld() == targetLocation.getWorld()
                        && player.getLocation().distance(targetLocation) <= 50) {
                    LocatorManager.this.clearLocator(playerId, locator.getStructureId());
                }
            }
        };

        synchronized (this.activeLocators) {
            this.activeLocators
                    .computeIfAbsent(playerId, uuid -> new LinkedHashMap<>())
                    .put(locator.getStructureId(), new ActiveLocator(bar, task));
        }

        task.runTaskTimerAsynchronously(this.plugin, 0L, 300L);
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

    // Dismisses a single active locator for a player by its structure id.
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

    private NamespacedKey getNamespacedKey(String structureId) {
        return structureId.contains("fib:") ? NamespacedKey.fromString(structureId) : NamespacedKey.minecraft(structureId);
    }

    private String locationToString(Location location) {
        if (location.getWorld() == null) {
            return "<red>unknown location";
        }

        return "<dark_aqua>" + location.getBlockX() + "<gray>, <dark_aqua>?<gray>, <dark_aqua>" + location.getBlockZ();
    }

    private String distance(Location playerLocation, Location destination) {
        if (playerLocation.getWorld() == null || destination.getWorld() == null
                || playerLocation.getWorld() != destination.getWorld()) {
            return " <red>(unknown)";
        }

        return " <green>(" + (int) playerLocation.distance(destination) + " blocks away)";
    }

    private String getCurrentWorld(Player player) {
        if (player.getWorld().getName().equals("world_nether")) return "<dark_red>nether";
        else if (player.getWorld().getName().equals("world_the_end")) return "<dark_purple>end";
        return "overworld";
    }

    private boolean isInOverworld(Player player) {
        return player.getWorld().getName().equals("world");
    }

    private boolean isAlreadyRevealed(String structureId, Location location) {
        return location.equals(locatedStructures.get(structureId));
    }

    private void destroyLocator(Player player, Material material) {
        if (player.getInventory().getItemInMainHand().getType() != material) return;
        player.getInventory().setItemInMainHand(null);
    }

    private record ActiveLocator(BossBar bossBar, BukkitRunnable task) {

        void cancelAndHide(@Nullable Player player) {
            this.task.cancel();
            if (player != null) {
                player.hideBossBar(this.bossBar);
            }
        }
    }
}
