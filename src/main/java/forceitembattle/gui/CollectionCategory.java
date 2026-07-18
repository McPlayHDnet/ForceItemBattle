package forceitembattle.gui;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.MaterialCategory;
import java.util.function.Predicate;
import org.bukkit.Material;

/**
 * Buckets for the collection book. Reuses {@link MaterialCategory} for the achievement-shared
 * predicates (tools/armor/wood/stone/food) and adds book-only predicates for the rest.
 * {@link #OTHER} is the fallback and MUST stay last.
 *
 * ORDER MATTERS: {@link #categoryOf(Material)} returns the first match in declaration order, so
 * specific categories precede general ones. Notable interactions:
 *   - CANDLES_AND_HONEY before FOOD (honey bottle is edible).
 *   - TRANSPORT / FLOWERS_AND_PLANTS / NETHER_BLOCKS before WOOD (wooden boats, saplings/leaves and
 *     the crimson/warped fungus+roots, and nylium would otherwise match isWoodType).
 *   - FOOD before FLOWERS_AND_PLANTS (edible crops/berries).
 *   - ORES / COPPER / NETHER before STONE (deepslate ores, blackstone/basalt).
 *   - MOB_DROPS before UTILITY (string, bones, etc.).
 *   - STONE adds sandstone here; the achievement's isStoneType still excludes it, unchanged.
 *
 * Each category carries a representative {@code icon} (rendered now) and an empty {@code headTexture}
 * placeholder -- fill the texture in to switch that category to a player head.
 */
public enum CollectionCategory {

    TOOLS("Tools", Material.IRON_PICKAXE, "", CollectionCategory::isToolItem),
    ARMOR("Armor", Material.IRON_CHESTPLATE, "", CollectionCategory::isArmorItem),
    MUSIC_DISCS("Music Discs", Material.JUKEBOX, "", material -> material.name().startsWith("MUSIC_DISC")),
    POTTERY_SHERDS("Pottery Sherds", Material.DECORATED_POT, "", material -> material.name().endsWith("_POTTERY_SHERD")),
    TEMPLATES("Templates & Patterns", Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "", material ->
            material.name().endsWith("_SMITHING_TEMPLATE") || material.name().endsWith("_BANNER_PATTERN")),
    TRANSPORT("Transport", Material.MINECART, "", CollectionCategory::isTransport),
    CANDLES_AND_HONEY("Candles & Honey", Material.HONEY_BOTTLE, "", CollectionCategory::isCandleOrHoney),
    FOOD("Food", Material.BREAD, "", CollectionCategory::isFoodItem),
    COPPER("Copper", Material.COPPER_INGOT, "", material ->
            material.name().contains("COPPER") || material.name().contains("LIGHTNING_ROD")),
    ORES_AND_MINERALS("Ores & Minerals", Material.IRON_ORE, "", CollectionCategory::isOreOrMineral),
    NETHER_BLOCKS("Nether Blocks", Material.NETHERRACK, "", CollectionCategory::isNetherBlock),
    END_BLOCKS("End Blocks", Material.END_STONE, "", CollectionCategory::isEndBlock),
    ICE("Ice", Material.PACKED_ICE, "", material -> material.name().endsWith("ICE")),
    FLOWERS_AND_PLANTS("Flowers & Plants", Material.POPPY, "", CollectionCategory::isFlowerOrPlant),
    WOOD("Wood", Material.OAK_LOG, "", CollectionCategory::isWoodItem),
    CORALS("Corals", Material.TUBE_CORAL, "", material -> material.name().contains("CORAL")),
    DYES("Dyes", Material.RED_DYE, "", material -> material.name().endsWith("_DYE")),
    WOOL_AND_FABRIC("Wool & Fabric", Material.WHITE_WOOL, "", CollectionCategory::isWoolOrFabric),
    GLASS("Glass", Material.GLASS, "", material -> material.name().contains("GLASS")),
    CLAY_BLOCKS("Concrete & Terracotta", Material.TERRACOTTA, "", CollectionCategory::isConcreteOrTerracotta),
    REDSTONE("Redstone", Material.REDSTONE, "", CollectionCategory::isRedstone),
    DIRT_AND_SOIL("Dirt & Soil", Material.DIRT, "", CollectionCategory::isDirtOrSoil),
    STONE("Stone & Bricks", Material.STONE, "", CollectionCategory::isStoneItem),
    MOB_DROPS("Mob Drops", Material.BONE, "", CollectionCategory::isMobDrop),
    UTILITY("Utility", Material.CRAFTING_TABLE, "", CollectionCategory::isUtility),
    CUSTOM_ITEMS("Custom Items", Material.NAME_TAG, "", material -> CustomMaterials.byMaterial(material) != null),
    OTHER("Other", Material.CHEST, "", material -> true);

