package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * No class may reach Bukkit's scheduler directly. {@link forceitembattle.util.Scheduler} is the
 * plugin's one entry point to it, and holding a {@code Plugin} in order to schedule is exactly what
 * that module exists to make unnecessary.
 *
 * <p>The rule went half-kept for a long time because {@code Scheduler} covered only the one-shot
 * half of the API. It had no repeating method at all, so fourteen sites called
 * {@code new BukkitRunnable(){…}.runTaskTimer(plugin, …)} straight through it, and seven modules
 * carried a {@code Plugin} field for no other reason — {@code LocatorManager}'s said so on the
 * field: <i>"Kept for runTaskTimerAsynchronously, which needs a Plugin."</i> Adding
 * {@code runTimerSync}/{@code runTimerAsync} removed the excuse; this removes the drift.
 *
 * <p>Pinned by reading the source rather than by behaviour, for the same reason
 * {@link NoServiceLocatorTest} is: reintroducing one {@code .runTaskTimer(plugin, …)} compiles and
 * passes every other test.
 *
 * <p>Two modules legitimately keep a {@code Plugin} and are not exempted here, because they do not
 * schedule through it: {@code TimerManager} reads and writes {@code config.yml}, and
 * {@code WanderingTraderManager} logs a warning. This test only forbids the scheduling calls, so
 * both pass on their own merits rather than by being listed.
 */
class NoPluginForSchedulingTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/forceitembattle");

    /** The class that is allowed to talk to Bukkit's scheduler, because it is the seam. */
    private static final String SCHEDULER = "Scheduler.java";

    /**
     * Any of Bukkit's scheduling entry points: the {@code BukkitRunnable.runTask*} family, and
     * {@code Bukkit.getScheduler()} for the callers that would otherwise go around it.
     */
    private static final Pattern SCHEDULES_DIRECTLY = Pattern.compile(
            "\\.runTask(Later|Timer)?(Asynchronously)?\\s*\\(|getScheduler\\s*\\(\\s*\\)");

    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    @Test
    void onlySchedulerTalksToBukkitsScheduler() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (var paths = Files.walk(SOURCE_ROOT)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals(SCHEDULER)) {
                    continue;
                }
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = SCHEDULES_DIRECTLY.matcher(stripComments(source));
                if (matcher.find()) {
                    offenders.add(SOURCE_ROOT.relativize(file) + " -> " + matcher.group().trim());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "These schedule without going through Scheduler, which is the only class that may. "
                        + "Use Scheduler.runSync/runAsync/runLaterSync/runLaterAsync/runTimerSync/"
                        + "runTimerAsync instead: " + offenders);
    }

    private static String stripComments(String source) {
        String stripped = BLOCK_COMMENT.matcher(source).replaceAll("");
        return LINE_COMMENT.matcher(stripped).replaceAll("");
    }
}
