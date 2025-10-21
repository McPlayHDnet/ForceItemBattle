package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Setter
@Getter
public class WanderingTraderTimer {

    private int randomAfterStartSpawnTime, timer, traderTimer;

    private final Map<UUID, Boolean> canBuyWheel;

    public WanderingTraderTimer() {
        this.randomAfterStartSpawnTime = (new Random().nextInt(4) + 7) * 60; //random number between 7 and 10 -> [7, 10]
        this.timer += this.randomAfterStartSpawnTime + 1;

        this.canBuyWheel = new HashMap<>();
    }

    public void startTimer() {
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!ForceItemBattle.getInstance().getGamemanager().isMidGame()) {
                    return;
                }
                int elapsedSeconds = timer;

                if(elapsedSeconds == 0) {
                    spawnWanderingTrader();
                }

                elapsedSeconds--;
                timer = elapsedSeconds;
            }
        };

        bukkitRunnable.runTaskTimer(ForceItemBattle.getInstance(), 0L, 20L);
    }

    public void spawnWanderingTrader() {
        World world = Bukkit.getWorld("world");
        Location traderLocation = this.getRandomLocationWithinSpawnChunks(world.getSpawnLocation(), 5);
        WanderingTrader wanderingTrader = (WanderingTrader) world.spawnEntity(traderLocation.add(0.0, 1.0, 0.0), EntityType.WANDERING_TRADER);
        wanderingTrader.setGlowing(true);
        wanderingTrader.setInvulnerable(true);
        wanderingTrader.setAI(false);
        wanderingTrader.setGravity(true);
        List<MerchantRecipe> merchantRecipes = new ArrayList<>(wanderingTrader.getRecipes());

        merchantRecipes.forEach(merchantReciper -> {
            List<ItemStack> ingredients = merchantReciper.getIngredients();
            ingredients.forEach(ingredient -> ingredient.setAmount(1));
            merchantReciper.setIngredients(ingredients);
            merchantReciper.setMaxUses(Integer.MAX_VALUE);
        });
        ItemStack wheelOfFortune = new ItemBuilder(Material.NETHER_STAR).setDisplayName("<yellow><b>Wheel of Fortune").getItemStack();
        wheelOfFortune.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addString("wheel")
                .build());

        MerchantRecipe merchantRecipe = new MerchantRecipe(wheelOfFortune, Integer.MAX_VALUE);
        merchantRecipe.addIngredient(new ItemStack(Material.EMERALD, 1));
        merchantRecipes.add(merchantRecipe);

        wanderingTrader.setRecipes(merchantRecipes);

        this.canBuyWheel.clear();
        Map<UUID, BossBar> playerBossBars = new HashMap<>();
        ForceItemBattle.getInstance().getGamemanager().forceItemPlayerMap().values().forEach(players -> {
            this.canBuyWheel.put(players.player().getUniqueId(), Boolean.TRUE);

            players.player().sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Position <dark_gray>┃ <gray>The <green>Wandering Trader <gray>just spawned at <dark_aqua>" + (int) traderLocation.getX() + "<gray>, <dark_aqua>" + (int) traderLocation.getY() + "<gray>, <dark_aqua>" + (int) traderLocation.getZ() + this.distance(players.player().getLocation(), traderLocation)));
            ForceItemBattle.getInstance().getPositionManager().playParticleLine(players.player(), traderLocation, Color.LIME);
        });

        traderTimer = 5 * 60;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (traderTimer <= 0 || wanderingTrader.isDead()) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendPlayerListFooter(Component.empty());
                    });
                    wanderingTrader.remove();
                    cancel();
                    Bukkit.broadcast(ForceItemBattle.getInstance().getGamemanager().getMiniMessage()
                            .deserialize("<dark_gray>» <gold>Position <dark_gray>┃ <gray>The <green>Wandering Trader <gray>just despawned! :("));
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    String footerText = "\n<green><b>Wandering Trader</b>\n"
                            + locationToString(traderLocation) + "\n"
                            + formatColoredTime(traderTimer) + "\n";
                    Component footer = ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(footerText);
                    player.sendPlayerListFooter(footer);
                });

                traderTimer--;
            }
        }.runTaskTimer(ForceItemBattle.getInstance(), 0L, 20L);
    }

    private String locationToString(Location location) {
        if (location.getWorld() == null) {
            return "<red>unknown location";
        }

        return "<dark_aqua>" + location.getBlockX() + "<gray>, <dark_aqua>" + location.getBlockY() + "<gray>, <dark_aqua>" + location.getBlockZ();
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

    private String formatColoredTime(int remainingSeconds) {
        remainingSeconds = Math.max(remainingSeconds, 0);

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        String timeString = String.format("%02d:%02d", minutes, seconds);

        String colorTag;

        if (remainingSeconds <= 10) {
            colorTag = "<dark_red>";
        } else if (remainingSeconds <= 30) {
            colorTag = "<red>";
        } else if (remainingSeconds <= 120) {
            colorTag = "<gold>";
        } else {
            colorTag = "<green>";
        }

        return colorTag + timeString;
    }

    private Location getRandomLocationWithinSpawnChunks(Location location, int chunkRadius) {
        World world = location.getWorld();

        double offsetX = (Math.random() - 0.5) * chunkRadius * 16 * 2;
        double offsetZ = (Math.random() - 0.5) * chunkRadius * 16 * 2;

        double newX = location.getX() + offsetX;
        double newZ = location.getZ() + offsetZ;

        double newY = world.getHighestBlockYAt((int)newX, (int)newZ);

        return new Location(world, newX, newY, newZ);

    }
}
