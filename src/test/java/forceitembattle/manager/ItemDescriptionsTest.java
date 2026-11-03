package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.model.RoundClock;
import forceitembattle.settings.GameSettings;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemDescriptionsTest {

    private JavaPlugin plugin;
    private YamlConfiguration config;

    @BeforeEach
    void setUp() {
        this.plugin = mock(JavaPlugin.class);
        this.config = new YamlConfiguration();
        when(this.plugin.getConfig()).thenReturn(this.config);
        when(this.plugin.getResource("descriptions.yml")).thenAnswer(invocation ->
                getClass().getClassLoader().getResourceAsStream("descriptions.yml"));
    }

    private ItemDifficultiesManager enabledManager() {
        ItemDifficultiesManager items =
                new ItemDifficultiesManager(this.plugin, new RoundClock(), mock(GameSettings.class));
        items.enable();
        return items;
    }

    /** enable() throws on a key that is no longer a Material, which would stop the plugin booting. */
    @Test
    void bundledDescriptionsLoadWithoutAConfig() {
        ItemDifficultiesManager items = enabledManager();

        assertTrue(items.isItemInDescriptionList(Material.ANGLER_POTTERY_SHERD));
        assertFalse(items.getDescriptionItemLines(Material.ANGLER_POTTERY_SHERD).isEmpty());
    }

    @Test
    void theConfigOverridesABundledDescription() {
        this.config.set("descriptions.ANGLER_POTTERY_SHERD", List.of("from the config"));

        ItemDifficultiesManager items = enabledManager();

        assertEquals(List.of("from the config"), items.getDescriptionItemLines(Material.ANGLER_POTTERY_SHERD));
    }
}
