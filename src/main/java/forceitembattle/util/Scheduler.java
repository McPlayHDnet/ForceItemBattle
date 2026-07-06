package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;

public final class Scheduler {

    private static ForceItemBattle plugin;

    private Scheduler() {
    }

    public static void init(ForceItemBattle plugin) {
        Scheduler.plugin = plugin;
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runLaterAsync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public static void runLaterSync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void runTimerAsync(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }

    public static void runTimerSync(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

}
