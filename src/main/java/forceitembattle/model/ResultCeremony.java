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
 * <p>Depends on nothing — no Bukkit, no plugin, no managers. Page maps are opaque: stored, handed
 * back, never looked inside, so {@link ItemStack} appears in the signature and nowhere else.
 *
 * <p>The archive keys on the {@link ScoreOwner} <em>instance</em>, which holds only because an
 * existing roster entry always wins over every default — a player reconnecting at END_GAME gets their
 * original {@code ForceItemPlayer} back. If {@code Roster.admit} stops guaranteeing that, this breaks
 * quietly.
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
     * @param order worst-placed first. Spectators are already excluded and ties already broken by the
     *              caller; this is deliberately not the ordering the stats write uses.
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

    /** Advances on every call: this is the reveal being handed out, not a peek. */
    public Optional<Reveal> nextReveal() {
        if (this.next >= this.order.size()) {
            return Optional.empty();
        }
        return Optional.of(this.order.get(this.next++));
    }

    public boolean isFinished() {
        return this.next >= this.order.size();
    }

    /** Stores the paged screen built for an owner, so {@code /result <id>} can reopen it. */
    public void archive(ScoreOwner owner, Map<Integer, Map<Integer, ItemStack>> pages) {
        this.pagesByOwner.put(owner, pages);
    }

    public Optional<Map<Integer, Map<Integer, ItemStack>>> pagesFor(@Nullable ScoreOwner owner) {
        return owner == null ? Optional.empty() : Optional.ofNullable(this.pagesByOwner.get(owner));
    }

    /**
     * Builds the reveal order from the places, worst first. Takes places rather than computing them
     * so this module stays free of the roster and team manager. The map is expected best-first,
     * which is what {@link Standings} returns.
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
     * @param last whether this is the winner — the reveal after which the stats link may go out. The
     *             ceremony decides which turn that is; what happens then is the caller's.
     */
    public record Reveal(ScoreOwner owner, int place, boolean last) {

        public Reveal {
            Objects.requireNonNull(owner, "owner");
        }
    }
}
