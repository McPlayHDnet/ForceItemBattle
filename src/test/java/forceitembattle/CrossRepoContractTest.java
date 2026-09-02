package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.RoundClock;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.QuickieMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The contracts other repositories parse out of this one, enforced here rather than reviewed.
 *
 * <p>It asserts on <em>source text</em> because here the source text is the interface:
 * {@code website/scripts/vendor-pool.mjs} fetches two files of this plugin from GitHub {@code main}
 * and parses them with regular expressions. There is no endpoint, artifact or schema between the two
 * repositories, so renaming a file or reformatting a line is a breaking change to a consumer that
 * cannot see this repository's compiler — and the consumer only refuses outright when it parses
 * <em>zero</em> entries, so breaking <em>some</em> quietly drops items from the site.
 *
 * <p>The patterns below are copied verbatim from the consumer. When the consumer changes, change
 * them here in the same commit.
 */
class CrossRepoContractTest {

    /** The exact paths {@code vendor-pool.mjs} builds its raw.githubusercontent URLs from. */
    private static final Path ITEM_MANAGER =
            Path.of("src/main/java/forceitembattle/manager/ItemDifficultiesManager.java");
    private static final Path CUSTOM_MATERIALS =
            Path.of("src/main/java/forceitembattle/model/CustomMaterials.java");

    // --- copied verbatim from website/scripts/vendor-pool.mjs --------------------------------

    private static final Pattern REGISTER =
            Pattern.compile("register\\(Material\\.(\\w+),\\s*State\\.(\\w+)((?:,\\s*ItemTag\\.\\w+)*)\\)");

    private static final Pattern CUSTOM = Pattern.compile(
            "^ {4}([A-Z][A-Z0-9_]*)\\(Material\\.(\\w+),\\s*\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
            Pattern.MULTILINE);

    private static final Pattern SHARES_MATERIAL = Pattern.compile(
            "SHARES_MATERIAL_WITH_POOL_ITEM\\s*=\\s*\\n?\\s*Set\\.of\\(([^)]*)\\)");

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path),
                path + " is fetched by name from GitHub main by website/scripts/vendor-pool.mjs. "
                        + "Moving or renaming it breaks the site's item index silently.");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void theItemPoolSourceIsWhereTheWebsiteLooksForIt() throws IOException {
        assertFalse(read(ITEM_MANAGER).isBlank());
    }

    @Test
    void theRegisterCallsStillParse() throws IOException {
        int matches = 0;
        Matcher matcher = REGISTER.matcher(read(ITEM_MANAGER));
        while (matcher.find()) {
            matches++;
        }

        assertTrue(matches > 1_000,
                "the website builds its whole item index from these; found only " + matches);
    }

    /**
     * The check that matters: <b>what the game registers must equal what the website can parse</b>.
     * Comparing against the <em>runtime</em> registry, not against a second regex — a registration
     * made through any shape the pattern cannot see (a constant instead of a literal, a loop, a
     * helper method) registers in game and is silently absent from the site.
     */
    @Test
    void thePoolTheGameRegistersIsThePoolTheWebsiteCanSee() throws IOException {
        int parsed = 0;
        Matcher matcher = REGISTER.matcher(read(ITEM_MANAGER));
        while (matcher.find()) {
            parsed++;
        }

        assertEquals(registeredAtRuntime(), parsed,
                "the game registers %d items but vendor-pool.mjs can parse %d; the difference "
                        .formatted(registeredAtRuntime(), parsed)
                        + "vanishes from the site's item index without any error");
    }

    private int registeredAtRuntime() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        GameSettings settings = mock(GameSettings.class);

        when(settings.getQuickieMode()).thenReturn(QuickieMode.DISABLED);

        ItemDifficultiesManager items =
                new ItemDifficultiesManager(plugin, new RoundClock(), settings);
        items.enable();
        return items.getAllItems().size();
    }

    @Test
    void theCustomItemSourceIsWhereTheWebsiteLooksForIt() throws IOException {
        assertFalse(read(CUSTOM_MATERIALS).isBlank());
    }

    /**
     * Every enum constant must be parseable, and the consumer's pattern is anchored to
     * <b>exactly four spaces</b> of indentation. Reformatting this file — or letting an IDE do it —
     * drops whichever constants moved.
     */
    @Test
    void everyCustomItemIsInTheShapeTheWebsiteCanRead() throws IOException {
        Set<String> parsed = new LinkedHashSet<>();
        Matcher matcher = CUSTOM.matcher(read(CUSTOM_MATERIALS));
        while (matcher.find()) {
            parsed.add(matcher.group(1));
        }

        Set<String> declared = new LinkedHashSet<>();
        for (CustomMaterials custom : CustomMaterials.values()) {
            declared.add(custom.name());
        }

        assertEquals(declared, parsed,
                "these custom items are declared but not parseable by vendor-pool.mjs, so the site "
                        + "would show their vanilla names instead");
    }

    /**
     * The website reads {@code SHARES_MATERIAL_WITH_POOL_ITEM} so it does <em>not</em> rename a brush
     * or a totem to its custom name. Pinned because the pattern is sensitive to the {@code Set.of(}
     * spelling and to the line break before it.
     */
    @Test
    void theSharedMaterialSetStillParses() throws IOException {
        Matcher matcher = SHARES_MATERIAL.matcher(read(CUSTOM_MATERIALS));

        assertTrue(matcher.find(), "vendor-pool.mjs refuses to build when this parses as empty");

        Set<String> parsed = new LinkedHashSet<>();
        for (String name : matcher.group(1).split(",")) {
            if (!name.isBlank()) {
                parsed.add(name.trim());
            }
        }

        assertEquals(Set.of("KILN_FIRED_BRUSH", "TOTEM_OF_ANTIMATTER"), parsed);
    }

    /**
     * The one contract with no parser behind it. {@code wheel-backend/src/server/services/itemPool.js}
     * keeps a hand-written {@code CUSTOM_OWNED_MATERIALS} set, maintained in another repository and
     * guarded only by a {@code [WARNING]} log line. If this fails, a custom item was added or removed
     * and the wheel needs the same edit — that repository has no way to find out on its own.
     */
    @Test
    void theWheelsExclusionListStillHasTheRightSize() {
        int owned = CustomMaterials.values().length - 2; // minus the two shared with pool items

        assertEquals(6, owned,
                "CUSTOM_OWNED_MATERIALS in wheel-backend/src/server/services/itemPool.js lists 6 "
                        + "materials and is hand-maintained. The plugin now owns " + owned
                        + " outright, so that set needs updating in the same change.");
    }
}
