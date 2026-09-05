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
 * <p>Two rules run through all of it. <b>Run mode</b> is the axis: there the whole server races one
 * seeded sequence, so a single draw serves every owner and one find advances all of them; otherwise
 * each owner draws privately. And every loop walks {@link Roster#activeScoreOwners()} — <b>once per
 * owner, never once per member</b>, or a two-player team takes two draws to show one item.
 */
public final class ForceItemAssignment {

    private final Roster roster;
    private final ItemDifficultiesManager items;

    /** Rows queued by {@code /forceitem}, keyed by owner so one admin's row cannot leak into another. */
    private final Map<Object, Deque<Material>> forcedRows = new HashMap<>();

    /** The key every owner shares while the server is racing one seeded sequence. */
    private static final Object SHARED_QUEUE = new Object();

    public ForceItemAssignment(Roster roster, ItemDifficultiesManager items) {
        this.roster = roster;
        this.items = items;
    }

    /** Clears forced rows first: a row left over from last round would open the new one. */
    public void beginRound(boolean runMode) {
        this.forcedRows.clear();

        long now = System.currentTimeMillis();
        Pair shared = runMode ? this.pairFor(null, true) : null;

        this.roster.activeScoreOwners().forEach(owner -> {
            Pair pair = runMode ? shared : this.pairFor(owner, false);
            owner.startRound(pair.current(), pair.next(), now);
        });
    }

    /** In run mode one find advances the whole server; otherwise only the finder's owner moves. */
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
     * Replaces the current item for the whole server — a carried {@code /voteskip}, or {@code /skip}.
     *
     * <p><b>Do not charge a joker in the loop below:</b> it runs once per owner, so the initiator
     * would pay one for each of them. The only caller that costs a joker spends it itself.
     */
    public void skipAll(ForceItemPlayer requester, boolean runMode) {
        if (!this.roster.contains(requester.player().getUniqueId())) {
            return;
        }

        Pair pair = this.pairFor(null, runMode);

        // Spectators are already out of activeScoreOwners(), which keeps a countdown joiner — who
        // holds no team — from NPE-ing every skip for the rest of the round.
        this.roster.activeScoreOwners()
                .forEach(owner -> owner.assignMaterials(pair.current(), pair.next()));
    }

    /**
     * Hands an owner an explicit row: the first item now, the second queued, the rest drained in
     * order. A row of one takes a drawn item as its second so the chain display has something to show.
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

    /** A {@code null} owner is the skip-all pair: a draw with no owner in view, so it gets its own key. */
    private Deque<Material> queueFor(ScoreOwner owner, boolean runMode) {
        Object key = runMode ? SHARED_QUEUE : owner;
        return this.forcedRows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    }

    private record Pair(Material current, Material next) {
    }
}
