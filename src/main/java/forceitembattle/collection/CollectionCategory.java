package forceitembattle.collection;

import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.MaterialCategory;
import java.util.function.Predicate;
import org.bukkit.Material;

/**
 * Buckets for the collection book. Reuses {@link MaterialCategory} for the achievement-shared
 * predicates (tools/armor/wood/stone/food) and adds book-only predicates for the rest.
 * {@link #OTHER} is the fallback and MUST stay last.
 */
public enum CollectionCategory {

    TOOLS("Tools", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODM2MjQzOGZmNGVjZjhmNGEyY2FhMTI3NzU2MWM5NTEzYzlhOTg2ZGJlMzhhODBiOWJhZmNiZmVkOGIyYTljOCJ9fX0=", CollectionCategory::isToolItem),
    ARMOR("Armor", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjNlMThhNWI3ZDA0YzYxNjkyYTBkMWUxNDdhOTJlYmM2YTVhYjNlZmIzMjQ1ZTFlMzRkZDU1ZTJiOTFmODA2In19fQ==", CollectionCategory::isArmorItem),
    MUSIC_DISCS("Music Discs", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlYjVkMjJhZWM5NjI5ODQ1Njg1YWY3MTJjYmRmYjg2Zjc1NjVkYjQ3NDk5MzViNTc3ZjliZWIwOTI4MDBhMCJ9fX0=", material -> material.name().startsWith("MUSIC_DISC")),
    POTTERY_SHERDS("Pottery Sherds", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNlNDFjZWY1ZGViYjIyY2M0NWQ4N2Y0MDg1NWQ4OWM0NDJjZWJiYjk4MjBjNDdjZDRkMWYzODlmZGUzNmNmOSJ9fX0=", material -> material.name().endsWith("_POTTERY_SHERD")),
    TEMPLATES("Templates & Patterns", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc3ZTQwYmIwYjM3MWIzNzQ1ZGZlNWVkNTIwMzNmZmNmYzhjODJmZWVmYzI1YjM0YmFjN2FlZDFmZjljZTU4ZiJ9fX0=", material ->
            material.name().endsWith("_SMITHING_TEMPLATE") || material.name().endsWith("_BANNER_PATTERN")),
    CANDLES_AND_HONEY("Candles & Honey", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFhMWVkNzBiM2Y5NTJiYzI4OWQ0NzJhNmNhNjcyZDA0NmMxMWVmNDE4MjUwOTgzYzg0ODcyNjE4YjkwNGJhZCJ9fX0=", CollectionCategory::isCandleOrHoney),
    FOOD("Food", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTZlZjFjMjVmNTE2ZjJlN2Q2Zjc2Njc0MjBlMzNhZGNmM2NkZjkzOGNiMzdmOWE0MWE4YjM1ODY5ZjU2OWIifX19", CollectionCategory::isFoodItem),
    COPPER("Copper", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmQ1NzI0YTc5ZjkwZmQxZTU0NzY1ZDc1OTY5MjgxNDZkY2I5ZTUwZmRiODg1OTYyMDFjNGEyMzVjNGE2ZjRlZSJ9fX0=", material ->
            material.name().contains("COPPER") || material.name().contains("LIGHTNING_ROD")),
    ORES_AND_MINERALS("Ores & Minerals", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UwYWEzYTk1MjVkODY0NmYwNmIxMmE1NGExOTc3MGVhZjMyMDA1N2M5OGViZjYzZTY2M2ZkZTJkOWQ5YjEzMSJ9fX0=", CollectionCategory::isOreOrMineral),
    NETHER_BLOCKS("Nether Blocks", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTliOTRlNWFkOTNkYzdhZGY1OTAwNTZkNGExZTAzNDA5MjUzZGZlY2ZjODhlODMxNTQxYzhkZjU0ZmYwNWNhNiJ9fX0=", CollectionCategory::isNetherBlock),
    END_BLOCKS("End Blocks", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTlmMjFmNWQ4ODMzMTZmZDY1YTkzNjZmMzJhMzMwMTMxODJlMzM4MWRlYzIxYzE3Yzc4MzU1ZDliZjRmMCJ9fX0=", CollectionCategory::isEndBlock),
    FLOWERS_AND_PLANTS("Flowers & Plants", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmZTBmMmU2YzBmZmVlZmJiODRjMzJlNzE4NzZiNjhkY2JmN2FjOWU4NDIwYTNkMWJmNTkzYWEyMWE4Mzc0YSJ9fX0=", CollectionCategory::isFlowerOrPlant),
    WOOD("Wood", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2YxMzQ2MDkyYzgwZDNkYjIxN2VmZTRjOTM2OTY5MWU2MWM4YWZjMWIyODc0MWZhNTA0ODJjOTJjOWZkM2QxOCJ9fX0=", CollectionCategory::isWoodItem),
    CORALS("Corals", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZlOGRlZDNjNzRlYWNkNzg0MTJhOTAzYjkwNGY1NTc3ODUwZDFlMjBkMzQ4NzhmZDc3NTk3YWQxNjMzYmY3NCJ9fX0=", material -> material.name().contains("CORAL")),
    DYES("Dyes", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdjOTAzY2ZjM2NlOGJkYTU5YzgzZTViYjc4NTU1MjJkYzE3ZmViMmI4MTI4ZDA3NDc4NmYyMDBkNzMwYzdiNCJ9fX0=", material -> material.name().endsWith("_DYE")),
    WOOL_AND_FABRIC("Wool & Fabric", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzE4Njk5YmFlOWYxZTAyYTdhNjUxODNiZDAwNzk0ODllNGU2MTVkYWRkY2UzYzkyNjQ5Y2RlZDMxMmIyMjZkYyJ9fX0=", CollectionCategory::isWoolOrFabric),
    GLASS("Glass", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTE1MjBlYTQzODRlOGM0ODI1ODY1ZWU3ZGNmMTA5MmFmNDQzZjE1MGFhYjE3MDQxODA4YzlkMzFjZDAxZmRmNyJ9fX0=", CollectionCategory::isGlass),
    CLAY_BLOCKS("Concrete & Terracotta", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UwYjk3Yzc4NWUzOTc2ZWZhNzhhY2M4MjhkYjY3ZTcxZGVhZDliNjY5NGZkZTYxM2QyZTJlN2NhZmJlMWQ1YiJ9fX0=", CollectionCategory::isConcreteOrTerracotta),
    REDSTONE("Redstone", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQyMGExMjVjMmNjMWE2NDQzNjNhYzNlMDg1MDRkNjYwNTRiMmE3ZTRlMDVlYTU2MGYyNDNkZjVmNzdiYWY0MyJ9fX0=", CollectionCategory::isRedstone),
    DIRT_AND_SOIL("Dirt & Soil", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFiNDNiOGMzZDM0ZjEyNWU1YTNmOGI5MmNkNDNkZmQxNGM2MjQwMmMzMzI5ODQ2MWQ0ZDRkN2NlMmQzYWVhIn19fQ==", CollectionCategory::isDirtOrSoil),
    STONE("Stone & Bricks", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNmYTBmYzA5OTZjZjc3MmQxZGJjMDUyYWEyNWIxMWRhYmFlOTc3ODIwYWY2NjNlZjAyMmQzY2UxZGI2MTEyMiJ9fX0=", CollectionCategory::isStoneItem),
    MOB_DROPS("Mob Drops", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIwMjUzZWNkYTc5OWFkN2Y0YzQzMjM1MmM3MGNjNzNkYjUxODBjNTgwNjIyMTRhMmY1ZjllODZiMjQ2NTEzZiJ9fX0=", CollectionCategory::isMobDrop),
    UTILITY("Utility", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTgyNzBjOGFjMjVhMDdhZTJhNzFkYmQwNjFiNmRlZTEwNTZiZjYyNWY4Yjg4MWExZjJlZjQ2NGY3MDUzYWNhOCJ9fX0=", CollectionCategory::isUtility),
    CUSTOM_ITEMS("Custom Items", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTcwMjE2YmFmMWI5Njc1ZjgwNWRmZGY5NWRiMDQzYWZlNmY4ODFjODJiMjU5MzdlNDZiMTUwNjhlOGYzZTg4MiJ9fX0=", material -> CustomMaterials.byMaterial(material) != null),
    OTHER("Other", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWMwYjFjZmNhMmM2ZmJhZmI1NjNlZTdlYWI4NTVlNDhlNzNlZjk0MTU1ZTllMDczZmYzZTBhOTQ4NDBjMGYwNSJ9fX0=", material -> true);

    private final String displayName;
    private final String headTexture;
    private final Predicate<Material> matcher;

    CollectionCategory(String displayName, String headTexture, Predicate<Material> matcher) {
        this.displayName = displayName;
        this.headTexture = headTexture;
        this.matcher = matcher;
    }

    /** First category in declaration order whose predicate accepts the material. */
    public static CollectionCategory categoryOf(Material material) {
        // Custom items win despite CUSTOM_ITEMS being declared last (so it renders last in the book).
        // Their vanilla material would otherwise be claimed earlier: nether star -> mob drops, etc.
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

    private static boolean isToolItem(Material material) {
        return MaterialCategory.isTool(material) || material.name().endsWith("_SPEAR");
    }

    private static boolean isArmorItem(Material material) {
        // The suffix covers wolf/horse/nautilus armour, which MaterialCategory.isArmor does not.
        return MaterialCategory.isArmor(material) || material.name().endsWith("_ARMOR");
    }

    private static boolean isFoodItem(Material material) {
        return switch (material.name()) {
            // Not edible, so MaterialCategory.isFood misses these.
            case "POPPED_CHORUS_FRUIT", "CAKE", "SUGAR", "GLISTERING_MELON_SLICE" -> true;
            default -> MaterialCategory.isFood(material);
        };
    }

    private static boolean isGlass(Material material) {
        String name = material.name();
        return !name.equals("SPYGLASS") && name.contains("GLASS");
    }

    private static boolean isWoodItem(Material material) {
        String name = material.name();
        if (name.endsWith("_BOAT") || name.endsWith("_RAFT")) {
            return true;
        }
        // STICK and STRIPPED_BAMBOO_BLOCK are excluded by isWoodType; add them for the book.
        return MaterialCategory.isWoodType(material) || name.equals("STICK") || name.endsWith("BAMBOO_BLOCK");
    }

    private static boolean isStoneItem(Material material) {
        String name = material.name();
        if (name.equals("PRISMARINE_SHARD") || name.equals("PRISMARINE_CRYSTALS")) {
            return false; // guardian drops -> Mob Drops, not stone blocks
        }
        // isStoneType excludes sandstone for the achievement; the book adds it and all brick blocks.
        // Nether/End bricks are claimed earlier.
        return MaterialCategory.isStoneType(material)
                || name.contains("SANDSTONE") || name.contains("BRICK") || name.contains("PRISMARINE")
                || name.contains("SULFUR") || name.contains("CINNABAR") || name.equals("RESIN_BLOCK");
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
        if (name.equals("RAIL") || name.endsWith("_RAIL") || name.contains("MINECART")) {
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
        String name = material.name();
        if (name.equals("ICE") || name.endsWith("_ICE")) {
            return true;
        }
        return switch (name) {
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
                 "BOW", "CROSSBOW", "ARROW", "TRIDENT", "FIREWORK_ROCKET",
                 "SADDLE", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK" -> true;
            default -> false;
        };
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getHeadTexture() {
        return this.headTexture;
    }

    public ItemBuilder head() {
        return new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture(this.headTexture);
    }
}
