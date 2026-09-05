package forceitembattle.achievements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.Nullable;

public enum BiomeGroup {

    OCEAN(Biome.OCEAN, Biome.DEEP_OCEAN),
    WARM_OCEAN(Biome.WARM_OCEAN),
    LUKEWARM_OCEAN(Biome.LUKEWARM_OCEAN, Biome.DEEP_LUKEWARM_OCEAN),
    COLD_OCEAN(Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN),
    FROZEN_OCEAN(Biome.FROZEN_OCEAN, Biome.DEEP_FROZEN_OCEAN),
    MUSHROOM_FIELDS(Biome.MUSHROOM_FIELDS),
    SNOWY_SLOPES(Biome.SNOWY_SLOPES, Biome.JAGGED_PEAKS, Biome.FROZEN_PEAKS, Biome.STONY_PEAKS, Biome.STONY_SHORE),
    MEADOW(Biome.MEADOW),
    CHERRY_GROVE(Biome.CHERRY_GROVE),
    TAIGA(Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.SNOWY_TAIGA, Biome.WINDSWEPT_GRAVELLY_HILLS, Biome.WINDSWEPT_FOREST, Biome.GROVE),
    FOREST(Biome.FOREST),
    FLOWER_FOREST(Biome.FLOWER_FOREST),
    BIRCH(Biome.BIRCH_FOREST, Biome.OLD_GROWTH_BIRCH_FOREST),
    DARK_FOREST(Biome.DARK_FOREST),
    JUNGLE(Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE),
    RIVER(Biome.RIVER, Biome.FROZEN_RIVER),
    SWAMP(Biome.SWAMP),
    MANGROVE_SWAMP(Biome.MANGROVE_SWAMP),
    BEACH(Biome.BEACH, Biome.SNOWY_BEACH),
    PLAINS(Biome.PLAINS, Biome.SUNFLOWER_PLAINS, Biome.SNOWY_PLAINS),
    ICE_SPIKES(Biome.ICE_SPIKES),
    SAVANNA(Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.WINDSWEPT_SAVANNA),
    BADLANDS(Biome.BADLANDS, Biome.WOODED_BADLANDS, Biome.ERODED_BADLANDS),
    DESERT(Biome.DESERT),
    PALE(Biome.PALE_GARDEN);

    private final List<Biome> biomes;

    BiomeGroup(Biome... biomes) {
        this.biomes = List.of(biomes);
    }

    /**
     * Reverse index of {@link #biomes}. Built once, because the lookup below runs for every player on
     * every block they walk into, where scanning all {@code values()} meant a fresh array clone and up
     * to two dozen list scans per step.
     *
     * <p>{@code putIfAbsent} keeps declaration order authoritative, so a biome claimed by two groups
     * resolves to the same one the old first-match loop returned.
     */
    private static final Map<Biome, BiomeGroup> GROUP_OF = buildIndex();

    private static Map<Biome, BiomeGroup> buildIndex() {
        Map<Biome, BiomeGroup> index = new HashMap<>();
        for (BiomeGroup group : values()) {
            for (Biome biome : group.biomes) {
                index.putIfAbsent(biome, group);
            }
        }
        return Map.copyOf(index);
    }

    /** The group claiming this biome, or null when none does. */
    @Nullable
    public static BiomeGroup of(Biome biome) {
        return GROUP_OF.get(biome);
    }
}
