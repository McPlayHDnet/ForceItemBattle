package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * The plugin's entry point to the Bukkit scheduler, so no caller has to hold a plugin reference
 * just to schedule something.
 *
 * Every method returns the {@link BukkitTask} rather than void, so a caller that needs to cancel
 * later (a vote timer, a repeating announcement) never has to drop down to
 * {@code Bukkit.getScheduler()} for the handle.
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

}
