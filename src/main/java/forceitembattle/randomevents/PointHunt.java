package forceitembattle.randomevents;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
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
 * Unlike the other events it resolves on its own clock rather than on a find, so it holds the
 * active slot the whole time and concludes from {@link #tick()}. Scoring keys on the scoring
 * entity the game itself uses: the {@link Team} in a team game, the {@link ForceItemPlayer} in
 * solo (where {@code currentTeam()} is null). The countdown is driven by the manager's per-second
 * tick, which runs mid-game only, so it freezes during pause.
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

    private final ForceItemBattle plugin;

    /**
     * Points this round, keyed by scoring entity: a Team in a team game, a ForceItemPlayer in solo.
     * Identity keys — both are the live per-round instances, reused for the whole round.
     */
    private final Map<Object, Integer> points = new LinkedHashMap<>();

    private int secondsLeft = DURATION_SECONDS;

    @Override
    public void start() {
        boolean teamGame = this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM);

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
    public boolean onFoundItem(FoundItemEvent foundItemEvent, ForceItemPlayer forceItemPlayer) {
        if (foundItemEvent.isSkipped()) {
            return false; // skips never score; back-to-backs do
        }

        ItemStack found = foundItemEvent.getFoundItem();
        if (found == null) {
            return false;
        }

        State state = this.plugin.getItemDifficultiesManager().getState(found.getType());
        if (state == null) {
            return false; // not a pool item (custom/unregistered) — no points
        }

        // Team game: everyone has a team, teammates share the tally. Solo: currentTeam() is null,
        // so the player is their own scoring entity.
        Team team = forceItemPlayer.currentTeam();
        Object entity = team != null ? team : forceItemPlayer;
        this.points.merge(entity, pointsFor(state), Integer::sum);
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
     * How long the hunt has left, for the tab footer. Scores are deliberately not shown — the hunt
     * is a race you play by finding items faster, not by watching a board, and the footer already
     * carries pools, jokers and any trader.
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

        List<Map.Entry<Object, Integer>> ranked = this.points.entrySet().stream()
                .sorted(Map.Entry.<Object, Integer>comparingByValue().reversed())
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

        Object winner = ranked.getFirst().getKey();
        String display = this.awardAndDescribe(winner);

        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + display + " <gray>won the "
                + RandomEvents.POINT_HUNT.coloredName() + " <gray>with <yellow>" + topScore + " points<gray>!"));
    }

    /** Pays the winner and returns their display name. Team: 4 split evenly (2/2). Solo: 3. */
    private String awardAndDescribe(Object winner) {
        if (winner instanceof Team team) {
            List<ForceItemPlayer> members = team.getPlayers();
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
            return team.getTeamDisplay();
        }

        ForceItemPlayer solo = (ForceItemPlayer) winner;
        this.giveWheels(solo.player(), SOLO_WHEELS);
        return "<green>" + solo.player().getName();
    }

    private void giveWheels(Player player, int amount) {
        ItemStack wheels = CustomMaterials.WHEEL_OF_FORTUNE.itemStack(amount);
        player.getInventory().addItem(wheels).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
