package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.GameState;
import forceitembattle.util.PlayerStat;
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

                String subtitle = getSubtitle();
                Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500));
                Title startingTitle = Title.title(plugin.getGamemanager().getMiniMessage().deserialize("<green>" + seconds), plugin.getGamemanager().getMiniMessage().deserialize(subtitle), times);
                Bukkit.getOnlinePlayers().forEach(
                        players -> players.showTitle(startingTitle)
                );
            }

            private String getSubtitle() {
                String subTitle = "";

                switch (seconds) {
                    case 9, 8 -> subTitle = "<white>» <gold>" + (plugin.getTimer().getTimeLeft() / 60) + " minutes <white>«";
                    case 7, 6 -> subTitle = "<white>» <gold>" + jokersAmount + " Joker <white>«";
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
        this.plugin.getPositionManager().clearPositions();

        World world = Bukkit.getWorld("world");
        assert world != null;
        Location spawnLocation = world.getSpawnLocation();
        setupSpawnLocation(spawnLocation);

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);

        Bukkit.getOnlinePlayers().forEach(player -> {

            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

            player.sendMessage(" ");
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold><b>Force Item Battle</b> <dark_gray>«"));
            player.sendMessage(" ");
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Duration <dark_gray>» <green>" + timeMinutes + " minutes"));
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Joker <dark_gray>» <green>" + jokersAmount));
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
                for(ForceItemPlayer teamPlayers : forceItemPlayer.currentTeam().getPlayers()) {
                    if(teamPlayers == forceItemPlayer) continue;

                    Component subTitle = this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>Team #" + teamPlayers.currentTeam().getTeamId() + " <gray>| <green>" + teamPlayers.player().getName());

                    Title.Times times = Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(2000), Duration.ofMillis(600));
                    Title title = Title.title(Component.empty(), subTitle, times);

                    teamPlayers.player().showTitle(title);
                }

                this.plugin.getTeamManager().getTeamsList().forEach(teams -> {
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
                player.getInventory().setItem(4, Gamemanager.getJokers(jokersAmount));
            }

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
            new ScoreboardManager(player);

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
                if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    this.plugin.getBackpack().createTeamBackpack(forceItemPlayer.currentTeam(), player);
                } else {
                    this.plugin.getBackpack().createBackpack(player);
                }

            }

            if(!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
                forceItemPlayer.createItemDisplay();
            }

            if(this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.GAMES_PLAYED, this.plugin.getStatsManager().playerStats(player.getName()), 1);
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