    private final String displayName;
    private final Material icon;
    private final String headTexture;
    private final Predicate<Material> matcher;

    CollectionCategory(String displayName, Material icon, String headTexture, Predicate<Material> matcher) {
        this.displayName = displayName;
        this.icon = icon;
        this.headTexture = headTexture;
        this.matcher = matcher;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Material getIcon() {
        return this.icon;
    }

    public String getHeadTexture() {
        return this.headTexture;
    }

    /** First category (declaration order) whose predicate accepts the material; OTHER catches the rest. */
    public static CollectionCategory categoryOf(Material material) {
        // Custom items win regardless of their base material's category, even though CUSTOM_ITEMS is
        // declared last (so it renders last in the book). Their vanilla material would otherwise be
        // claimed by an earlier category (nether star -> mob drops, torchflower -> flowers, ...).
        if (CustomMaterials.byMaterial(material) != null) {
            return CUSTOM_ITEMS;
        }
        for (CollectionCategory category : values()) {
            if (category.matcher.test(material)) {
                return category;
            }
        }
        return OTHER;
    }

    // --- book-only extensions of the achievement-shared MaterialCategory predicates ---

    private static boolean isToolItem(Material material) {
        return MaterialCategory.isTool(material) || material.name().endsWith("_SPEAR");
    }

    private static boolean isArmorItem(Material material) {
        // Covers wolf/horse/nautilus armor, which MaterialCategory.isArmor (helmet/chestplate/
        // leggings/boots only) does not.
        return MaterialCategory.isArmor(material) || material.name().endsWith("_ARMOR");
    }

    private static boolean isFoodItem(Material material) {
        return switch (material.name()) {
            case "POPPED_CHORUS_FRUIT", "CAKE", "SUGAR" -> true;
            default -> MaterialCategory.isFood(material);
        };
    }

    private static boolean isWoodItem(Material material) {
        String name = material.name();
        // STICK and STRIPPED_BAMBOO_BLOCK are excluded by isWoodType; add them for the book.
        return MaterialCategory.isWoodType(material) || name.equals("STICK") || name.endsWith("BAMBOO_BLOCK");
    }

    private static boolean isStoneItem(Material material) {
        String name = material.name();
        if (name.equals("PRISMARINE_SHARD") || name.equals("PRISMARINE_CRYSTALS")) {
            return false; // guardian drops -> Mob Drops, not stone blocks
        }
        // isStoneType stays as-is for the achievement (excludes sandstone). The book adds sandstone
        // and all brick blocks (clay bricks, mud bricks, etc.). Nether/End bricks are claimed earlier.
        return MaterialCategory.isStoneType(material)
                || name.contains("SANDSTONE") || name.contains("BRICK") || name.contains("PRISMARINE")
                || name.contains("SULFUR") || name.contains("CINNABAR") || name.equals("RESIN_BLOCK");
    }

    // --- book-only predicates (achievement-shared ones live in MaterialCategory) ---

    private static boolean isTransport(Material material) {
        String name = material.name();
        if (name.endsWith("_BOAT") || name.endsWith("_RAFT") || name.contains("MINECART")
                || name.equals("RAIL") || name.endsWith("_RAIL")) {
            return true;
        }
        return switch (name) {
            case "SADDLE", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK" -> true;
            default -> false;
        };
    }

    private static boolean isCandleOrHoney(Material material) {
        String name = material.name();
        return name.contains("CANDLE") || name.contains("HONEY")
                || name.equals("HONEYCOMB") || name.equals("HONEYCOMB_BLOCK")
                || name.equals("BEEHIVE") || name.equals("BEE_NEST");
    }

    // Ores, raw drops, ingots/nuggets/gems, and mineral blocks. Copper is claimed earlier.
    private static boolean isOreOrMineral(Material material) {
        String name = material.name();
        if (name.contains("SULFUR") || name.contains("CINNABAR")) {
            return false; // routed to Stone & Bricks below
        }
        if (name.endsWith("_ORE") || name.startsWith("RAW_")) {
            return true;
        }
        return switch (name) {
            case "ANCIENT_DEBRIS", "NETHERITE_SCRAP", "NETHERITE_INGOT",
                 "IRON_INGOT", "GOLD_INGOT", "IRON_NUGGET", "GOLD_NUGGET",
                 "DIAMOND", "EMERALD", "LAPIS_LAZULI", "QUARTZ", "AMETHYST_SHARD", "COAL", "CHARCOAL",
                 "IRON_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK", "EMERALD_BLOCK", "NETHERITE_BLOCK",
                 "LAPIS_BLOCK", "COAL_BLOCK", "AMETHYST_BLOCK", "RAW_IRON_BLOCK", "RAW_GOLD_BLOCK" -> true;
            default -> false;
        };
    }

    // Specific on purpose -- must NOT grab NETHER_WART/NETHER_SPROUTS (plants), crimson/warped wood,
    // or MAGMA_CREAM (a mob drop). QUARTZ blocks land here; raw QUARTZ / NETHER_QUARTZ_ORE earlier.
    private static boolean isNetherBlock(Material material) {
        String name = material.name();
        if (name.contains("NETHERRACK") || name.contains("NETHER_BRICK") || name.contains("BLACKSTONE")
                || name.contains("BASALT") || name.contains("QUARTZ") || name.contains("NYLIUM")
                || name.contains("SHROOMLIGHT") || name.contains("GLOWSTONE")) {
            return true;
        }
        return switch (name) {
            case "MAGMA_BLOCK", "NETHER_WART_BLOCK", "WARPED_WART_BLOCK", "CRYING_OBSIDIAN" -> true;
            default -> false;
        };
    }

    private static boolean isEndBlock(Material material) {
        String name = material.name();
        return name.contains("END_STONE") || name.contains("PURPUR") || name.equals("END_ROD")
                || name.equals("DRAGON_EGG") || name.contains("SHULKER");
    }

    private static boolean isFlowerOrPlant(Material material) {
        String name = material.name();
        if (name.equals("FLOWER_POT")) {
            return false; // an empty pot, not a plant -> falls through to Utility
        }
        if (name.endsWith("_SAPLING") || name.endsWith("_LEAVES") || name.endsWith("_PROPAGULE")
                || name.endsWith("_TULIP") || name.endsWith("_SEEDS") || name.contains("MUSHROOM")
                || name.contains("FLOWER") || name.contains("AZALEA")) {
            return true;
        }
        return switch (name) {
            case "DANDELION", "GOLDEN_DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET",
                 "OXEYE_DAISY", "CORNFLOWER", "LILY_OF_THE_VALLEY", "WITHER_ROSE", "LILAC", "ROSE_BUSH",
                 "PEONY", "PITCHER_PLANT", "PITCHER_POD", "SPORE_BLOSSOM", "FERN", "LARGE_FERN",
                 "SHORT_GRASS", "TALL_GRASS", "BUSH", "DEAD_BUSH", "FIREFLY_BUSH", "SEAGRASS", "KELP",
                 "VINE", "GLOW_LICHEN", "HANGING_ROOTS", "LEAF_LITTER", "BIG_DRIPLEAF", "SMALL_DRIPLEAF",
                 "LILY_PAD", "SUGAR_CANE", "BAMBOO", "CACTUS", "MOSS_BLOCK", "MOSS_CARPET", "COCOA_BEANS",
                 "SEA_PICKLE", "WHEAT", "SHORT_DRY_GRASS", "TALL_DRY_GRASS", "DRIED_KELP_BLOCK",
                 "PALE_MOSS_BLOCK", "PALE_MOSS_CARPET", "PALE_HANGING_MOSS", "PINK_PETALS",
                 "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM", "MELON", "PUMPKIN", "CARVED_PUMPKIN",
                 "NETHER_WART", "NETHER_SPROUTS", "WEEPING_VINES", "TWISTING_VINES",
                 "CRIMSON_FUNGUS", "WARPED_FUNGUS", "CRIMSON_ROOTS", "WARPED_ROOTS",
                 "CHORUS_PLANT", "CHORUS_FLOWER" -> true;
            default -> false;
        };
    }

    private static boolean isWoolOrFabric(Material material) {
        String name = material.name();
        return name.equals("WOOL") || name.endsWith("_WOOL")
                || name.equals("CARPET") || name.endsWith("_CARPET")
                || name.endsWith("_BED")
                || name.endsWith("_BANNER")
                || name.endsWith("_HARNESS");
    }

    private static boolean isConcreteOrTerracotta(Material material) {
        String name = material.name();
        return name.contains("CONCRETE") || name.contains("TERRACOTTA");
    }

    // Dust, torch, block, lamp, and common components. Redstone ORE is claimed earlier by Ores.
    private static boolean isRedstone(Material material) {
        String name = material.name();
        if (name.contains("REDSTONE") || name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE")) {
            return true;
        }
        return switch (name) {
            case "REPEATER", "COMPARATOR", "OBSERVER", "PISTON", "STICKY_PISTON",
                 "DISPENSER", "DROPPER", "HOPPER", "LEVER", "TRIPWIRE_HOOK", "TARGET",
                 "DAYLIGHT_DETECTOR", "NOTE_BLOCK", "TNT", "CRAFTER", "LECTERN" -> true;
            default -> false;
        };
    }

    private static boolean isDirtOrSoil(Material material) {
        return switch (material.name()) {
            case "DIRT", "COARSE_DIRT", "ROOTED_DIRT", "GRASS_BLOCK", "PODZOL", "MYCELIUM",
                 "DIRT_PATH", "FARMLAND", "MUD", "MUDDY_MANGROVE_ROOTS", "PACKED_MUD", "CLAY",
                 "SAND", "RED_SAND", "GRAVEL", "SUSPICIOUS_SAND", "SUSPICIOUS_GRAVEL",
                 "SOUL_SAND", "SOUL_SOIL", "SNOW", "SNOW_BLOCK", "POWDER_SNOW" -> true;
            default -> false;
        };
    }

    // Generalized mob-drop items. HONEYCOMB (Candles), SHULKER_SHELL (End) and edible meats (Food)
    // are claimed by earlier categories on purpose.
    private static boolean isMobDrop(Material material) {
        String name = material.name();
        if (name.endsWith("_SKULL") || name.endsWith("_HEAD")) {
            return true;
        }
        return switch (name) {
            case "LEATHER", "RABBIT_HIDE", "FEATHER", "EGG", "STRING", "FERMENTED_SPIDER_EYE",
                 "BONE", "BONE_MEAL", "ROTTEN_FLESH", "GUNPOWDER", "ENDER_PEARL", "ENDER_EYE",
                 "BLAZE_ROD", "BLAZE_POWDER", "SLIME_BALL", "MAGMA_CREAM", "GHAST_TEAR",
                 "PHANTOM_MEMBRANE", "RABBIT_FOOT", "INK_SAC", "GLOW_INK_SAC", "PRISMARINE_SHARD",
                 "PRISMARINE_CRYSTALS", "NAUTILUS_SHELL", "HEART_OF_THE_SEA", "NETHER_STAR",
                 "DRAGON_BREATH", "TURTLE_SCUTE", "ARMADILLO_SCUTE", "BREEZE_ROD", "GOAT_HORN",
                 "BLUE_EGG", "BROWN_EGG" -> true;
            default -> false;
        };
    }

    // Late catch-all for functional items. Placed just before OTHER, so specific categories win.
    private static boolean isUtility(Material material) {
        String name = material.name();
        if (name.endsWith("_BUCKET") || name.endsWith("_TORCH") || name.endsWith("_LANTERN")
                || name.endsWith("_CAMPFIRE") || name.endsWith("_BUNDLE")) {
            return true;
        }
        return switch (name) {
            case "TORCH", "LANTERN", "CAMPFIRE", "BUCKET", "BUNDLE", "BOWL", "JUKEBOX",
                 "CHEST", "TRAPPED_CHEST", "ENDER_CHEST", "BARREL", "COMPOSTER",
                 "CRAFTING_TABLE", "FURNACE", "BLAST_FURNACE", "SMOKER", "BREWING_STAND", "CAULDRON",
                 "ENCHANTING_TABLE", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "GRINDSTONE",
                 "SMITHING_TABLE", "CARTOGRAPHY_TABLE", "FLETCHING_TABLE", "LOOM", "STONECUTTER",
                 "BELL", "BEACON", "CONDUIT", "LODESTONE", "RESPAWN_ANCHOR", "END_CRYSTAL",
                 "BOOKSHELF", "CHISELED_BOOKSHELF", "LADDER", "SCAFFOLDING",
                 "IRON_DOOR", "IRON_TRAPDOOR", "IRON_BARS", "IRON_CHAIN",
                 "FLINT_AND_STEEL", "SHEARS", "FISHING_ROD", "COMPASS", "RECOVERY_COMPASS", "CLOCK",
                 "MAP", "SPYGLASS", "NAME_TAG", "LEAD", "SHIELD", "ELYTRA", "TOTEM_OF_UNDYING",
                 "BOOK", "WRITABLE_BOOK", "ENCHANTED_BOOK", "PAPER", "BRUSH",
                 "ITEM_FRAME", "GLOW_ITEM_FRAME", "PAINTING", "ARMOR_STAND", "FLOWER_POT",
                 "BOW", "CROSSBOW", "ARROW", "TRIDENT", "FIREWORK_ROCKET" -> true;
            default -> false;
        };
    }
}
