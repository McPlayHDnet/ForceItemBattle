package forceitembattle.settings;

/** Where a {@link Ruleset} reads and writes settings: config.yml in production, a map in tests. */
public interface ConfigSource {

    boolean getBoolean(String path);

    int getInt(String path);

    void set(String path, Object value);

    /** Flushes to disk. A no-op for sources that have no disk. */
    void save();
}
