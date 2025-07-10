package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.stats.SeasonalStats;
import forceitembattle.manager.stats.StatsManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
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
        if(player.isOp()) {
            if (args.length == 1) {
                if(this.plugin.getSettings().getGamePreset(args[0]) == null) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[0] + " <red>does not exist in presets."));
                    return;
                }

                GamePreset gamePreset = this.plugin.getSettings().getGamePreset(args[0]);
                this.plugin.getGamemanager().setCurrentGamePreset(gamePreset);
                this.performCommand(gamePreset, player, args);

            } else if (args.length == 2) {
                try {
                    this.performCommand(null, player, args);

                } catch (NumberFormatException e) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /start <time in min> <jokers>"));
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red><time> and <jokers> have to be numbers"));
                }
            } else {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /start <time in min> <jokers>"));
            }
        }
    }

    private void performCommand(GamePreset gamePreset, Player player, String[] args) {
        int durationMinutes = (gamePreset != null ? gamePreset.getCountdown() : Integer.parseInt(args[0]));
        int durationSeconds = durationMinutes * 60;
        int jokersAmount = (gamePreset != null ? gamePreset.getJokers() : (Integer.parseInt(args[1])));

        if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            if(plugin.getGamemanager().forceItemPlayerMap().size() < 4) {
                Bukkit.broadcast(plugin.getGamemanager().getMiniMessage().deserialize("<red>There are not enough players online to enable teams"));
                this.plugin.getSettings().setSettingEnabled(GameSetting.TEAM, false);
                this.plugin.getTeamManager().clearAllTeams();
            } else {
                this.plugin.getTeamManager().autoTeams();
            }
        }

        this.plugin.getTimer().setTimeLeft(durationSeconds);
        this.plugin.getGamemanager().setGameDuration(durationSeconds);
        this.plugin.getGamemanager().initializeMats();

        if (gamePreset == null) {
            if (Integer.parseInt(args[1]) > 64) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The maximum amount of jokers is 64."));
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

                if (seconds == 10) {
                    showTeams();
                    return;
                }

                String subtitle = getSubtitle();
                Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500));
                Title startingTitle = Title.title(plugin.getGamemanager().getMiniMessage().deserialize("<green>" + seconds), plugin.getGamemanager().getMiniMessage().deserialize(subtitle), times);
                Bukkit.getOnlinePlayers().forEach(
                        players -> players.showTitle(startingTitle)
                );
            }

            private void showTeams() {
                if (!plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    ForceItemPlayer forceItemPlayer = plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                    if (forceItemPlayer.isSpectator()) {
                        return;
                    }

                    for (ForceItemPlayer teammate : forceItemPlayer.currentTeam().getPlayers()) {
                        if (teammate == forceItemPlayer) continue;

                        Component subTitle = plugin.getGamemanager().getMiniMessage()
                                .deserialize("<yellow>Team " + teammate.currentTeam().getTeamDisplay() + " <gray>| <green>" + forceItemPlayer.player().getName());

                        Title.Times times = Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(2000), Duration.ofMillis(600));
                        Title title = Title.title(Component.empty(), subTitle, times);

                        teammate.player().showTitle(title);
                    }
                });
            }

            private String getSubtitle() {
                String subTitle = "";

                switch (seconds) {
                    case 8 -> subTitle = "<white>» <gold>" + (plugin.getTimer().getTimeLeft() / 60) + " minutes <white>«";
                    case 6 -> subTitle = "<white>» <gold>" + jokersAmount + " Jokers <white>«";
                    case 5 -> subTitle = "<white>» <gold>/info & /infowiki <white>«";
                    case 4 -> subTitle = "<white>» <gold>/spawn & /bed <white>«";
                    case 3, 2 -> subTitle = "<white>» <gold>Collect as many items as possible <white>«";
                    case 1 -> subTitle = "<white>» <gold>Have fun! <white>«";
                }

                return subTitle;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private void startGame(int timeMinutes, int jokersAmount) {
        this.plugin.initRecipes();

        this.plugin.getPositionManager().clearPositions();
        // Fixed 5 / 15 minutes switch times.
        if (timeMinutes >= 50) {
            ItemDifficultiesManager.State.EARLY.setUnlockedAtPercentage(0);
            ItemDifficultiesManager.State.MID.setUnlockedAtPercentage((5. / timeMinutes) * 100);
            ItemDifficultiesManager.State.LATE.setUnlockedAtPercentage((15. / timeMinutes) * 100);
        } else {
            // If game is under 50 minutes, use percentages
            this.plugin.getItemDifficultiesManager().setupStates();
        }

        World world = Bukkit.getWorld("world");
        assert world != null;
        Location spawnLocation = world.getSpawnLocation();
        setupSpawnLocation(spawnLocation);

        world.getWorldBorder().reset();
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);

        Bukkit.getOnlinePlayers().forEach(player -> {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

            if(forceItemPlayer.isSpectator()) {
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
                return;
            }

            player.sendMessage(" ");
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold><b>Force Item Battle</b> <dark_gray>«"));
            player.sendMessage(" ");
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Duration <dark_gray>» <green>" + timeMinutes + " minutes"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Jokers <dark_gray>» <green>" + jokersAmount));
            for (GameSetting gameSettings : GameSetting.values()) {
                if(gameSettings.defaultValue() instanceof Integer) continue;
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>" + gameSettings.displayName() + " <dark_gray>» <green>" + (this.plugin.getSettings().isSettingEnabled(gameSettings) ? "<dark_green>✔" : "<dark_red>✘")));
            }
            player.sendMessage(" ");
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(" <dark_gray>● <gray>Useful Commands:"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>/info"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>/infowiki"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>/spawn"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>/bed"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>/pos"));
            player.sendMessage("");

            player.setHealth(20);
            player.setSaturation(20);
            player.getInventory().clear();

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {

                this.plugin.getTeamManager().getTeams().forEach(teams -> {
                    teams.setRemainingJokers(jokersAmount);
                    int totalPlayers = teams.getPlayers().size();
                    int jokerPerPlayer = jokersAmount / totalPlayers;
                    int remainingJoker = jokersAmount % totalPlayers;
                    int jokerGiven = 0;

                    for(ForceItemPlayer teamPlayer : teams.getPlayers()) {
                        int playerJoker = jokerPerPlayer;

                        if(remainingJoker > 0) {
                            playerJoker++;
                            remainingJoker--;
                        }
                        teamPlayer.player().getInventory().setItem(4, Gamemanager.getJokers(playerJoker));

                        jokerGiven += playerJoker;
                    }

                    while(jokerGiven < jokersAmount) {
                        for(ForceItemPlayer teamPlayer : teams.getPlayers()) {
                            ItemStack itemStack = teamPlayer.player().getInventory().getItem(4);
                            if(itemStack != null && itemStack.getType() == Gamemanager.getJokerMaterial()) {
                                itemStack.setAmount(itemStack.getAmount() + 1);
                                jokerGiven++;

                                if(jokerGiven >= jokersAmount) {
                                    break;
                                }
                            }
                        }
                    }
                });
            } else {
                forceItemPlayer.setRemainingJokers(jokersAmount);
                if (!this.plugin.getSettings().isSettingEnabled(GameSetting.RUN)) {
                    player.getInventory().setItem(4, Gamemanager.getJokers(jokersAmount));
                }
            }

            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
            player.getPassengers().forEach(Entity::remove);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.setGameMode(!forceItemPlayer.isSpectator() ? GameMode.SURVIVAL : GameMode.SPECTATOR);
            player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
                if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    this.plugin.getBackpack().createTeamBackpack(forceItemPlayer.currentTeam(), player);
                } else {
                    this.plugin.getBackpack().createBackpack(player);
                }

            }


            if(this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                ForceItemPlayerStats playerStats = this.plugin.getStatsManager().loadPlayerStats(player.getName());
                SeasonalStats seasonalStats = playerStats.getSeasonStats(StatsManager.CURRENT_SEASON);
                if(!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    this.plugin.getStatsManager().updateSoloStats(player.getName(), PlayerStat.GAMES_PLAYED, seasonalStats.getGamesPlayed().getSolo() + 1);
                    return;
                }
                if (forceItemPlayer.currentTeam() != null) {
                    Team currentTeam = forceItemPlayer.currentTeam();

                    for (ForceItemPlayer teamPlayer : currentTeam.getPlayers()) {
                        if (!teamPlayer.equals(forceItemPlayer)) {
                            this.plugin.getStatsManager().updateTeamStats(player.getName(), teamPlayer.player().getName(), 1, PlayerStat.GAMES_PLAYED);
                            break;
                        }
                    }
                }
            }

        });
        this.plugin.getWanderingTraderTimer().startTimer();
        this.plugin.getGamemanager().setGameStartTime(System.currentTimeMillis());
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
