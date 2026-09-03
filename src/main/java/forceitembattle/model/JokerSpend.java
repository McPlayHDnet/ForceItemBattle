package forceitembattle.model;

import java.util.OptionalInt;
import org.bukkit.Material;

/**
 * Spending a joker: what it costs, what it hands back, and what the button should read afterwards.
 *
 * <p>A joker is a skip. The count that gates one lives on the {@link ScoreOwner}; the hotbar stack is
 * only the button. Keeping those two in step is the whole job of this module, and it exists because
 * they were not: the click path charged the pool <em>and</em> rewrote the stack, while
 * {@code VoteSkipManager} charged the pool and left the stack alone — so after a vote the button read
 * one too high, and {@code /fixskips} running on respawn quietly repaired it. Four writers of the
 * stack against one writer of the count is how a divergence like that survives.
 *
 * <p><b>These methods mutate.</b> They call {@link ForceItemPlayer#spendJoker()} themselves rather
 * than returning a decision for the caller to act on, which is deliberate and the exception to this
 * codebase's preference for pure rules: a caller that computes a verdict and then forgets to charge
 * is precisely the bug above, and the returned stack amount would silently disagree with an
 * uncharged pool.
 *
 * <p><b>Two entry points, because the two callers want different amounts of the same operation.</b>
 * A click spends, hands the player their current item, and fires a skipped {@code FoundItemEvent}; a
 * carried vote spends and nothing else, because {@code ForceItemAssignment.skipAll} replaces the item
 * for everybody. {@link #charge} therefore returns only the new stack amount — handing the vote path
 * a {@link Spent} whose material it must know to discard is how the initiator would end up with a
 * free item.
 *
 * <p>One asymmetry survives this and is deliberate: <b>a carried vote charges only the initiator</b>
 * while replacing everyone's item. {@code skipAll}'s javadoc explains why — charging inside its loop
 * would bill the initiator once per owner.
 */
public sealed interface JokerSpend {

    /**
     * The joker was spent.
     *
     * @param handedOver the item they were hunting, which the caller gives them
     * @param stackAmount what the joker stack should now read; {@code 0} means remove it
     */
    record Spent(Material handedOver, int stackAmount) implements JokerSpend {
    }

    /**
     * No jokers left. The button should not exist, so the caller strips it — this is the one refusal
     * that repairs something.
     */
    record Exhausted() implements JokerSpend {
    }

    /** They are not holding a joker. Nothing happened and nothing needs saying. */
    record NoStackInHand() implements JokerSpend {
    }

    /**
     * Spends one joker for this player's Score Owner and says what the button should read.
     *
     * <p>An empty pool beats an absent stack: someone with no jokers <em>and</em> nothing in hand gets
     * {@link Exhausted}, not {@link NoStackInHand}. That ordering is the click handler's current
     * behaviour, moved somewhere it can be asserted.
     *
     * @param stackInHand the size of the joker stack they are holding, or empty if they hold none
     */
    static JokerSpend spend(ForceItemPlayer forceItemPlayer, OptionalInt stackInHand) {
        if (forceItemPlayer.activeJokers() <= 0) {
            return new Exhausted();
        }
        if (stackInHand.isEmpty()) {
            return new NoStackInHand();
        }

        Material hunted = forceItemPlayer.activeMaterial();
        return new Spent(hunted, chargeAndRestack(forceItemPlayer, stackInHand.getAsInt()));
    }

    /**
     * Charges one joker without handing anything over — what a carried vote costs its initiator.
     *
     * <p><b>An empty pool is not refused here.</b> {@code spendJoker} floors at zero, so a vote by
     * someone with no jokers left costs nothing and still succeeds. That is unchanged behaviour and a
     * gameplay rule belonging to {@code /voteskip} rather than to this module — see
     * {@code CONTEXT.md § Joker} for the note.
     *
     * @return what the joker stack should now read; {@code 0} means remove it
     */
    static int charge(ForceItemPlayer forceItemPlayer, OptionalInt stackInHand) {
        return chargeAndRestack(forceItemPlayer, stackInHand.orElse(0));
    }

    /**
     * The arithmetic both paths share, and the reason this module is worth having.
     *
     * <p>In a team game the pool is shared between members, so this player's stack loses only the one
     * they just spent. Solo, the stack size <em>is</em> the remaining count, so it becomes it. Getting
     * that backwards either shows a team member the whole team's pool or silently multiplies a solo
     * player's skips, and it lived behind an {@code ItemStack} where no test could reach it.
     */
    private static int chargeAndRestack(ForceItemPlayer forceItemPlayer, int stackInHand) {
        int jokersLeft = forceItemPlayer.spendJoker();
        return forceItemPlayer.isInTeam() ? Math.max(0, stackInHand - 1) : jokersLeft;
    }
}
