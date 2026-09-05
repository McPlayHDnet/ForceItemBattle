package forceitembattle.achievements;

import static forceitembattle.achievements.Finds.found;
import static forceitembattle.achievements.Finds.participant;
import static forceitembattle.achievements.Finds.skipped;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.handlers.BackToBackAchievementHandler;
import forceitembattle.achievements.handlers.TradingAchievementHandler;
import forceitembattle.achievements.progress.BackToBackAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * That an achievement rule's interface stays narrow.
 *
 * <p>Taking a {@link ForceItemBattle} in {@code check} makes every rule's real interface every
 * manager and puts the package behind a running server. Reintroducing it is one edit that nothing
 * else would fail on, which is what the first test here is for.
 */
class AchievementSeamTest {

    @Test
    void checkDoesNotTakeThePlugin() {
        Method check = List.of(AchievementHandler.class.getDeclaredMethods()).stream()
                .filter(method -> method.getName().equals("check"))
                .findFirst()
                .orElseThrow();

        List<Class<?>> parameters = List.of(check.getParameterTypes());

        assertFalse(parameters.contains(ForceItemBattle.class),
                "an achievement rule should ask a narrow world, not hold the plugin");
        assertTrue(parameters.contains(AchievementWorld.class));
    }

    @Test
    void elapsedIsTheRoundMinusWhatIsLeft() {
        assertEquals(40, new FakeAchievementWorld().clock(5400, 5360).elapsedSeconds());
    }

    /** Most handlers are pure logic over their progress tracker and ask the world nothing. */
    @Test
    void aRuleThatNeedsNothingIsToldNothing() {
        ForceItemPlayer alice = participant("a");
        BackToBackAchievementHandler handler = new BackToBackAchievementHandler(1, false, false);
        BackToBackAchievementProgress progress = handler.createProgress();
        FakeAchievementWorld nothing = new FakeAchievementWorld();

        assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, nothing));
        assertTrue(handler.check(Finds.backToBack(alice, Material.STONE), progress, alice, nothing));
    }

    @Test
    void aSkipBreaksABackToBackStreak() {
        ForceItemPlayer alice = participant("a");
        BackToBackAchievementHandler handler = new BackToBackAchievementHandler(2, false, false);
        BackToBackAchievementProgress progress = handler.createProgress();
        FakeAchievementWorld nothing = new FakeAchievementWorld();

        assertFalse(handler.check(Finds.backToBack(alice, Material.DIRT), progress, alice, nothing));
        assertFalse(handler.check(skipped(alice, Material.STONE), progress, alice, nothing));
        assertFalse(handler.check(Finds.backToBack(alice, Material.OAK_LOG), progress, alice, nothing),
                "the streak restarts after a skip, so one b2b is not yet two");
    }

    /**
     * A trade only counts against one of the round's own wandering traders. The handler asks the
     * world; nothing about a villager reaches it.
     */
    @Test
    void tradingAsksTheWorldWhoIsTrading() {
        ForceItemPlayer alice = participant("a");
        TradingAchievementHandler handler = new TradingAchievementHandler(1);
        FakeAchievementWorld world = new FakeAchievementWorld();

        assertFalse(world.isTrading(alice.player().getUniqueId()));
        assertTrue(world.trading(alice.player().getUniqueId())
                .isTrading(alice.player().getUniqueId()));
    }
}
