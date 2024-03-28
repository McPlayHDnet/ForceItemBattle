package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.GameState;
import forceitembattle.util.PlayerStat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CommandStart extends CustomCommand implements CustomTabCompleter {

    public CommandStart() {
        super("start");
        setUsage("<time in min> <jokers> or <preset>");
        setDescription("Start the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 1) {
            if(this.plugin.getSettings().getGamePreset(args[0]) == null) {
                player.sendMessage("§e" + args[0] + " §cdoes not exist in presets.");
                return;
            }

            GamePreset gamePreset = this.plugin.getSettings().getGamePreset(args[0]);
            this.plugin.getGamemanager().setCurrentGamePreset(gamePreset);
            this.performCommand(gamePreset, player, args);

        } else if (args.length == 2) {
            try {
                this.performCommand(null, player, args);

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
                player.sendMessage(ChatColor.RED + "<time> and <jokers> have to be numbers");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
        }

    }

    private void performCommand(GamePreset gamePreset, Player player, String[] args) {
        int durationMinutes = (gamePreset != null ? gamePreset.countdown() : Integer.parseInt(args[0]));
        int countdown = durationMinutes * 60;
        int jokersAmount = (gamePreset != null ? gamePreset.jokers() : (Integer.parseInt(args[1])));
        this.plugin.getTimer().setTime(countdown);
        this.plugin.getGamemanager().initializeMats();

        if (gamePreset == null) {
            if (Integer.parseInt(args[1]) > 64) {
                player.sendMessage(ChatColor.RED + "The maximum amount of jokers is 64.");
                return;
            }
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

            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
            player.getPassengers().forEach(Entity::remove);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(spawnLocation);
            player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                this.plugin.getBackpack().createBackpack(player);
            }

            if(!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                forceItemPlayer.createItemDisplay();
            }

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
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

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return new ArrayList<>(this.plugin.getSettings().gamePresetMap().keySet());
    }
}
