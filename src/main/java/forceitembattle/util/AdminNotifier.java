package forceitembattle.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** A repeat is suppressed for a minute, not forever: messages carry names and coordinates, so a permanent set grows unbounded. */
public final class AdminNotifier {

    /** How long a repeat of the same message stays suppressed. */
    static final long REPEAT_WINDOW_MILLIS = 60_000L;

    private final LongSupplier clock;
    private final Map<String, Long> lastSent = new HashMap<>();

    public AdminNotifier() {
        this(System::currentTimeMillis);
    }

    /** Test seam: lets a suite move time without waiting for it. */
    AdminNotifier(LongSupplier clock) {
        this.clock = clock;
    }

    /** Records the message as sent when it returns true. */
    public boolean claim(String message) {
        long now = this.clock.getAsLong();
        this.pruneExpired(now);

        Long previous = this.lastSent.get(message);
        if (previous != null && now - previous < REPEAT_WINDOW_MILLIS) {
            return false;
        }

        this.lastSent.put(message, now);
        return true;
    }

    /** Sends a MiniMessage line to every operator online, at most once per window. */
    public void notifyOps(String message) {
        if (!this.claim(message)) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(Text.of(message));
            }
        }
    }

    private void pruneExpired(long now) {
        Iterator<Map.Entry<String, Long>> entries = this.lastSent.entrySet().iterator();
        while (entries.hasNext()) {
            if (now - entries.next().getValue() >= REPEAT_WINDOW_MILLIS) {
                entries.remove();
            }
        }
    }

    /** Exposed so a test can pin the pruning. */
    int trackedMessages() {
        return this.lastSent.size();
    }
}
