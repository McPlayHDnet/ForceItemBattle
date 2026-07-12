package forceitembattle.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.util.BiomeSearchResult;
import org.jetbrains.annotations.Nullable;

public final class BiomeSearch {

    public static final int SEARCH_RADIUS = 6400;

    private BiomeSearch() {
    }

    @Nullable
    public static Biome resolve(NamespacedKey key) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .get(key);
    }

    @Nullable
    public static Location nearest(Location origin, Biome biome) {
        BiomeSearchResult result = origin.getWorld().locateNearestBiome(origin, SEARCH_RADIUS, biome);
        return result != null ? result.getLocation() : null;
    }
}
