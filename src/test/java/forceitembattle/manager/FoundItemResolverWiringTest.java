package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * That {@link FoundItemResolver} can be built from its collaborators alone.
 *
 * <p>Not a behaviour test — resolving a find drives Bukkit broadcasts and sounds, which still need
 * a server. This pins the thing constructor injection actually bought: the module no longer needs a
 * {@code ForceItemBattle} to exist, so a test can stand it up without the other twenty-two managers
 * being reachable. That was impossible while it held the plugin.
 */
class FoundItemResolverWiringTest {

    @Test
    void buildsFromItsCollaboratorsWithoutAPlugin() {
        FoundItemResolver resolver = new FoundItemResolver(
                mock(GameSettings.class),
                mock(Gamemanager.class),
                mock(ForceItemAssignment.class),
                mock(ScoreboardManager.class),
                mock(BackToBackManager.class),
                mock(RandomEventManager.class),
                new forceitembattle.model.RoundClock(),
                mock(ItemDifficultiesManager.class),
                mock(FIBServiceClient.class));

        assertNotNull(resolver);
    }

    /**
     * The property the injection exists for, guarded so it cannot quietly come back: taking the
     * plugin again would restore the module's reach to all twenty-three managers in one edit, and
     * nothing else would fail.
     */
    @Test
    void namesWhatItNeedsRatherThanReachingThroughThePlugin() {
        List<Class<?>> parameters =
                List.of(FoundItemResolver.class.getDeclaredConstructors()[0].getParameterTypes());

        assertFalse(parameters.contains(ForceItemBattle.class),
                "FoundItemResolver should declare its collaborators, not take the plugin");
    }

    /**
     * One find, one number — pinned at the seam, because this is where it went wrong.
     *
     * <p>{@code score()} used to ask {@code BackToBackManager} for the odds itself, and it ran
     * <em>before</em> {@code handleAfterFind} bumped the streak. So the percentage written to a
     * player's stats row was computed at a shorter chain than the one announced to them a tick later:
     * two numbers for one find, differing systematically rather than racily.
     *
     * <p>Asserted structurally rather than by driving a find, which needs a server: the resolver must
     * not be able to get hold of the odds at all. {@code BackToBackManager} hands them to nobody — it
     * attaches them to the event it schedules, which is the find they actually describe.
     *
     * <p>That last part is the second bug this guards. While {@code handleAfterFind} returned the
     * probability, the resolver recorded it against the find in hand, but the number is computed for
     * the item <em>just handed out</em> — the next find. Each item was stored with its successor's
     * rarity and the closing link of every chain with none, which read as "null" in the result GUI
     * and, since {@code MatchHistoryReporter} drops a null rarity, as a missing back-to-back on the
     * website. A chain of one has nothing but a closing link, so a lone back-to-back disappeared.
     */
    @Test
    void cannotGetTheOddsToRecordAgainstTheWrongFind() {
        boolean handsOutAProbability = List.of(BackToBackManager.class.getMethods()).stream()
                .anyMatch(method -> BackToBackProbability.class.equals(method.getReturnType())
                        || Optional.class.equals(method.getReturnType()));

        assertFalse(handsOutAProbability,
                "BackToBackManager must not return the odds: they belong to the find it schedules, "
                        + "not to the one that called in, and a caller holding them can only record "
                        + "them against the previous item");
    }
}
