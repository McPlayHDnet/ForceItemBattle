package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GamePreset;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class CommandStart extends CustomCommand implements CustomTabCompleter {

    private final Gamemanager gamemanager;
    private final TimerManager timerManager;
    private final Roster roster;
    private final RoundPhase roundPhase;
    private final RoundClock roundClock;
    private final GameSettings settings;
    private final TeamsManager teamManager;

    /** Only to schedule the countdown. */
    private final Plugin plugin;

    public CommandStart(Gamemanager gamemanager, TimerManager timerManager, Roster roster, RoundPhase roundPhase, RoundClock roundClock, GameSettings settings, TeamsManager teamManager, Plugin plugin) {
        super("start");
        this.gamemanager = gamemanager;
        this.timerManager = timerManager;
        this.roster = roster;
        this.roundPhase = roundPhase;
        this.roundClock = roundClock;
        this.settings = settings;
        this.teamManager = teamManager;
        this.plugin = plugin;
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
     * Starting a round needs no player: it acts on the roster, not on whoever asked. Overridden
     * because the base class refuses console senders, which rules out the console, RCON and tests.
     */
    @Override
    public void onConsoleCommand(CommandSender sender, String label, String[] args) {
        this.start(sender, args);
    }

    private void start(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (this.settings.getGamePreset(args[0]) == null) {
                sender.sendMessage(Text.of("<yellow>" + args[0] + " <red>does not exist in presets."));
                return;
            }

            GamePreset gamePreset = this.settings.getGamePreset(args[0]);
            this.settings.getRuleset().usePreset(gamePreset);
            this.performCommand(gamePreset, sender, args);

        } else if (args.length == 2) {
            try {
                // Clears whatever the last round used. Without this `/start speedrun` followed by
                // `/start 90 3` plays the second round on speedrun's settings — hidden in production
                // only because scheduleReset restarts the JVM between rounds.
                this.settings.getRuleset().usePreset(null);
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
        boolean teamsConfigured = this.settings.isSettingEnabled(GameSetting.TEAM);
        int rosterSize = this.roster.players().size();

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

        this.roundClock.startRound(plan.durationSeconds());
        this.gamemanager.setJokerAmount(jokersAmount);
        this.gamemanager.initializeMaterials();

        // Teams and force items are assigned by now, so the roster is frozen from here on.
        this.roundPhase.moveTo(GameState.STARTING);

        new BukkitRunnable() {

            int seconds = 11;

            @Override
            public void run() {
                seconds--;
                if (seconds == 0) {
                    cancel();

                    CommandStart.this.gamemanager.startGame(durationMinutes, jokersAmount);
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
                // The decision, not the setting: asking the plan means this does not depend on
                // applyTeams having written the setting off first.
                if (plan.teams() != RoundStart.Teams.BUILD) {
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    ForceItemPlayer forceItemPlayer =
                            CommandStart.this.roster.participant(player.getUniqueId()).orElse(null);
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
                            subTitle = "<white>» <gold>" + (CommandStart.this.timerManager.getTimeLeft() / 60) + " minutes <white>«";
                    case 6 -> subTitle = "<white>» <gold>" + jokersAmount + " Jokers <white>«";
                    case 5 -> subTitle = "<white>» <gold>/info & /infowiki <white>«";
                    case 4 -> subTitle = "<white>» <gold>/spawn & /bed <white>«";
                    case 3, 2 -> subTitle = "<white>» <gold>Collect as many items as possible <white>«";
                    case 1 -> subTitle = "<white>» <gold>Have fun! <white>«";
                }

                return subTitle;
            }
        }.runTaskTimer(CommandStart.this.plugin, 0L, 20L);
    }

    /** Every branch here is an effect; which one runs was decided by {@link RoundStart}. */
    private void applyTeams(RoundStart.Teams teams) {
        switch (teams) {
            case BUILD -> this.teamManager.autoTeams();
            case TOO_FEW_PLAYERS -> {
                Bukkit.broadcast(Text.of("<red>There are not enough players online to enable teams"));
                this.settings.setSettingEnabled(GameSetting.TEAM, false);
                this.teamManager.clearAllTeams();
            }
            case NONE -> { }
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return new ArrayList<>(this.settings.gamePresetMap().keySet());
    }
}
