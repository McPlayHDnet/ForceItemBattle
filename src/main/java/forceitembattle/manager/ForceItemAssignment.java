package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.ScoreOwner;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/**
 * Who is hunting what, and what they hunt next.
 *
 * <p>The one module that decides a Force Item: drawn from the pool at the start of a round, advanced
 * on a find, replaced outright on a skip, and overridden by {@code /forceitem}. It touches nothing
 * but {@link Material}, the {@link Roster} and {@link ScoreOwner}, so all of it is testable with no
 * server behind it — which was the point of lifting it out of {@code Gamemanager}, where it sat
 * beside {@code Bukkit.getOnlinePlayers()} and {@code world.setGameRule} and could not be reached.
 *
 * <p><b>Run mode is the axis everything here turns on.</b> In run mode the whole server races one
 * seeded sequence, so a single draw serves every owner and one find advances all of them; otherwise
 * each owner draws privately and only the finder's owner moves. The flag is passed in at every entry
 * point rather than read from the settings, so a test states the mode it means and nothing is stubbed.
 *
 * <p><b>Once per owner, never once per member.</b> Every loop here walks
 * {@link Roster#activeScoreOwners()}. Walking members instead hands a two-player team two draws to
 * show one item, and advancing twice discards the queued item outright — see the warnings on
 * {@link ScoreOwner#startRound} and {@link ScoreOwner#advance}, which say the same thing from the
 * other side.
 *
 * <p><b>The forced queue is per owner.</b> It used to be one server-wide deque published by a getter,
 * which {@code /forceitem} cleared and filled directly. Because the draw is per owner outside run
 * mode, an admin forcing a row for themselves queued items that the next player to find <em>anything</em>
 * drained instead — the command's own comment promised the row would be "walked through in order",
 * and for the caller it was not. Keyed by owner, that is what it now does. Run mode keeps one shared
 * queue for the same reason it keeps one shared draw: there, every owner really is hunting the same
 * item.
 */
public final class ForceItemAssignment {

    private final Roster roster;
    private final ItemDifficultiesManager items;

    /**
     * Rows queued by {@code /forceitem}, drained as items are handed out. Keyed by owner so one
     * admin's row cannot leak into another player's round; {@link #SHARED_QUEUE} stands in as the key
     * for run mode, where a shared draw means a shared queue.
     */
    private final Map<Object, Deque<Material>> forcedRows = new HashMap<>();

    /** The key every owner shares while the server is racing one seeded sequence. */
    private static final Object SHARED_QUEUE = new Object();

    public ForceItemAssignment(Roster roster, ItemDifficultiesManager items) {
        this.roster = roster;
        this.items = items;
    }

    /**
     * Draws the opening pair for every owner in the round and starts their find clocks.
     *
     * <p>Clears every forced row first: a row left over from the previous round would be handed out
     * as the new round's opening items.
     */
    public void beginRound(boolean runMode) {
        this.forcedRows.clear();

        long now = System.currentTimeMillis();
        Pair shared = runMode ? this.pairFor(null, true) : null;

        this.roster.activeScoreOwners().forEach(owner -> {
            Pair pair = runMode ? shared : this.pairFor(owner, false);
            owner.startRound(pair.current(), pair.next(), now);
        });
    }

    /**
     * Moves whoever this find belongs to onto their next item. In run mode one find advances the
     * whole server, because everybody is racing the same seeded item; otherwise only the finder's
     * owner moves. Team versus solo is the owner's business, not this method's.
     */
    public void advanceFor(ForceItemPlayer finder, boolean runMode) {
        long now = System.currentTimeMillis();

        if (runMode) {
            Material next = this.draw(null, true);
            this.roster.activeScoreOwners().forEach(owner -> owner.advance(next, now));
            return;
        }

        ScoreOwner owner = finder.scoreOwner();
        owner.advance(this.draw(owner, false), now);
    }

    /**
     * Replaces the current item for the whole server with a freshly drawn one — what {@code /voteskip}
     * does when the vote carries, and what {@code /skip} does as an admin override.
     *
     * <p>Charging a joker is <em>not</em> part of this: the only caller that costs one spends it
     * itself, on the initiator. Do not charge one in the loop below — it runs once per owner, so the
     * initiator would pay a joker for each of them.
     */
    public void skipAll(ForceItemPlayer requester, boolean runMode) {
        if (!this.roster.contains(requester.player().getUniqueId())) {
            return;
        }

        Pair pair = this.pairFor(null, runMode);

        // Spectators are already out of activeScoreOwners(), which is what keeps a countdown joiner
        // — who holds no team — from NPE-ing every skip for the rest of the round.
        this.roster.activeScoreOwners()
                .forEach(owner -> owner.assignMaterials(pair.current(), pair.next()));
    }

    /**
     * Hands an owner an explicit row: the first item now, the second queued behind it, the rest
     * drained in order as they are found. A row of one takes a drawn item as its second, so the
     * chain display always has something to show.
     *
     * <p>The row is the whole operation. It used to be spelled out at the call site, which is how the
     * queue came to be reachable — and therefore shared — in the first place.
     *
     * @param row at least one material; anything past the second is queued
     */
    public void force(ScoreOwner owner, List<Material> row, boolean runMode) {
        if (row.isEmpty()) {
            throw new IllegalArgumentException("a forced row needs at least one item");
        }

        Deque<Material> queue = this.queueFor(owner, runMode);
        queue.clear();
        if (row.size() > 2) {
            queue.addAll(row.subList(2, row.size()));
        }

        Material current = row.getFirst();
        // The queue holds only what is past the second item, so a drawn "next" cannot pull from it.
        Material next = row.size() >= 2 ? row.get(1) : this.draw(owner, runMode);

        owner.assignMaterials(current, next);
    }

    /** A drawn item, or the next one this owner has had forced on them. */
    private Material draw(ScoreOwner owner, boolean runMode) {
        Material forced = this.queueFor(owner, runMode).poll();
        if (forced != null) {
            return forced;
        }
        return runMode
                ? this.items.generateSeededRandomMaterial()
                : this.items.generateRandomMaterial();
    }

    private Pair pairFor(ScoreOwner owner, boolean runMode) {
        return new Pair(this.draw(owner, runMode), this.draw(owner, runMode));
    }

    /**
     * The row queued for this owner. Run mode collapses every owner onto one queue, matching its
     * single shared draw; outside it, {@code null} means a draw with no owner in view — the skip-all
     * pair — and gets its own queue rather than any player's.
     */
    private Deque<Material> queueFor(ScoreOwner owner, boolean runMode) {
        Object key = runMode ? SHARED_QUEUE : owner;
        return this.forcedRows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    }

    private record Pair(Material current, Material next) {
    }
}
