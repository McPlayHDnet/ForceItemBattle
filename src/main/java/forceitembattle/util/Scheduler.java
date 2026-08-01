package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * The plugin's entry point to the Bukkit scheduler, so no caller has to hold a plugin reference
 * just to schedule something.
 *
 * Every method returns the {@link BukkitTask} rather than void — a caller that needs to cancel
 * later (a vote timer, a repeating announcement) was previously forced back to
 * {@code Bukkit.getScheduler()} because these did not hand the handle back, which is most of why
 * the util was bypassed at twenty-odd call sites.
 */
public final class Scheduler {

    private static ForceItemBattle plugin;

    private Scheduler() {
    }

    public static void init(ForceItemBattle plugin) {
        Scheduler.plugin = plugin;
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

    public static BukkitTask runTimerAsync(Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }

    public static BukkitTask runTimerSync(Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

}
