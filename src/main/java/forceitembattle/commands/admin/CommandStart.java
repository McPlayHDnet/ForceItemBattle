package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.Precondition;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GamePreset;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundStart;
import forceitembattle.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class CommandStart extends CustomCommand implements CustomTabCompleter {

    public CommandStart(ForceItemBattle plugin) {
        super(plugin, "start");
        setUsage("<time in min> <jokers> or <preset>");
        setDescription("Start the game");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        this.start(player, args);
    }

    /**
     * Starting a round needs no player: it acts on the roster, not on whoever asked.
     *
     * <p>Overridden because the base class refuses console senders, which meant a server owner
     * could not start a round from the console or RCON — and neither could a test. Nothing below
     * touches the sender except to report back.
     */
    @Override
    public void onConsoleCommand(CommandSender sender, String label, String[] args) {
        this.start(sender, args);
    }

    private void start(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (this.plugin.getSettings().getGamePreset(args[0]) == null) {
                sender.sendMessage(Text.of("<yellow>" + args[0] + " <red>does not exist in presets."));
                return;
            }

            GamePreset gamePreset = this.plugin.getSettings().getGamePreset(args[0]);
            this.plugin.getSettings().getRuleset().usePreset(gamePreset);
            this.performCommand(gamePreset, sender, args);

        } else if (args.length == 2) {
            try {
                // Clears whatever the last round used. Without this a preset outlives its round:
                // it was only ever set, never reset, so `/start speedrun` followed by `/start 90 3`
                // played the second round on speedrun's settings. Production hides it because
                // scheduleReset restarts the JVM between rounds; a session that plays two does not.
                this.plugin.getSettings().getRuleset().usePreset(null);
                this.performCommand(null, sender, args);

            } catch (NumberFormatException e) {
                sender.sendMessage(Text.of("<red>Usage: /start <time in min> <jokers>"));
                sender.sendMessage(Text.of("<red><time> and <jokers> have to be numbers"));
            }
        } else {
            sender.sendMessage(Text.of("<red>Usage: /start <time in min> <jokers>"));
        }
    }

    private void performCommand(GamePreset gamePreset, CommandSender player, String[] args) {
        boolean teamsConfigured = this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM);
        int rosterSize = this.plugin.getRoster().players().size();

        RoundStart start = gamePreset != null
                ? RoundStart.fromPreset(gamePreset, teamsConfigured, rosterSize)
                : RoundStart.fromArguments(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                        teamsConfigured, rosterSize);

        if (start instanceof RoundStart.Refused refused) {
            player.sendMessage(Text.of(switch (refused.refusal()) {
                case TOO_MANY_JOKERS -> "<red>The maximum amount of jokers is " + RoundStart.MAX_JOKERS + ".";
            }));
            return;
        }

        RoundStart.Planned plan = (RoundStart.Planned) start;
        int durationMinutes = plan.durationMinutes();
        int jokersAmount = plan.jokers();

        this.applyTeams(plan.teams());

        this.plugin.getRoundClock().startRound(plan.durationSeconds());
        this.plugin.getGamemanager().setJokerAmount(jokersAmount);
        this.plugin.getGamemanager().initializeMaterials();

        // Teams and force items are assigned by now, so the roster is frozen from here on.
        this.plugin.getRoundPhase().moveTo(GameState.STARTING);

        new BukkitRunnable() {

            int seconds = 11;

            @Override
            public void run() {
                seconds--;
                if (seconds == 0) {
                    cancel();

                    plugin.getGamemanager().startGame(durationMinutes, jokersAmount);
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
                // The decision, not the setting. These agree only because applyTeams wrote the
                // setting off on the TOO_FEW_PLAYERS path; asking the plan means the reveal no
                // longer depends on that write having happened first.
                if (plan.teams() != RoundStart.Teams.BUILD) {
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    ForceItemPlayer forceItemPlayer =
                            plugin.getRoster().participant(player.getUniqueId()).orElse(null);
                    if (forceItemPlayer == null) {
                        return;
                    }

                    forceItemPlayer.teammate().ifPresent(teammate -> {
                        // A teammate who disconnected during the countdown keeps their roster spot,
                        // so their Player reference is still here but no longer connected.
                        if (teammate.player() == null || !teammate.player().isOnline()) return;

                        Component subTitle = Text.of("<yellow>Team " + teammate.currentTeam().getTeamDisplay()
                                + " <gray>| <green>" + forceItemPlayer.player().getName());

                        Title.Times times = Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(2000), Duration.ofMillis(600));
                        teammate.player().showTitle(Title.title(Component.empty(), subTitle, times));
                    });
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

    /**
     * The only place teams are touched by {@code /start}. Every branch here is an effect; which one
     * runs was decided by {@link RoundStart}.
     */
    private void applyTeams(RoundStart.Teams teams) {
        switch (teams) {
            case BUILD -> this.plugin.getTeamManager().autoTeams();
            case TOO_FEW_PLAYERS -> {
                Bukkit.broadcast(Text.of("<red>There are not enough players online to enable teams"));
                this.plugin.getSettings().setSettingEnabled(GameSetting.TEAM, false);
                this.plugin.getTeamManager().clearAllTeams();
            }
            case NONE -> { }
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return new ArrayList<>(this.plugin.getSettings().gamePresetMap().keySet());
    }
}
