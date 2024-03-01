package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;

public class Scheduler {

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(
                ForceItemBattle.getInstance(),
                runnable
        );
    }

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(
                ForceItemBattle.getInstance(),
                runnable
        );
    }

    public static void runLaterAsync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                ForceItemBattle.getInstance(),
                runnable,
                delay
        );
    }

    public static void runLaterSync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(
                ForceItemBattle.getInstance(),
                runnable,
                delay
        );
    }

    public static void runTimerAsync(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                ForceItemBattle.getInstance(),
                runnable,
                delay,
                period
        );
    }

    public static void runTimerSync(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(
                ForceItemBattle.getInstance(),
                runnable,
                delay,
                period
        );
    }

}
