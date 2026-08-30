package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
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
}
