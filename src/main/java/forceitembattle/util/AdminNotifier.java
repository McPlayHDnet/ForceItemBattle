package forceitembattle.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Tells the operators about something suspicious, without saying the same thing over and over.
 *
 * <h2>Why this is a window and not a set</h2>
 *
 * The rule this replaces kept every message it had ever sent in an {@code ArrayList} and skipped
 * anything already in it. Two things followed, neither intended:
 *
 * <ul>
 *   <li>The list never shrank. Messages carry player names and block coordinates, so the number of
 *       distinct strings is effectively unbounded, and it grew for the life of the server.</li>
 *   <li>A message was shown <em>once, ever</em>. The second time somebody mined at another player's
 *       bed on the same block, nobody was told — including in a later round on the same server.</li>
 * </ul>
 *
 * <p>The intent was plainly "do not spam", not "mention it once and never again", so this keeps the
 * anti-spam behaviour and drops the permanence: a repeat is suppressed for a minute, then reported
 * again. Expired entries are pruned as they are passed, so the map stays the size of what is
 * actually happening rather than of everything that ever has.
 */
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

    /**
     * Whether this message should go out now, recording it as sent when so.
     *
     * <p>Separate from the sending because this is the whole rule, and reaching Bukkit to check it
     * would put it behind a running server.
     */
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

    /** How many messages are currently being suppressed. Exposed so a test can pin the pruning. */
    int trackedMessages() {
        return this.lastSent.size();
    }
}
