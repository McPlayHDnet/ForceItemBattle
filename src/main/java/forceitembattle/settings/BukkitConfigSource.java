package forceitembattle.settings;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class BukkitConfigSource implements ConfigSource {

    private final JavaPlugin plugin;

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
