package forceitembattle.settings;

import java.util.HashMap;
import java.util.Map;

/**
 * The second adapter: a {@link ConfigSource} backed by a map.
 *
 * <p>Its whole job is to make paths visible. The rule {@link Ruleset} encodes is <em>which path a
 * setting resolves to</em>, and that was previously only observable by watching which value came
 * back out of a real {@code config.yml} — so the two places it went wrong (a write that missed the
 * read's path, a preset that outlived its round) were both invisible.
 */
final class MapConfigSource implements ConfigSource {

    private final Map<String, Object> values = new HashMap<>();
    private int saves;

    /** Seeds a path. Named apart from {@link #set} so the builder does not collide with it. */
    MapConfigSource with(String path, Object value) {
        this.values.put(path, value);
        return this;
    }

    @Override
    public boolean getBoolean(String path) {
        return this.values.get(path) instanceof Boolean b && b;
    }

    @Override
    public int getInt(String path) {
        return this.values.get(path) instanceof Integer i ? i : 0;
    }

    @Override
    public void set(String path, Object value) {
        this.values.put(path, value);
    }

    @Override
    public void save() {
        this.saves++;
    }

    /** Every path that has been written, so a test can assert where a write landed. */
    Map<String, Object> contents() {
        return Map.copyOf(this.values);
    }

    int saveCount() {
        return this.saves;
    }
}
