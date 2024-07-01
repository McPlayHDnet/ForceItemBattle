package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AntimatterLocator {

    private final List<Location> locatedStructures;

    private static final String prefix = "<dark_gray>» <dark_purple>Locator <dark_gray>┃ ";

    public AntimatterLocator() {
        this.locatedStructures = new ArrayList<>();
    }

    public void locateAntimatter(ForceItemPlayer forceItemPlayer, ItemStack locator) {
        if(!this.isInOverworld(forceItemPlayer.player())) {
            forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<red>There is no <dark_aqua>Antimatter <red>in the " + this.getCurrentWorld(forceItemPlayer.player()) + "<red>."));
            return;
        }

        StructureSearchResult structureSearchResult = forceItemPlayer.player().getWorld().locateNearestStructure(forceItemPlayer.player().getLocation(), Objects.requireNonNull(Registry.STRUCTURE.get(Objects.requireNonNull(NamespacedKey.fromString("fib:antimatter_depths")))), 20, false);

        if(structureSearchResult == null) {
            forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<dark_aqua>Antimatter <red>could not be found."));
            return;
        }

        Location structureLocation = structureSearchResult.getLocation();
        if(!this.isAlreadyRevealed(structureLocation)) {
            this.destroyLocator(forceItemPlayer.player(), locator);
            forceItemPlayer.player().playSound(forceItemPlayer.player(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2, 1);
            new BukkitRunnable() {
                final BossBar bar = BossBar.bossBar(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(""), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
                @Override
                public void run() {
                    String bossBarTitle = "<gradient:#B314A8:#E775C3><b>Antimatter <reset><dark_gray>» " + locationToString(structureLocation) + distance(forceItemPlayer.player().getLocation(), structureLocation);
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

        forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(prefix + "<dark_aqua>Antimatter <gray>located at " + locationToString(structureLocation) + distance(forceItemPlayer.player().getLocation(), structureLocation)));

        this.locatedStructures.add(structureLocation);
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

    private boolean isAlreadyRevealed(Location location) {
        return this.locatedStructures.contains(location);
    }

    private void destroyLocator(Player player, ItemStack locator) {
        ItemStack copy = locator.clone();
        copy.setAmount(1);
        player.getInventory().removeItem(locator);
    }

}
