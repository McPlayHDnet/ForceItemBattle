package forceitembattle.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.CustomMaterials;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * {@link CollectionCategory}, whose correctness is entirely a matter of <em>declaration order</em>.
 *
 * <p>{@code categoryOf} returns the first category whose predicate accepts the material, so an
 * earlier entry shadows a later one. Two ordering rules are written in the source as MUST and were
 * enforced by nothing: {@code OTHER} has to stay last, and the {@code UTILITY} catch-all has to sit
 * just before it. Either one broken puts items in the wrong bucket of the collection book, or
 * removes a whole category from it, and nothing else would say so.
 *
 * <p>Needs a server, which was not obvious: {@code isFoodItem} asks {@code Material.isEdible()},
 * and on Paper 26 that resolves through the item registry. Without one, MockBukkit reports a
 * version mismatch — its guess at why its registry is empty — several frames from the actual cause.
 */
class CollectionCategoryTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void otherIsLast() {
        CollectionCategory[] values = CollectionCategory.values();

        assertSame(CollectionCategory.OTHER, values[values.length - 1],
                "OTHER is the fallback; anything declared after it can never be reached");
    }

    @Test
    void theUtilityCatchAllSitsJustBeforeOther() {
        CollectionCategory[] values = CollectionCategory.values();

        assertSame(CollectionCategory.UTILITY, values[values.length - 3],
                "UTILITY is a late catch-all and must not shadow the specific categories");
        assertSame(CollectionCategory.CUSTOM_ITEMS, values[values.length - 2]);
    }

    /**
     * Every material lands somewhere, so nothing can fall out of the book. Legacy materials are
     * skipped: pre-1.13 aliases that need {@code Bukkit.getUnsafe()} to resolve at all, and the
     * catalogue is built from the register table, which contains none of them.
     */
    @Test
    void everyMaterialGetsACategory() {
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            assertNotNull(CollectionCategory.categoryOf(material), material + " has no category");
        }
    }

    /**
     * A category whose materials are all claimed by an earlier predicate can never be returned, so it
     * renders as an empty page while looking correct in the source.
     */
    @Test
    void everyCategoryIsReachableBySomeMaterial() {
        Map<CollectionCategory, Material> firstMatch = new EnumMap<>(CollectionCategory.class);
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            firstMatch.putIfAbsent(CollectionCategory.categoryOf(material), material);
        }

        Set<CollectionCategory> unreachable = EnumSet.allOf(CollectionCategory.class);
        unreachable.removeAll(firstMatch.keySet());

        assertTrue(unreachable.isEmpty(),
                "these categories are shadowed by an earlier one and would render empty: " + unreachable);
    }

    /**
     * A custom item beats whatever its base material would otherwise sort as — checked before the
     * loop precisely because {@code CUSTOM_ITEMS} is declared late so it renders last in the book.
     */
    @Test
    void aCustomItemBeatsItsBaseMaterialsCategory() {
        for (CustomMaterials custom : CustomMaterials.values()) {
            Material base = custom.getMaterial();
            if (CustomMaterials.byMaterial(base) == null) {
                continue; // shares its material with a pool item -- see the test below
            }
            assertSame(CollectionCategory.CUSTOM_ITEMS, CollectionCategory.categoryOf(base),
                    custom + " should be filed under custom items, not under " + base);
        }
    }

    /**
     * The deliberate exception: two custom items share their material with a real force item, so
     * {@code CustomMaterials.BY_MATERIAL} excludes them and the base material keeps its ordinary
     * category — a brush is filed under tools, not swallowed into the custom page.
     */
    @Test
    void anItemSharingItsMaterialWithAPoolItemKeepsItsOwnCategory() {
        assertSame(null, CustomMaterials.byMaterial(Material.BRUSH),
                "a brush is a force item in its own right");
        assertNotSame(CollectionCategory.CUSTOM_ITEMS,
                CollectionCategory.categoryOf(Material.BRUSH));
        assertNotSame(CollectionCategory.CUSTOM_ITEMS,
                CollectionCategory.categoryOf(Material.TOTEM_OF_UNDYING));
    }

    @Test
    void musicDiscsAreTheirOwnCategory() {
        assertEquals(CollectionCategory.MUSIC_DISCS,
                CollectionCategory.categoryOf(Material.MUSIC_DISC_CAT));
    }

    @Test
    void potterySherdsAreTheirOwnCategory() {
        assertEquals(CollectionCategory.POTTERY_SHERDS,
                CollectionCategory.categoryOf(Material.ANGLER_POTTERY_SHERD));
    }

    @Test
    void dyesAreTheirOwnCategory() {
        assertEquals(CollectionCategory.DYES, CollectionCategory.categoryOf(Material.LIME_DYE));
    }

    /** A bucket is utility, not "the liquid it holds" — the case the late catch-all exists for. */
    @Test
    void aBucketIsUtility() {
        assertEquals(CollectionCategory.UTILITY, CollectionCategory.categoryOf(Material.BUCKET));
    }

    @Test
    void everyCategoryHasADisplayNameAndAHead() {
        for (CollectionCategory category : CollectionCategory.values()) {
            assertNotNull(category.getDisplayName(), category + " needs a display name");
            assertTrue(category.getDisplayName().length() > 0);
            assertNotNull(category.getHeadTexture(), category + " needs a head texture");
        }
    }
}
