package forceitembattle.settings;

import forceitembattle.ForceItemBattle;
import lombok.RequiredArgsConstructor;

/**
 * The live {@link ConfigSource}: the plugin's own {@code config.yml}.
 *
 * <p>Deliberately thin. Everything interesting about a setting — which path it lives at, and
 * therefore whether a preset is in force — is {@link Ruleset}'s job, and none of it needs a plugin.
 */
@RequiredArgsConstructor
public final class BukkitConfigSource implements ConfigSource {

    private final ForceItemBattle plugin;

    @Override
    public boolean getBoolean(String path) {
        return this.plugin.getConfig().getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return this.plugin.getConfig().getInt(path);
    }

    @Override
    public void set(String path, Object value) {
        this.plugin.getConfig().set(path, value);
    }

    @Override
    public void save() {
        this.plugin.saveConfig();
    }
}
