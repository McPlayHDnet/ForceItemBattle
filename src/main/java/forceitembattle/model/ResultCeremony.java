package forceitembattle.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;

/**
 * The archive keys on the ScoreOwner instance, which holds only because Roster.admit always returns
 * an existing entry to a reconnecting player.
 */
public final class ResultCeremony {

    private final Map<ScoreOwner, Map<Integer, Map<Integer, ItemStack>>> pagesByOwner =
            new IdentityHashMap<>();

    private List<Reveal> order = List.of();
    private int next;

    @Nullable
    private UUID matchId;

    /** @param order worst-placed first, spectators excluded and ties already broken */
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

    /** Every owner, best first. */
    public List<Reveal> standings() {
        return this.order.stream()
                .sorted(Comparator.comparingInt(Reveal::place))
                .toList();
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

    /** Expects the places best-first, as {@link Standings} returns them. */
    public static <T extends ScoreOwner> List<Reveal> orderFrom(Map<T, Integer> placesBestFirst) {
        List<Map.Entry<T, Integer>> entries = new ArrayList<>(placesBestFirst.entrySet());
        List<Reveal> reveals = new ArrayList<>(entries.size());

        for (int index = entries.size() - 1; index >= 0; index--) {
            Map.Entry<T, Integer> entry = entries.get(index);
            reveals.add(new Reveal(entry.getKey(), entry.getValue(), index == 0));
        }
        return List.copyOf(reveals);
    }

    /** @param last whether this is the winner, after which the stats link may go out */
    public record Reveal(ScoreOwner owner, int place, boolean last) {

        public Reveal {
            Objects.requireNonNull(owner, "owner");
        }
    }
}
