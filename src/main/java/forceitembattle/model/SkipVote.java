package forceitembattle.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Material;

/**
 * One skip vote: who may vote, who has, and what the result is.
 *
 * <p><b>The eligible voters are handed in at {@link #open}</b>, not counted from the live roster on
 * each cast. Counting live let a spectator both inflate the quorum and fill it, and left a quorum
 * nobody could reach when a participant disconnected mid-vote.
 */
public final class SkipVote {

    public enum Cast {
        COUNTED,
        CLOSES_THE_VOTE,
        ALREADY_VOTED,
        /** A spectator, or someone who joined after the round began. */
        NOT_ELIGIBLE,
        NO_VOTE_OPEN
    }

    /** @param carried whether the item is skipped: a majority, or the coin flip that settles a tie */
    public record Tally(int yes, int no, boolean tie, boolean carried) {
    }

    private final Random random;

    private final Set<UUID> eligible = new LinkedHashSet<>();
    private final Set<UUID> yesVotes = new LinkedHashSet<>();
    private final Set<UUID> noVotes = new LinkedHashSet<>();

    private boolean open;
    private UUID initiator;
    private Material material;

    public SkipVote(Random random) {
        this.random = random;
    }

    public SkipVote() {
        this(new Random());
    }

    /**
     * Opens a vote on {@code material}, with the initiator's own YES already cast. Deliberately does
     * not close itself when they are the only eligible voter: a vote of one would resolve before
     * anyone could read the message announcing it.
     */
    public void open(UUID initiator, Material material, Collection<UUID> eligible) {
        this.eligible.clear();
        this.eligible.addAll(eligible);
        this.yesVotes.clear();
        this.noVotes.clear();

        this.initiator = initiator;
        this.material = material;
        this.open = true;
        this.yesVotes.add(initiator);
    }

    public boolean isOpen() {
        return this.open;
    }

    @Nullable
    public UUID initiator() {
        return this.initiator;
    }

    @Nullable
    public Material material() {
        return this.material;
    }

    public Cast cast(UUID voter, boolean voteYes) {
        if (!this.open) {
            return Cast.NO_VOTE_OPEN;
        }
        if (!this.eligible.contains(voter)) {
            return Cast.NOT_ELIGIBLE;
        }
        if (this.yesVotes.contains(voter) || this.noVotes.contains(voter)) {
            return Cast.ALREADY_VOTED;
        }

        (voteYes ? this.yesVotes : this.noVotes).add(voter);

        return this.yesVotes.size() + this.noVotes.size() >= this.eligible.size()
                ? Cast.CLOSES_THE_VOTE
                : Cast.COUNTED;
    }

    /** A tie is broken by a coin flip, which is why the {@code Random} is a constructor parameter. */
    public Tally close() {
        this.open = false;

        int yes = this.yesVotes.size();
        int no = this.noVotes.size();
        boolean tie = yes == no;

        return new Tally(yes, no, tie, yes > no || (tie && this.random.nextBoolean()));
    }

    /** No result: nothing is skipped and nothing is charged. */
    public void cancel() {
        this.open = false;
        this.initiator = null;
        this.material = null;
        this.eligible.clear();
        this.yesVotes.clear();
        this.noVotes.clear();
    }
}
