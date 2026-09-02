package forceitembattle.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * The plugin's entry point to the Bukkit scheduler, so no caller has to hold a plugin reference
 * just to schedule something.
 *
 * <p>Every method returns the {@link BukkitTask} rather than void, so a caller that needs to cancel
 * later (a vote timer, a repeating announcement) never has to drop down to
 * {@code Bukkit.getScheduler()} for the handle.
 *
 * <p>The repeating pair takes a {@link BukkitRunnable} rather than a {@link Runnable}, which is the
 * one place this class is deliberately wider than it looks. Nine of the plugin's repeating bodies
 * end themselves with {@code this.cancel()} and three more are cancelled from outside through the
 * stored handle; a {@code Runnable} serves the first group only by making each one reach itself
 * through a holder array, and Bukkit's {@code Consumer<BukkitTask>} overload serves the second
 * group not at all, because it returns void. {@code BukkitRunnable} in and {@code BukkitTask} out
 * is the only shape both groups can use.
 *
 * <p><b>Deliberately static.</b> This is the one module exempt from the rule
 * {@code NoServiceLocatorTest} pins, and the exemption is the point: were it injected, every class
 * that schedules would take a constructor parameter to reach a facility with no state and no
 * alternative implementation. Tests drive it through MockBukkit's own scheduler rather than a
 * stand-in — see {@code SchedulerHarnessTest} for the idiom, which is
 * {@code Scheduler.init(MockBukkit.createMockPlugin())} in setup and
 * {@code server.getScheduler().performTicks(n)} to advance. {@code NoPluginForSchedulingTest}
 * enforces the other half: holding a {@code Plugin} solely to schedule is what this class exists to
 * make unnecessary.
 */
public final class Scheduler {

    private static Plugin plugin;

    private Scheduler() {
    }

    public static void init(Plugin plugin) {
        Scheduler.plugin = plugin;
    }

    /**
     * Drops the plugin reference.
     *
     * <p>For tests. {@link #plugin} is static and so outlives the server a test class mocks: after
     * {@code MockBukkit.unmock()} it still points at a torn-down {@code PluginMock}, and the next
     * test class to schedule would hand its work to a dead plugin and pass or fail on class
     * ordering. Called from {@code @AfterEach} beside {@code unmock()}.
     */
    public static void reset() {
        Scheduler.plugin = null;
    }

    public static BukkitTask runAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static BukkitTask runSync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static BukkitTask runLaterAsync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public static BukkitTask runLaterSync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static BukkitTask runTimerSync(BukkitRunnable runnable, long delay, long period) {
        return runnable.runTaskTimer(plugin, delay, period);
    }

    public static BukkitTask runTimerAsync(BukkitRunnable runnable, long delay, long period) {
        return runnable.runTaskTimerAsynchronously(plugin, delay, period);
    }

}
