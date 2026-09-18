package forceitembattle.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.plugin.Plugin;

public class SeedPool {

    // Version guard: must match the pool's manifest.txt header.
    private static final String EXPECTED_MC    = "26.2";
    private static final String EXPECTED_FLAGS = "0";
    private static final String EXPECTED_POINT = "0,63,0";

    private final Plugin plugin;
    private final File dir;
    private final Set<String> groups = new TreeSet<>(); // sorted, lowercase names
    private boolean available = false;

    public SeedPool(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "seeds");
    }

    /** (Re)reads and validates the manifest. Safe to call again to reload. */
    public void load() {
        this.available = false;
        this.groups.clear();

        File manifest = new File(this.dir, "manifest.txt");
        if (!this.dir.isDirectory() || !manifest.isFile()) {
            this.plugin.getLogger().warning("[seeds] '" + this.dir.getPath()
                    + "' or its manifest.txt is missing — /reset <biome> disabled.");
            return;
        }

        String mc = null, flags = null, point = null;
        Set<String> parsed = new TreeSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(manifest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                switch (key) {
                    case "mc":        mc = val;    break;
                    case "flags":     flags = val; break;
                    case "point":     point = val; break;
                    case "target":
                    case "generated": break; // informational only
                    default:          parsed.add(key.toLowerCase(Locale.ROOT)); // <name>=<count>
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("[seeds] failed to read manifest: " + e.getMessage()
                    + " — /reset <biome> disabled.");
            return;
        }

        if (!EXPECTED_MC.equals(mc) || !EXPECTED_FLAGS.equals(flags) || !EXPECTED_POINT.equals(point)) {
            this.plugin.getLogger().warning("[seeds] manifest signature mismatch (mc=" + mc
                    + " flags=" + flags + " point=" + point + "; expected mc=" + EXPECTED_MC
                    + " flags=" + EXPECTED_FLAGS + " point=" + EXPECTED_POINT
                    + ") — /reset <biome> disabled. Regenerate the pool, or update EXPECTED_* if this was intentional.");
            return;
        }
        if (parsed.isEmpty()) {
            this.plugin.getLogger().warning("[seeds] manifest lists no biomes — /reset <biome> disabled.");
            return;
        }

        this.groups.addAll(parsed);
        this.available = true;
        this.plugin.getLogger().info("[seeds] loaded " + this.groups.size()
                + " biome pools (mc=" + mc + ").");
    }

    public boolean isAvailable() {
        return this.available;
    }

    public boolean has(String group) {
        return this.available && this.groups.contains(group.toLowerCase(Locale.ROOT));
    }

    /** Sorted, lowercase biome/group names available for /reset. */
    public Set<String> groups() {
        return Collections.unmodifiableSet(this.groups);
    }

    /**
     * Returns a uniformly random seed from the group's file using reservoir
     * sampling (single pass, O(1) memory — the files can hold a million lines).
     *
     * @throws IOException if the file is missing, unreadable, or empty
     */
    public long randomSeed(String group) throws IOException {
        File file = new File(this.dir, group.toLowerCase(Locale.ROOT) + ".txt");
        if (!file.isFile()) {
            throw new FileNotFoundException("no seed file for '" + group + "'");
        }

        long chosen = 0L;
        long seen = 0L;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                long value;
                try {
                    value = Long.parseLong(line);
                } catch (NumberFormatException ignored) {
                    continue; // skip a malformed line rather than failing the reset
                }
                seen++;
                if (rnd.nextLong(seen) == 0L) { // keep current with probability 1/seen
                    chosen = value;
                }
            }
        }

        if (seen == 0L) {
            throw new IOException("seed file for '" + group + "' is empty");
        }
        return chosen;
    }
}
