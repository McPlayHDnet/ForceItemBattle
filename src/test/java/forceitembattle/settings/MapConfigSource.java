package forceitembattle.settings;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ConfigSource} backed by a map, so paths are visible. The rule {@link Ruleset} encodes is
 * <em>which path a setting resolves to</em>, which a real {@code config.yml} only shows indirectly.
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
