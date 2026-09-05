package forceitembattle.settings;

/**
 * Where a {@link Ruleset} reads and writes settings.
 *
 * <p>Four operations, which is everything the rules need — the rest of the plugin's configuration
 * (defaults, sections, preset loading) is Bukkit's business and stays behind {@link GameSettings}.
 *
 * <p>Two adapters justify the seam: {@code BukkitConfigSource} over the plugin's {@code config.yml}
 * in production, and a map in tests. Before this existed, asking "which value does this setting
 * have?" meant a {@code ForceItemBattle} with a real {@code FileConfiguration} behind it.
 */
public interface ConfigSource {

    boolean getBoolean(String path);

    int getInt(String path);

    void set(String path, Object value);

    /** Flushes to disk. A no-op for sources that have no disk. */
    void save();
}
