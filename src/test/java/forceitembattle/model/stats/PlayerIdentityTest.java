package forceitembattle.model.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The identity rendering rule, which existed three times before it existed once.
 *
 * <p>{@code CommandStats} and {@code CommandLeaderboard} held byte-identical copies;
 * {@code AchievementInventory} held a third that differed only in its fallback string. Three copies
 * of a rule agreeing on everything but one literal is the state just before they start disagreeing
 * about something that matters.
 */
class PlayerIdentityTest {

    private static final UUID UUID_A =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void aKnownNameIsUsed() {
        assertEquals("Steve",
                PlayerIdentity.displayName(new PlayerIdentity(UUID_A, "Steve"), "?"));
    }

    /** The service can know the id without the name; eight characters is enough to tell them apart. */
    @Test
    void anUnknownNameFallsBackToTheFrontOfTheUuid() {
        assertEquals("aaaaaaaa",
                PlayerIdentity.displayName(new PlayerIdentity(UUID_A, null), "?"));
    }

    /**
     * The only thing the three copies disagreed about, now a parameter rather than a fork: a stats
     * line says "?", a GUI lore line says "Unknown".
     */
    @Test
    void noIdentityAtAllUsesTheCallersWording() {
        assertEquals("?", PlayerIdentity.displayName(null, "?"));
        assertEquals("Unknown", PlayerIdentity.displayName(null, "Unknown"));
    }

    @Test
    void anIdentityWithoutAUuidIsNoIdentity() {
        assertEquals("Unknown",
                PlayerIdentity.displayName(new PlayerIdentity(null, "Steve"), "Unknown"));
    }
}
