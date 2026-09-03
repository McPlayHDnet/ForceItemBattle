package forceitembattle.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * The plugin's entry point to the Bukkit scheduler.
 *
 * <p>The repeating pair takes a {@link BukkitRunnable}, not a {@link Runnable}, and that is
 * load-bearing: nine repeating bodies end with {@code this.cancel()} and three more are cancelled
 * from outside through the returned {@link BukkitTask}. Bukkit's {@code Consumer<BukkitTask>}
 * overload serves the first group but returns void, so it cannot serve the second.
 *
 * <p><b>Deliberately static</b> — the one module exempt from the rule {@code NoServiceLocatorTest}
 * pins, because injecting a stateless facility with no alternative implementation would put a
 * constructor parameter on every class that schedules.
 */
public final class Scheduler {

    private static Plugin plugin;

    private Scheduler() {
    }

    public static void init(Plugin plugin) {
        Scheduler.plugin = plugin;
    }

    /**
     * For tests, from {@code @AfterEach} beside {@code unmock()}. This field is static and outlives
     * the server a test class mocks, so without it the next class to schedule hands work to a
     * torn-down plugin and passes or fails on class ordering.
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
