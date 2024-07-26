package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Locator;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LocatorManager {

    private final Map<String, Locator> locators;
    private final Map<String, Location> locatedStructures;

    private static final String prefix = "<dark_gray>» <dark_purple>Locator <dark_gray>┃ ";

    public LocatorManager() {
        this.locators = new HashMap<>();
        this.locatedStructures = new HashMap<>();

        this.addLocator(new Locator("fib:antimatter_depths", "Antimatter", Material.KNOWLEDGE_BOOK));
        this.addLocator(new Locator("trial_chambers", "Trial Chambers", Material.WITHER_ROSE));
    }

    private void addLocator(Locator locator) {
        this.locators.put(locator.getStructureId(), locator);
    }

    public void locate(String structureId, ForceItemPlayer forceItemPlayer) {
        Locator locator = this.locators.get(structureId);
        if(locator == null) {
            return;
        }

        if(!this.isInOverworld(forceItemPlayer.player())) {
            forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<red>There is no <dark_aqua>" + locator.getStructureName() + " <red>in the " + this.getCurrentWorld(forceItemPlayer.player()) + "<red>."));
            return;
        }

        @Nullable Structure structureKey = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).get(this.getNamespacedKey(locator.getStructureId()));

        if(structureKey == null) {
            forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return;
        }

        StructureSearchResult structureSearchResult = forceItemPlayer.player().getWorld().locateNearestStructure(
                forceItemPlayer.player().getLocation(),
                structureKey,
                20,
                false
        );

        if(structureSearchResult == null) {
            forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found."));
            return;
        }

        Location structureLocation = structureSearchResult.getLocation();
        if(!this.isAlreadyRevealed(locator.getStructureId(), structureLocation)) {
            this.destroyLocator(forceItemPlayer.player(), locator.getLocatorMaterial());
            forceItemPlayer.player().playSound(forceItemPlayer.player(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2, 1);
            new BukkitRunnable() {
                final BossBar bar = BossBar.bossBar(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(""), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
                @Override
                public void run() {
                    String bossBarTitle = "<gradient:#B314A8:#E775C3><b>" + locator.getStructureName() + " <reset><dark_gray>» " + locationToString(structureLocation) + distance(forceItemPlayer.player().getLocation(), structureLocation);
                    bar.name(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(bossBarTitle));
                    forceItemPlayer.player().showBossBar(bar);
                    ForceItemBattle.getInstance().getPositionManager().playParticleLine(forceItemPlayer.player(), structureSearchResult.getLocation(), Color.PURPLE);

                    if(forceItemPlayer.player().getLocation().distance(structureLocation) <= 50) {
                        forceItemPlayer.player().hideBossBar(bar);
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(ForceItemBattle.getInstance(), 0L, 300L);
        }

        ForceItemBattle.getInstance().getPositionManager().playParticleLine(forceItemPlayer.player(), structureSearchResult.getLocation(), Color.PURPLE);
        forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<dark_aqua>" + locator.getStructureName() + " <gray>located at " + locationToString(structureLocation) + distance(forceItemPlayer.player().getLocation(), structureLocation)));
        this.locatedStructures.put(locator.getStructureId(), structureLocation);
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
        if (playerLocation.getWorld() == null || destination.getWorld() == null) {
            return " <red>(unknown)";
        }

        return " <green>(" + (int) playerLocation.distance(destination) + " blocks away)";
    }

    private String getCurrentWorld(Player player) {
        if(player.getWorld().getName().equals("world_nether")) return "<dark_red>nether";
        else if(player.getWorld().getName().equals("world_the_end")) return "<dark_purple>end";
        return "overworld";
    }

    private boolean isInOverworld(Player player) {
        return player.getWorld().getName().equals("world");
    }

    private boolean isAlreadyRevealed(String structureId, Location location) {
        return location.equals(locatedStructures.get(structureId));
    }


    private void destroyLocator(Player player, Material material) {
        if(player.getInventory().getItemInMainHand().getType() != material) return;
        player.getInventory().setItemInMainHand(null);
    }

}
