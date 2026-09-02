package forceitembattle.randomevents;

import forceitembattle.model.Find;
import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import forceitembattle.util.TimeFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A ten-minute scoring race. Every non-skipped find (back-to-backs included) scores by pool tier
 * — Early 1, Mid 2, Late 3 — and the top scorer at the end takes the Wheels.
 *
 * <p>Unlike the other events it resolves on its own clock rather than on a find, so it holds the
 * active slot the whole time and concludes from {@link #tick()}. That countdown is driven by the
 * manager's mid-game-only tick, so it freezes during a pause.
 */
@RequiredArgsConstructor
public class PointHunt implements RandomEvent {

    public static final int DURATION_SECONDS = 10 * 60;
    /** Won't start without this much clock left, so a full run always finishes before game end. */
    public static final int MIN_START_SECONDS = DURATION_SECONDS + 60;

    private static final int SOLO_WHEELS = 3;
    private static final int TEAM_WHEELS_TOTAL = 4;

    private static int pointsFor(State state) {
        return switch (state) {
            case EARLY -> 1;
            case MID -> 2;
            case LATE -> 3;
        };
    }

    private final EventContext context;

    /**
     * Keyed by {@link ScoreOwner}, so teammates share a tally and a solo player has their own.
     * Identity keys: an owner is the live per-round instance and neither implementation overrides
     * equals.
     */
    private final Map<ScoreOwner, Integer> points = new LinkedHashMap<>();

    private int secondsLeft = DURATION_SECONDS;

    @Override
    public void start() {
        boolean teamGame = this.context.settings().isSettingEnabled(GameSetting.TEAM);

        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<aqua><b>Point Hunt</b><reset><gray> has begun!"));
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gray>For the next <yellow>10 minutes<gray>, score the most points to win! "
                + "<green>Early <gray>= 1, <yellow>Mid <gray>= 2, <red>Late <gray>= 3."));

        String reward = teamGame
                ? "<gray>The top <yellow>team<gray> takes <yellow>" + TEAM_WHEELS_TOTAL
                  + " Wheels of Fortune<gray>, split across its members!"
                : "<gray>The top scorer takes <yellow>" + SOLO_WHEELS + " Wheels of Fortune<gray>!";
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + reward));

        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.4f));
    }

    @Override
    public boolean onFoundItem(Find find) {
        if (find.skipped()) {
            return false; // skips never score; back-to-backs do
        }

        if (find.material() == null) {
            return false;
        }

        State state = this.context.items().getState(find.material());
        if (state == null) {
            return false; // not a pool item (custom/unregistered) — no points
        }

        this.points.merge(find.finder().scoreOwner(), pointsFor(state), Integer::sum);
        return false; // Point Hunt only ends on its own clock
    }

    @Override
    public boolean tick() {
        if (--this.secondsLeft > 0) {
            return false;
        }
        this.conclude();
        return true;
    }

    /**
     * Time only. Scores are deliberately not shown: the hunt is a race you play by finding items
     * faster, not by watching a board.
     */
    @Override
    public String tabFooterBlock() {
        return "\n\n<b>" + RandomEvents.POINT_HUNT.coloredName() + "</b> <dark_gray>· "
                + TimeFormat.colored(this.secondsLeft);
    }

    @Override
    public void cancel() {
        // Interrupted by an early game end or reset: per design, an unfinished hunt pays nothing.
    }

    private void conclude() {
        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1));

        List<Map.Entry<ScoreOwner, Integer>> ranked = this.points.entrySet().stream()
                .sorted(Map.Entry.<ScoreOwner, Integer>comparingByValue().reversed())
                .toList();

        if (ranked.isEmpty()) {
            Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gray>The " + RandomEvents.POINT_HUNT.coloredName()
                    + " <gray>ended with no points scored — no winner."));
            return;
        }

        int topScore = ranked.getFirst().getValue();
        boolean tied = ranked.size() > 1 && ranked.get(1).getValue() == topScore;
        if (tied) {
            Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gray>The " + RandomEvents.POINT_HUNT.coloredName()
                    + " <gray>ended in a <yellow>tie<gray> at <yellow>" + topScore + " points<gray> — no payout!"));
            return;
        }

        ScoreOwner winner = ranked.getFirst().getKey();
        String display = this.awardAndDescribe(winner);

        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + display + " <gray>won the "
                + RandomEvents.POINT_HUNT.coloredName() + " <gray>with <yellow>" + topScore + " points<gray>!"));
    }

    /** Pays the winner and returns their display name. Team: 4 split evenly (2/2). Solo: 3. */
    private String awardAndDescribe(ScoreOwner winner) {
        List<ForceItemPlayer> members = winner.members();

        if (members.size() <= 1) {
            members.forEach(member -> this.giveWheels(member.player(), SOLO_WHEELS));
        } else {
            int base = TEAM_WHEELS_TOTAL / members.size();
            int remainder = TEAM_WHEELS_TOTAL % members.size();
            for (int i = 0; i < members.size(); i++) {
                int amount = base + (i < remainder ? 1 : 0);
                if (amount > 0) {
                    this.giveWheels(members.get(i).player(), amount);
                }
            }
        }

        // The payout is a score-owner question; the label is not — a team has a name and a colour.
        return winner instanceof Team team
                ? team.getTeamDisplay()
                : "<green>" + members.getFirst().player().getName();
    }

    private void giveWheels(Player player, int amount) {
        ItemStack wheels = CustomMaterials.WHEEL_OF_FORTUNE.itemStack(amount);
        player.getInventory().addItem(wheels).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}

