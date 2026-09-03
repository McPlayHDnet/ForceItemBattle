package forceitembattle.model;

import java.util.OptionalInt;
import org.bukkit.Material;

/**
 * Spending a joker. See {@code CONTEXT.md § Joker}.
 *
 * <p><b>These methods mutate</b> — they charge the pool themselves rather than returning a decision,
 * so a caller cannot compute a verdict and forget to pay for it.
 */
public sealed interface JokerSpend {

    /** @param stackAmount what the joker stack should now read; {@code 0} means remove it */
    record Spent(Material handedOver, int stackAmount) implements JokerSpend {
    }

    record Exhausted() implements JokerSpend {
    }

    record NoStackInHand() implements JokerSpend {
    }

    /** @param stackInHand the joker stack they are holding, or empty if they hold none */
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
     * An empty pool is not refused: {@code spendJoker} floors at zero, so a vote by someone with no
     * jokers costs nothing and still succeeds.
     *
     * @return what the joker stack should now read; {@code 0} means remove it
     */
    static int charge(ForceItemPlayer forceItemPlayer, OptionalInt stackInHand) {
        return chargeAndRestack(forceItemPlayer, stackInHand.orElse(0));
    }

    /** On a team the pool is shared, so the stack loses one; solo, the stack size *is* the count. */
    private static int chargeAndRestack(ForceItemPlayer forceItemPlayer, int stackInHand) {
        int jokersLeft = forceItemPlayer.spendJoker();
        return forceItemPlayer.isInTeam() ? Math.max(0, stackInHand - 1) : jokersLeft;
    }
}
