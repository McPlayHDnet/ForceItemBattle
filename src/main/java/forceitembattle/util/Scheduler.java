package forceitembattle.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Static on purpose, the one exemption from {@code NoServiceLocatorTest}. The repeating pair takes a
 * {@link BukkitRunnable} so bodies can {@code cancel()} themselves and still return a {@link BukkitTask}.
 */
public final class Scheduler {

    private static Plugin plugin;

    private Scheduler() {
    }

    public static void init(Plugin plugin) {
        Scheduler.plugin = plugin;
    }

    /** For tests' {@code @AfterEach}: the static field outlives the mocked server, which leaks between test classes. */
    public static void reset() {
        Scheduler.plugin = null;
    }

    public static BukkitTask runAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static BukkitTask runSync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
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
