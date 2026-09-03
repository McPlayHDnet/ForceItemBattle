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
                mock(TimerManager.class),
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
     * not be able to ask for the odds at all. It takes what {@code handleAfterFind} returns, and a
     * second computation is unavailable to it because {@code BackToBackManager} exposes none.
     */
    @Test
    void cannotComputeTheOddsASecondTime() {
        boolean exposesAProbabilityQuery = List.of(BackToBackManager.class.getMethods()).stream()
                .anyMatch(method -> BackToBackProbability.class.equals(method.getReturnType()));

        assertFalse(exposesAProbabilityQuery,
                "the odds are returned by handleAfterFind and nowhere else; a second entry point "
                        + "lets a caller compute them again at a different streak");
    }
}
