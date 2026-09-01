package forceitembattle.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;

/**
 * The end-of-round reveal: who is shown next, and the screens already shown.
 *
 * <p>It was three things in three places — the walk inside {@code CommandResult} (which held
 * {@code public int place} and a match id as command state, and wrote the same walk out twice for
 * solo and for teams), the archive as two maps on {@code Gamemanager} that nothing in
 * {@code Gamemanager} ever read, and the winner hook as a ternary building a {@code Runnable}.
 *
 * <p>Like {@link Roster} and {@link RoundPhase}, this depends on nothing — no Bukkit behaviour, no
 * plugin, no managers. The order is handed in at {@link #beginFor}, already built, rather than
 * computed here from a roster and a team manager.
 *
 * <h2>The pages are opaque</h2>
 *
 * <p>A page map is stored and handed back and never looked inside; {@link ItemStack} appears in the
 * signature and nowhere else. Keep it that way — reaching into a page would put this module behind
 * a running server, which is the whole reason the walk was untestable before.
 *
 * <h2>Keyed by identity</h2>
 *
 * <p>The archive keys on the {@link ScoreOwner} instance. That is safe for the length of a round
 * because an existing roster entry always wins over every default — see {@code CONTEXT.md § Roster}
 * — so a player reconnecting at END_GAME gets their original {@code ForceItemPlayer} back, and its
 * {@code SoloScore} is final. If {@code Roster.admit} ever stops guaranteeing that, this breaks
 * quietly, which is why it is written down here.
 */
public final class ResultCeremony {

    private final Map<ScoreOwner, Map<Integer, Map<Integer, ItemStack>>> pagesByOwner =
            new IdentityHashMap<>();

    private List<Reveal> order = List.of();
    private int next;

    @Nullable
    private UUID matchId;

    /**
     * Starts the ceremony for a match, discarding anything left from the previous one.
     *
     * @param order worst-placed first, which is the order the reveal walks. Spectators are already
     *              excluded and ties already broken by the caller — see {@code CONTEXT.md §
     *              Result Ceremony} for why that ordering is not the one the stats write uses.
     */
    public void beginFor(UUID matchId, List<Reveal> order) {
        this.matchId = matchId;
        this.order = List.copyOf(order);
        this.next = 0;
        this.pagesByOwner.clear();
    }

    /** The match this ceremony belongs to, or {@code null} before the first round finishes. */
    @Nullable
    public UUID matchId() {
        return this.matchId;
    }

    /**
     * The next owner to reveal, or empty once the winner has been shown.
     *
     * <p>Advances on every call: this is the reveal being handed out, not a peek.
     */
    public Optional<Reveal> nextReveal() {
        if (this.next >= this.order.size()) {
            return Optional.empty();
        }
        return Optional.of(this.order.get(this.next++));
    }

    /** Whether every owner has been revealed. */
    public boolean isFinished() {
        return this.next >= this.order.size();
    }

    /** Stores the paged screen built for an owner, so {@code /result <id>} can reopen it. */
    public void archive(ScoreOwner owner, Map<Integer, Map<Integer, ItemStack>> pages) {
        this.pagesByOwner.put(owner, pages);
    }

    /** The stored screen for an owner, or empty if none was ever built. */
    public Optional<Map<Integer, Map<Integer, ItemStack>>> pagesFor(@Nullable ScoreOwner owner) {
        return owner == null ? Optional.empty() : Optional.ofNullable(this.pagesByOwner.get(owner));
    }

    /**
     * Builds the reveal order from the places, worst first.
     *
     * <p>Takes places rather than computing them so this module stays free of the roster and the
     * team manager. The map is expected best-first, which is what {@link Standings} returns.
     */
    public static <T extends ScoreOwner> List<Reveal> orderFrom(Map<T, Integer> placesBestFirst) {
        List<Map.Entry<T, Integer>> entries = new ArrayList<>(placesBestFirst.entrySet());
        List<Reveal> reveals = new ArrayList<>(entries.size());

        for (int index = entries.size() - 1; index >= 0; index--) {
            Map.Entry<T, Integer> entry = entries.get(index);
            reveals.add(new Reveal(entry.getKey(), entry.getValue(), index == 0));
        }
        return List.copyOf(reveals);
    }

    /**
     * One owner's turn in the ceremony.
     *
     * @param last whether this is the winner — the reveal after which the stats link may go out.
     *             The ceremony decides which turn that is; what happens then is the caller's.
     */
    public record Reveal(ScoreOwner owner, int place, boolean last) {

        public Reveal {
            Objects.requireNonNull(owner, "owner");
        }
    }
}
