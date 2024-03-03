package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.GameState;
import forceitembattle.util.ItemBuilder;
import forceitembattle.util.PlayerStat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandStart implements CommandExecutor {

    private final ForceItemBattle plugin;

    public CommandStart(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("start").setTabCompleter(new TabCompletion(plugin));
        this.plugin.getCommand("start").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length == 1) {
            if (this.plugin.getSettings().getGamePreset(args[0]) == null) {
                player.sendMessage("§e" + args[0] + " §cdoes not exist in presets.");
                return false;
            }

            GamePreset gamePreset = this.plugin.getSettings().getGamePreset(args[0]);
            this.plugin.getGamemanager().setCurrentGamePreset(gamePreset);
            this.performCommand(gamePreset, player, args);

        } else if (args.length == 2) {
            try {
                this.performCommand(null, player, args);

            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <getJokers>");
                sender.sendMessage(ChatColor.RED + "<time> and <getJokers> have to be numbers");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <getJokers>");
        }

        return false;
    }

    private void performCommand(GamePreset gamePreset, Player player, String[] args) {
        int durationMinutes = (gamePreset != null ? gamePreset.getCountdown() : Integer.parseInt(args[0]));
        int countdown = durationMinutes * 60;
        int jokersAmount = (gamePreset != null ? gamePreset.getJokers() : (Integer.parseInt(args[1])));
        this.plugin.getTimer().setTime(countdown);
        this.plugin.getGamemanager().initializeMats();

        if (gamePreset == null && (Integer.parseInt(args[1]) > 64)) {
            player.sendMessage(ChatColor.RED + "The maximum amount of getJokers is 64.");
            return;
        }

        new BukkitRunnable() {
            int seconds = 11;

            @Override
            public void run() {
                seconds--;
                if (seconds == 0) {
                    cancel();

                    startGame(durationMinutes, jokersAmount);
                    return;
                }
                if (seconds < 6) {
                    Bukkit.getOnlinePlayers().forEach(
                            players -> players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1)
                    );
                }

                String subtitle = getSubtitle();
                Bukkit.getOnlinePlayers().forEach(
                        players -> players.sendTitle("§a" + seconds, subtitle, 0, 20, 10)
                );
            }

            private String getSubtitle() {
                String subTitle = "";

                switch (seconds) {
                    case 9, 8 -> subTitle = "§f» §6" + (plugin.getTimer().getTime() / 60) + " minutes §f«";
                    case 7, 6 -> subTitle = "§f» §6" + jokersAmount + " Joker §f«";
                    case 5 -> subTitle = "§f» §6/info & /infowiki §f«";
                    case 4 -> subTitle = "§f» §6/spawn & /bed §f«";
                    case 3, 2 -> subTitle = "§f» §6Collect as many items as possible §f«";
                    case 1 -> subTitle = "§f» §6Have fun! §f«";
                }

                return subTitle;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private void startGame(int timeMinutes, int jokersAmount) {
        this.plugin.getPositionManager().clearPositions();

        World world = Bukkit.getWorld("world");
        assert world != null;
        Location spawnLocation = world.getSpawnLocation();
        setupSpawnLocation(spawnLocation);

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);

        Bukkit.getOnlinePlayers().forEach(player -> {

            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            forceItemPlayer.setRemainingJokers(jokersAmount);


            player.sendMessage(" ");
            player.sendMessage("§8» §6§lMystery Item Battle §8«");
            player.sendMessage(" ");
            player.sendMessage("  §8● §7Duration §8» §a" + timeMinutes + " minutes");
            player.sendMessage("  §8● §7Joker §8» §a" + jokersAmount);
            for (GameSetting gameSettings : GameSetting.values()) {
                player.sendMessage("  §8● §7" + gameSettings.displayName() + " §8» §a" + (this.plugin.getSettings().isSettingEnabled(gameSettings) ? "§2✔" : "§4✘"));
            }
            player.sendMessage(" ");
            player.sendMessage(" §8● §7Useful Commands:");
            player.sendMessage("  §8» §6/info");
            player.sendMessage("  §8» §6/infowiki");
            player.sendMessage("  §8» §6/spawn");
            player.sendMessage("  §8» §6/bed");
            player.sendMessage("  §8» §6/pos");
            player.sendMessage("");

            player.setHealth(20);
            player.setSaturation(20);
            player.getInventory().clear();
            player.getInventory().setItem(4, Gamemanager.getJokers(jokersAmount));

            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 16));
            player.getInventory().addItem(new ItemBuilder(Material.ELYTRA).setUnbreakable(true).getItemStack());

            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
            player.getPassengers().forEach(Entity::remove);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(spawnLocation);
            player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                this.plugin.getBackpack().createBackpack(player);
            }

            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                forceItemPlayer.createItemDisplay();
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.GAMES_PLAYED, this.plugin.getStatsManager().playerStats(player.getName()), 1);
            }


        });
        Bukkit.getWorld("world").setTime(0);

        this.plugin.getGamemanager().setCurrentGameState(GameState.MID_GAME);
    }

    private void setupSpawnLocation(Location location) {
        this.plugin.setSpawnLocation(location.clone());

        Block block = location.getBlock();
        block.setType(Material.AIR);

        block.getRelative(BlockFace.UP)
                .setType(Material.AIR);
    }
}
