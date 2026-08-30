package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/** Material naming — the display name shown in chat and the slug used to build /infowiki links. */
class CustomMaterialsTest {

    @Test
    void vanillaMaterialsGetTheirTitleCasedName() {
        assertEquals("Stone", CustomMaterials.nameOf(Material.STONE));
        assertEquals("Redstone Block", CustomMaterials.nameOf(Material.REDSTONE_BLOCK));
    }

    @Test
    void ourOwnItemsKeepTheirCustomName() {
        assertEquals("Wheel of Fortune", CustomMaterials.nameOf(Material.NETHER_STAR));
        assertEquals("Antimatter Locator", CustomMaterials.nameOf(Material.KNOWLEDGE_BOOK));
    }

    @Test
    void idOfFallsBackToTheLowercasedMaterialName() {
        assertEquals("stone", CustomMaterials.idOf(Material.STONE));
        assertEquals("wheel_of_fortune", CustomMaterials.idOf(Material.NETHER_STAR));
    }

    @Test
    void wikiSlugsUseUnderscoresAndTitleCase() {
        assertEquals("Stone", CustomMaterials.wikiSlugOf(Material.STONE));
        assertEquals("Redstone_Block", CustomMaterials.wikiSlugOf(Material.REDSTONE_BLOCK));
    }

    /** The case the joining-word pass was written for: minecraft.wiki lowercases these words. */
    @Test
    void joiningWordsAreLowercasedInWikiSlugs() {
        assertEquals("Heart_of_the_Sea", CustomMaterials.wikiSlugOf(Material.HEART_OF_THE_SEA));
        assertEquals("Carrot_on_a_Stick", CustomMaterials.wikiSlugOf(Material.CARROT_ON_A_STICK));
        assertEquals("Totem_of_Undying", CustomMaterials.wikiSlugOf(Material.TOTEM_OF_UNDYING));
        assertEquals("Flint_and_Steel", CustomMaterials.wikiSlugOf(Material.FLINT_AND_STEEL));
    }

    /**
     * Regression: a joining word must only be lowered when it is a whole word. These four all came
     * out mangled while the lowering was a substring replace — Stone_axe, Golden_apple,
     * Polished_andesite, Potted_wither_Rose — because "a" matched the A in Axe/Apple/Andesite and
     * "with" matched Wither.
     */
    @Test
    void joiningWordsAreOnlyLoweredWhenTheyAreWholeWords() {
        assertEquals("Stone_Axe", CustomMaterials.wikiSlugOf(Material.STONE_AXE));
        assertEquals("Golden_Apple", CustomMaterials.wikiSlugOf(Material.GOLDEN_APPLE));
        assertEquals("Polished_Andesite", CustomMaterials.wikiSlugOf(Material.POLISHED_ANDESITE));
        assertEquals("Potted_Wither_Rose", CustomMaterials.wikiSlugOf(Material.POTTED_WITHER_ROSE));
        assertEquals("Diamond_Horse_Armor", CustomMaterials.wikiSlugOf(Material.DIAMOND_HORSE_ARMOR));
        assertEquals("Budding_Amethyst", CustomMaterials.wikiSlugOf(Material.BUDDING_AMETHYST));
    }

    /** A joining word can still lead a title, where it must stay capitalised. */
    @Test
    void aLeadingJoiningWordIsNotLowered() {
        assertEquals("Andesite", CustomMaterials.wikiSlugOf(Material.ANDESITE));
        assertEquals("Acacia_Boat", CustomMaterials.wikiSlugOf(Material.ACACIA_BOAT));
    }

    /** No material should come out with a lowercase letter starting any word but a joining one. */
    @Test
    void noMaterialProducesAnUnexpectedlyLowercasedWord() {
        java.util.List<String> bad = java.util.Arrays.stream(Material.values())
                .map(CustomMaterials::wikiSlugOf)
                .filter(slug -> {
                    String[] parts = slug.split("_");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].isEmpty()) {
                            continue;
                        }
                        boolean lower = Character.isLowerCase(parts[i].charAt(0));
                        boolean allowed = i > 0 && java.util.Set.of("and", "with", "of", "on", "a", "the")
                                .contains(parts[i]);
                        if (lower && !allowed) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();

        org.junit.jupiter.api.Assertions.assertTrue(bad.isEmpty(),
                "slugs with an unexpectedly lowercased word: " + bad);
    }
}
