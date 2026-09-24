package forceitembattle.ceremony;

import forceitembattle.util.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * One timer for the whole stage, so tearing it down cancels every pending step at once rather than
 * leaving dozens of delayed tasks to fire into removed entities.
 */
final class Cues {

    private final NavigableMap<Long, List<Runnable>> byTick = new TreeMap<>();
    private long now;
    @Nullable
    private BukkitTask task;

    /** Runs {@code action} {@code delay} ticks from now; a cue added from inside a cue counts from its tick. */
    void at(long delay, Runnable action) {
        this.byTick.computeIfAbsent(this.now + Math.max(0, delay), tick -> new ArrayList<>()).add(action);
        if (this.task == null) {
            this.task = Scheduler.runTimerSync(new BukkitRunnable() {
                @Override
                public void run() {
                    tick();
                }
            }, 1L, 1L);
        }
    }

    void clear() {
        this.byTick.clear();
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    // One failing step must not take the rest of the ceremony with it, or a reveal never completes.
    private static void runGuarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            Bukkit.getLogger().log(Level.WARNING, "A result stage step failed", exception);
        }
    }

    private void tick() {
        this.now++;
        while (!this.byTick.isEmpty() && this.byTick.firstKey() <= this.now) {
            this.byTick.pollFirstEntry().getValue().forEach(Cues::runGuarded);
        }
        if (this.byTick.isEmpty() && this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }
}
