package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.gui.ResultReveal;
import forceitembattle.gui.ResultScreen;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.Roster;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandResult extends CustomCommand {

    private final Gamemanager gamemanager;
    private final TimerManager timerManager;
    private final Roster roster;
    private final GameSettings settings;
    private final TeamsManager teamManager;
    private final ResultCeremony resultCeremony;

    public CommandResult(Gamemanager gamemanager, TimerManager timerManager, Roster roster, GameSettings settings, TeamsManager teamManager, ResultCeremony resultCeremony) {
        super("result");
        this.gamemanager = gamemanager;
        this.timerManager = timerManager;
        this.roster = roster;
        this.settings = settings;
        this.teamManager = teamManager;
        this.resultCeremony = resultCeremony;
        setDescription("Show the next player's result");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.timerManager.getTimeLeft() > 0) {
            return;
        }

        if (args.length == 1) {
            if (this.settings.isSettingEnabled(GameSetting.TEAM)) {
                Team team = this.teamAt(args[0]);
                if (team == null) {
                    player.sendMessage(Text.of("<red>Invalid team."));
                    return;
                }
                this.openScreen(player, team);
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Text.of("<red>Invalid UUID."));
                return;
            }

            ForceItemPlayer target = this.roster.get(uuid);
            if (target == null) {
                player.sendMessage(Text.of("<red>Nobody with that id played this round."));
                return;
            }

            this.openScreen(player, target.scoreOwner());
            return;
        }

        if (args.length == 0) {
            requireOp(player, () -> showNextResult(player));
        }
    }

    /**
     * The team {@code argument} names, or null when it names none. Both an unparseable id and an
     * out-of-range one have to answer null: they are different exceptions, and a bounds failure
     * escaping here threw {@code /result #99} out of the command.
     */
    @Nullable
    private Team teamAt(String argument) {
        int index;
        try {
            index = Integer.parseInt(argument.replace("#", "")) - 1;
        } catch (NumberFormatException e) {
            return null;
        }

        List<Team> teams = this.teamManager.getTeams();
        return index >= 0 && index < teams.size() ? teams.get(index) : null;
    }

    /** Hands out the next reveal, or says the ceremony is over. */
    private void showNextResult(Player player) {
        ResultCeremony ceremony = this.resultCeremony;

        Optional<ResultCeremony.Reveal> next = ceremony.nextReveal();
        if (next.isEmpty()) {
            player.sendMessage(Text.of("<gray>No more results left."));
            return;
        }

        ResultCeremony.Reveal reveal = next.get();

        // The winner is revealed last; the stats link may only go out once that reveal finished.
        Runnable onRevealComplete = reveal.last()
                ? () -> this.gamemanager.getMatchHistory().markResultsRevealed()
                : null;

        // The reveal builds the pages and hands them out; the ceremony stores them. The GUI never
        // reaches into shared state to do it.
        Bukkit.getOnlinePlayers().forEach(viewer -> new ResultReveal(
                this.settings,
                reveal,
                pages -> ceremony.archive(reveal.owner(), pages),
                onRevealComplete).open(viewer));
    }

    /** Reopens an owner's screen from the pages the reveal already built. */
    private void openScreen(Player viewer, ScoreOwner owner) {
        new ResultScreen(owner, this.resultCeremony.pagesFor(owner).orElse(null))
                .open(viewer);
    }

}
