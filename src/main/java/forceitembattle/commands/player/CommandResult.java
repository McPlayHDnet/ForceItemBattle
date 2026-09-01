package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.gui.ResultReveal;
import forceitembattle.gui.ResultScreen;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.Map;
import forceitembattle.model.ResultCeremony;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandResult extends CustomCommand {

    public CommandResult(ForceItemBattle plugin) {
        super(plugin, "result");
        setDescription("Show the next player's result");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getTimerManager().getTimeLeft() > 0) {
            return;
        }

        if (args.length == 1) {
            if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
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

            ForceItemPlayer target = this.plugin.getRoster().get(uuid);
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
     * The team {@code argument} names, or null when it names none.
     *
     * <p>Both halves of this used to be wrong. The lookup ran inside a
     * {@code catch (IllegalArgumentException)}, which catches the {@code NumberFormatException}
     * from bad text but <em>not</em> the {@code IndexOutOfBoundsException} from
     * {@code List.get()} — so {@code /result #99} threw out of the command. And the catch that did
     * fire had no {@code return} after it, so a failed parse fell through to open a result screen
     * with nothing to show.
     */
    @Nullable
    private Team teamAt(String argument) {
        int index;
        try {
            index = Integer.parseInt(argument.replace("#", "")) - 1;
        } catch (NumberFormatException e) {
            return null;
        }

        List<Team> teams = this.plugin.getTeamManager().getTeams();
        return index >= 0 && index < teams.size() ? teams.get(index) : null;
    }

    /**
     * Hands out the next reveal, or says the ceremony is over.
     *
     * <p>This was two branches differing only in solo-versus-team — which is the Score Owner
     * distinction, so the ceremony walks Score Owners and there is one branch. The command no
     * longer holds a place counter or a match id.
     */
    private void showNextResult(Player player) {
        ResultCeremony ceremony = this.plugin.getResultCeremony();

        Optional<ResultCeremony.Reveal> next = ceremony.nextReveal();
        if (next.isEmpty()) {
            player.sendMessage(Text.of("<gray>No more results left."));
            return;
        }

        ResultCeremony.Reveal reveal = next.get();

        // The winner is revealed last; the stats link may only go out once that reveal finished.
        Runnable onRevealComplete = reveal.last()
                ? () -> this.plugin.getGamemanager().getMatchHistory().markResultsRevealed()
                : null;

        // The reveal builds the pages and hands them out; the ceremony stores them. The GUI does
        // not reach into shared state to do it, which is what the old FinishInventory did.
        Bukkit.getOnlinePlayers().forEach(viewer -> new ResultReveal(
                this.plugin,
                reveal,
                pages -> ceremony.archive(reveal.owner(), pages),
                onRevealComplete).open(viewer));
    }

    /** Reopens an owner's screen from the pages the reveal already built. */
    private void openScreen(Player viewer, ScoreOwner owner) {
        new ResultScreen(owner, this.plugin.getResultCeremony().pagesFor(owner).orElse(null))
                .open(viewer);
    }

}
