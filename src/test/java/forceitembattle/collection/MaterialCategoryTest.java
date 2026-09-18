package forceitembattle.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Checks {@link MaterialCategory}'s hardcoded name sets against the real registry. The only
 * categorisation test that needs a server, which is why the rest do not.
 */
class MaterialCategoryTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Only over materials this API version has, so a newer food is missed rather than false-failing. */
    @Test
    void theFoodSetStillAgreesWithTheRegistry() {
        Set<String> registry = new TreeSet<>();
        Set<String> ours = new TreeSet<>();

        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            if (material.isEdible()) {
                registry.add(material.name());
            }
            if (MaterialCategory.isFood(material)) {
                ours.add(material.name());
            }
        }

        assertEquals(registry, ours,
                "the hardcoded food set has drifted from Material.isEdible(); update EDIBLE in MaterialCategory");
    }

    /** They carry {@code minecraft:food} but not {@code minecraft:consumable}. */
    @Test
    void aFishBucketIsNotFood() {
        for (Material material : new Material[]{Material.COD_BUCKET, Material.SALMON_BUCKET,
                Material.PUFFERFISH_BUCKET, Material.TROPICAL_FISH_BUCKET}) {
            assertFalse(MaterialCategory.isFood(material), material + " is not edible");
        }
    }

    /** The name and material forms are the same question; nothing may answer them differently. */
    @Test
    void theNameOverloadsAgreeWithTheMaterialOnes() {
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            String name = material.name();
            assertEquals(MaterialCategory.isFood(material), MaterialCategory.isFood(name), name);
            assertEquals(MaterialCategory.isTool(material), MaterialCategory.isTool(name), name);
            assertEquals(MaterialCategory.isArmor(material), MaterialCategory.isArmor(name), name);
            assertEquals(MaterialCategory.isWoodType(material), MaterialCategory.isWoodType(name), name);
            assertEquals(MaterialCategory.isStoneType(material), MaterialCategory.isStoneType(name), name);
            assertEquals(MaterialCategory.getWoodCategory(material),
                    MaterialCategory.getWoodCategory(name), name);
            assertEquals(CollectionCategory.categoryOf(material),
                    CollectionCategory.categoryOf(name), name);
        }
    }

    /** Honey is edible but reads as honey first; the book must not move it into Food. */
    @Test
    void honeyStaysWithTheCandles() {
        assertTrue(MaterialCategory.isFood(Material.HONEY_BOTTLE));
        assertEquals(CollectionCategory.CANDLES_AND_HONEY,
                CollectionCategory.categoryOf(Material.HONEY_BOTTLE));
    }
}
