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
 * <p>This is the same shape of hazard as the wood-category bug found earlier in this pass — a
 * matcher table where order decides the answer and the ordering is a comment.
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

    /** The MUST from the class javadoc, made a failure instead of a hope. */
    @Test
    void otherIsLast() {
        CollectionCategory[] values = CollectionCategory.values();

        assertSame(CollectionCategory.OTHER, values[values.length - 1],
                "OTHER is the fallback; anything declared after it can never be reached");
    }

    /** The second ordering rule, from the comment on {@code isUtility}. */
    @Test
    void theUtilityCatchAllSitsJustBeforeOther() {
        CollectionCategory[] values = CollectionCategory.values();

        assertSame(CollectionCategory.UTILITY, values[values.length - 3],
                "UTILITY is a late catch-all and must not shadow the specific categories");
        assertSame(CollectionCategory.CUSTOM_ITEMS, values[values.length - 2]);
    }

    /**
     * Total: every material lands somewhere, so nothing can fall out of the book.
     *
     * <p>Legacy materials are skipped. They are pre-1.13 compatibility aliases, they need a live
     * server to resolve at all ({@code Bukkit.getUnsafe()}), and the collection catalogue is built
     * from the register table, which contains none of them.
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
     * No category is unreachable.
     *
     * <p>The failure this catches: a category whose materials are all claimed by an earlier
     * predicate can never be returned, so it renders as an empty page in the collection book while
     * looking perfectly correct in the source. Only walking the whole material registry finds it.
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
     * A custom item beats whatever its base material would otherwise be sorted as — checked before
     * the loop precisely because {@code CUSTOM_ITEMS} is declared late so it renders last in the
     * book. Nether Star would be a mob drop; Torchflower would be a flower.
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
     * The exception, and it is deliberate: two custom items share their material with a real force
     * item. A brush and a totem are LATE/EXTREME pool items in their own right, so the Kiln-Fired
     * Brush and the Totem of Antimatter ride alongside them rather than claiming the material —
     * {@code CustomMaterials.BY_MATERIAL} excludes them for exactly this reason.
     *
     * <p>So the base material keeps its ordinary category, and the collection book files a brush
     * under tools rather than swallowing it into the custom page. Asserting the opposite is what
     * this test was written as first, and the code was right.
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

    /**
     * A bucket is utility, not "the liquid it holds" — the case the late catch-all exists for, and
     * the one most likely to be captured by an earlier predicate if a category is added carelessly.
     */
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
