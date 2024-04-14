package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WanderingTraderTimer {

    private final Map<ForceItemPlayer, Integer> startTimeMap;
    private final Map<ForceItemPlayer, BukkitRunnable> taskMap;
    private final Map<ForceItemPlayer, WanderingTrader> wanderingTraderMap;
    @Getter
    private int randomAfterStartSpawnTime;

    public WanderingTraderTimer() {
        this.startTimeMap = new HashMap<>();
        this.taskMap = new HashMap<>();
        this.wanderingTraderMap = new HashMap<>();
        this.randomAfterStartSpawnTime = (new Random().nextInt(4) + 7) * 60; //random number between 7 and 10 -> [7, 10]
    }

    public void startTimer(ForceItemPlayer forceItemPlayer) {
        if(!this.startTimeMap.containsKey(forceItemPlayer)) {
            this.startTimeMap.put(forceItemPlayer, randomAfterStartSpawnTime + 1);

            BukkitRunnable bukkitRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!ForceItemBattle.getInstance().getGamemanager().isMidGame()) {
                        return;
                    }
                    int elapsedSeconds = startTimeMap.get(forceItemPlayer);

                    if(elapsedSeconds == 0) {
                        spawnWanderingTrader(forceItemPlayer);
                        startTimeMap.put(forceItemPlayer, 5 * 60);
                    }

                    elapsedSeconds--;
                    startTimeMap.put(forceItemPlayer, elapsedSeconds);
                }
            };

            bukkitRunnable.runTaskTimer(ForceItemBattle.getInstance(), 0L, 20L);
            this.taskMap.put(forceItemPlayer, bukkitRunnable);
        }
    }

    public void resetTimer(ForceItemPlayer forceItemPlayer) {
        this.startTimeMap.remove(forceItemPlayer);
    }

    public void stopTimer(ForceItemPlayer forceItemPlayer) {
        this.startTimeMap.remove(forceItemPlayer);
        BukkitRunnable task = this.taskMap.get(forceItemPlayer);
        if (task != null) {
            task.cancel();
            this.taskMap.remove(forceItemPlayer);
        }
    }

    private void spawnWanderingTrader(ForceItemPlayer forceItemPlayer) {
        Location traderLocation = this.getRandomLocationWithinChunks(forceItemPlayer.player(), 5);
        WanderingTrader wanderingTrader = (WanderingTrader) forceItemPlayer.player().getWorld().spawnEntity(traderLocation, EntityType.WANDERING_TRADER);
        wanderingTrader.setGlowing(true);

        this.wanderingTraderMap.put(forceItemPlayer, wanderingTrader);

        forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Position <dark_gray>┃ <gray>Your <green>Wandering Trader <gray>just spawned at <dark_aqua>" + (int) traderLocation.getX() + "<gray>, <dark_aqua>" + (int) traderLocation.getY() + "<gray>, <dark_aqua>" + (int) traderLocation.getZ() + this.distance(forceItemPlayer.player().getLocation(), traderLocation)));
        ForceItemBattle.getInstance().getPositionManager().playParticleLine(forceItemPlayer.player(), traderLocation);

        BukkitRunnable despawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                wanderingTrader.remove();
                wanderingTraderMap.remove(forceItemPlayer);
                forceItemPlayer.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Position <dark_gray>┃ <gray>Your <green>Wandering Trader <gray>just despawned! :("));
                startTimeMap.put(forceItemPlayer, 10 * 60);

            }
        };
        despawnTask.runTaskLater(ForceItemBattle.getInstance(), 6000L); // 20 ticks per second, 5 minutes = 5 * 60 * 20 ticks
    }

    private String distance(Location playerLocation, Location destination) {
        if (playerLocation.getWorld() == null || destination.getWorld() == null) {
            return " <red>(unknown)";
        }

        if (!playerLocation.getWorld().equals(destination.getWorld())) {
            return " <gray>in the " + getWorldName(destination.getWorld());
        }


        return " <green>(" + (int) playerLocation.distance(destination) + " blocks away)";
    }

    private String getWorldName(World world) {
        if (world == null) {
            return "<dark_gray>unknown";
        }

        String worldName = world.getName();

        if (worldName.contains("nether")) {
            return "<red>nether";
        }

        if (worldName.contains("end")) {
            return "<yellow>end";
        }

        return "<green>overworld";
    }

    private Location getRandomLocationWithinChunks(Player player, int chunkRadius) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();

        double offsetX = (Math.random() - 0.5) * chunkRadius * 16 * 2;
        double offsetZ = (Math.random() - 0.5) * chunkRadius * 16 * 2;

        double newX = playerLocation.getX() + offsetX;
        double newZ = playerLocation.getZ() + offsetZ;

        double newY = world.getHighestBlockYAt((int)newX, (int)newZ) + 1;

        return new Location(world, newX, newY, newZ);
    }
}
