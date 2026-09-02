package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * That {@link Scheduler} can be driven from a test at all, and how.
 *
 * <p>This file exists because the codebase spent a full pass believing the opposite. Three comments
 * — in {@code CommandPositionTest}, {@code ClickableItemsListenerTest} and {@code RESUME.md} — said
 * scheduled work was out of reach because it "needs a real registered plugin". That was true of the
 * Mockito-only harness and stopped being true the day MockBukkit arrived, but nothing re-checked it,
 * so the claim outlived its own expiry and cost real coverage: every delayed body in the plugin was
 * written off as untestable on the strength of a stale sentence.
 *
 * <p>So the point of these four tests is not the assertions, which are close to trivial. It is that
 * the idiom below is executable and therefore cannot quietly stop being true:
 *
 * <pre>
 *   Scheduler.init(MockBukkit.createMockPlugin());        // in setup
 *   server.getScheduler().performTicks(n);                // to advance
 *   Scheduler.reset();                                    // in teardown, beside unmock()
 * </pre>
 *
 * <p>The teardown half is not decoration. {@code Scheduler}'s plugin is static and outlives the
 * server a test class mocks, so without {@link Scheduler#reset()} the next class to schedule hands
 * its work to a torn-down plugin and passes or fails on class ordering.
 */
class SchedulerHarnessTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        Scheduler.init(MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("a delayed body waits for its delay, then runs")
    void delayedSyncBodyRunsWhenTicksArePerformed() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Scheduler.runLaterSync(() -> ran.set(true), 20L);
        assertFalse(ran.get(), "should not have run before the delay elapses");

        this.server.getScheduler().performTicks(21L);
        assertTrue(ran.get(), "should have run once 21 ticks passed");
    }

    @Test
    @DisplayName("an async body runs, and the harness can wait for it")
    void asyncBodyRuns() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Scheduler.runAsync(() -> ran.set(true));
        this.server.getScheduler().waitAsyncTasksFinished();

        assertTrue(ran.get());
    }

    @Test
    @DisplayName("the returned handle cancels — the external-cancel idiom")
    void returnedHandleCancels() {
        AtomicInteger runs = new AtomicInteger();

        BukkitTask task = Scheduler.runLaterSync(runs::incrementAndGet, 20L);
        task.cancel();
        this.server.getScheduler().performTicks(40L);

        assertEquals(0, runs.get(), "a cancelled task must not run");
    }

    /**
     * The reason {@link Scheduler#runTimerSync} takes a {@code BukkitRunnable} and not a
     * {@code Runnable}: nine of the plugin's repeating bodies end themselves this way, and only a
     * {@code BukkitRunnable} can reach its own task to do it.
     */
    @Test
    @DisplayName("a repeating body can cancel itself from inside")
    void repeatingTaskTicksAndSelfCancels() {
        AtomicInteger runs = new AtomicInteger();

        Scheduler.runTimerSync(new BukkitRunnable() {
            @Override
            public void run() {
                if (runs.incrementAndGet() >= 3) {
                    this.cancel();
                }
            }
        }, 0L, 20L);

        this.server.getScheduler().performTicks(200L);
        assertEquals(3, runs.get(), "a self-cancelling repeating task should stop at 3");
    }
}
