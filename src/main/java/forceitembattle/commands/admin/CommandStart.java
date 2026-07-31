package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GamePreset;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandStart extends CustomCommand implements CustomTabCompleter {

    public CommandStart(ForceItemBattle plugin) {
        super(plugin, "start");
        setUsage("<time in min> <jokers> or <preset>");
        setDescription("Start the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;

        if (args.length == 1) {
            if (this.plugin.getSettings().getGamePreset(args[0]) == null) {
                player.sendMessage(Text.of("<yellow>" + args[0] + " <red>does not exist in presets."));
                return;
            }

            GamePreset gamePreset = this.plugin.getSettings().getGamePreset(args[0]);
            this.plugin.getGamemanager().setCurrentGamePreset(gamePreset);
            this.performCommand(gamePreset, player, args);

        } else if (args.length == 2) {
            try {
                this.performCommand(null, player, args);

            } catch (NumberFormatException e) {
                player.sendMessage(Text.of("<red>Usage: /start <time in min> <jokers>"));
                player.sendMessage(Text.of("<red><time> and <jokers> have to be numbers"));
            }
        } else {
            player.sendMessage(Text.of("<red>Usage: /start <time in min> <jokers>"));
        }
    }

    private void performCommand(GamePreset gamePreset, Player player, String[] args) {
        int durationMinutes = (gamePreset != null ? gamePreset.getCountdown() : Integer.parseInt(args[0]));
        int durationSeconds = durationMinutes * 60;
        int jokersAmount = (gamePreset != null ? gamePreset.getJokers() : (Integer.parseInt(args[1])));

        if (gamePreset == null && jokersAmount > 64) {
            player.sendMessage(Text.of("<red>The maximum amount of jokers is 64."));
            return;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            if (plugin.getGamemanager().forceItemPlayerMap().size() < 1) {
                Bukkit.broadcast(Text.of("<red>There are not enough players online to enable teams"));
                this.plugin.getSettings().setSettingEnabled(GameSetting.TEAM, false);
                this.plugin.getTeamManager().clearAllTeams();
            } else {
                this.plugin.getTeamManager().autoTeams();
            }
        }

        this.plugin.getTimerManager().setTimeLeft(durationSeconds);
        this.plugin.getGamemanager().setGameDuration(durationSeconds);
        this.plugin.getGamemanager().setJokerAmount(jokersAmount);
        this.plugin.getGamemanager().initializeMaterials();

        // Teams and force items are assigned by now, so the roster is frozen from here on.
        this.plugin.getGamemanager().setCurrentGameState(GameState.STARTING);

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
                Title startingTitle = Title.title(Text.of("<green>" + seconds), Text.of(subtitle), times);
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
                    if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
                        return;
                    }

                    for (ForceItemPlayer teammate : forceItemPlayer.currentTeam().getPlayers()) {
                        if (teammate == forceItemPlayer) continue;
                        // A teammate who disconnected during the countdown keeps their roster spot,
                        // so their Player reference is still here but no longer connected.
                        if (teammate.player() == null || !teammate.player().isOnline()) continue;

                        Component subTitle = Text.of("<yellow>Team " + teammate.currentTeam().getTeamDisplay() + " <gray>| <green>" + forceItemPlayer.player().getName());

                        Title.Times times = Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(2000), Duration.ofMillis(600));
                        Title title = Title.title(Component.empty(), subTitle, times);

                        teammate.player().showTitle(title);
                    }
                });
            }

            private String getSubtitle() {
                String subTitle = "";

                switch (seconds) {
                    case 8 ->
                            subTitle = "<white>» <gold>" + (plugin.getTimerManager().getTimeLeft() / 60) + " minutes <white>«";
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
        this.plugin.getGamemanager().setGameStartTime(System.currentTimeMillis());
        this.plugin.getGamemanager().setMatchId(java.util.UUID.randomUUID());
        
        this.plugin.getRecipeManager().initRecipes();

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

        World world = Objects.requireNonNull(Dimension.OVERWORLD.world());
        Location spawnLocation = world.getSpawnLocation();
        setupSpawnLocation(spawnLocation);

        Bukkit.getWorlds().forEach(worlds -> worlds.getWorldBorder().reset());
        world.setGameRule(GameRules.ADVANCE_TIME, true);

        // Only the players online at this instant. Anyone who disconnected during the countdown
        // keeps their roster spot and is set up by the same call when they rejoin.
        Bukkit.getOnlinePlayers().forEach(player -> this.plugin.getGamemanager().applyStartSetup(player));

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            distributeTeamJokers(jokersAmount);
        }

        this.plugin.getWanderingTraderManager().startTimer();
        this.plugin.getRandomEventManager().startGame();
        this.plugin.getGamemanager().setGameStartTime(System.currentTimeMillis());
        Dimension.OVERWORLD.world().setTime(0);
        this.plugin.getAchievementManager().resetProgress();
        this.plugin.getItemDifficultiesManager().resetUnlockAnnouncements();
        this.plugin.getGamemanager().setCurrentGameState(GameState.MID_GAME);
        this.plugin.getScoreboardManager().updateAllPlayers();
    }

    private void distributeTeamJokers(int jokersAmount) {
        this.plugin.getTeamManager().getTeams().forEach(team -> {
            team.setRemainingJokers(jokersAmount);
            int totalPlayers = team.getPlayers().size();
            int jokerPerPlayer = jokersAmount / totalPlayers;
            int remainingJoker = jokersAmount % totalPlayers;

            for (ForceItemPlayer teamPlayer : team.getPlayers()) {
                int playerJoker = jokerPerPlayer;

                if (remainingJoker > 0) {
                    playerJoker++;
                    remainingJoker--;
                }

                // The share is computed for the whole team either way, so the split stays stable;
                // a member who is offline right now simply gets no stack here and is handed the
                // team's remaining pool by applyStartSetup when they rejoin.
                Player teamMember = teamPlayer.player();
                if (teamMember == null || !teamMember.isOnline()) {
                    continue;
                }
                teamMember.getInventory().setItem(4, Gamemanager.getJokers(playerJoker));
            }
        });
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
