package forceitembattle.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.BiomeGroup;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.MaterialCategory;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;

public class CollectionHandler<T> implements AchievementHandler<CollectionProgress<T>> {

    public interface ItemExtractor<T> {
        T extract(Event event, ForceItemPlayer player, CollectionProgress<T> progress);
    }

    private final Trigger trigger;
    private final Set<T> requiredItems;
    private final ItemExtractor<T> extractor;

    public CollectionHandler(Trigger trigger, Set<T> requiredItems, ItemExtractor<T> extractor) {
        this.trigger = trigger;
        this.requiredItems = requiredItems;
        this.extractor = extractor;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    /** The set this achievement needs fully collected (used by progress inspection). */
    public Set<T> getRequiredItems() {
        return requiredItems;
    }

    @Override
    public boolean check(Event event, CollectionProgress<T> progress, ForceItemPlayer forceItemPlayer) {
        T item = extractor.extract(event, forceItemPlayer, progress);
        if (item != null) {
            progress.collected.add(item);
            return progress.collected.containsAll(requiredItems);
        }
        return false;
    }

    @Override
    public CollectionProgress<T> createProgress() {
        return new CollectionProgress<>();
    }

    // Factory methods
    public static CollectionHandler<BiomeGroup> biomeHandler(Set<BiomeGroup> requiredBiomes) {
        return new CollectionHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            if (event instanceof PlayerMoveEvent moveEvent) {
                // OPTIMIZATION: Only check when player moves to a new block
                int x = moveEvent.getTo().getBlockX();
                int y = moveEvent.getTo().getBlockY();
                int z = moveEvent.getTo().getBlockZ();

                CollectionProgress.LastCheckedPosition current =
                        new CollectionProgress.LastCheckedPosition(x, y, z);

                if (current.equals(progress.lastPosition)) {
                    return null; // Same block, no need to check
                }

                progress.lastPosition = current;

                // Now check biome
                Biome biome = moveEvent.getTo().getBlock().getBiome();
                for (BiomeGroup group : BiomeGroup.values()) {
                    if (group.getBiomes().contains(biome)) {
                        return group;
                    }
                }
            }
            return null;
        });
    }

    public static CollectionHandler<Biome> caveBiomeHandler(Set<Biome> requiredBiomes) {
        return new CollectionHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            if (event instanceof PlayerMoveEvent moveEvent) {
                int x = moveEvent.getTo().getBlockX();
                int y = moveEvent.getTo().getBlockY();
                int z = moveEvent.getTo().getBlockZ();

                CollectionProgress.LastCheckedPosition current =
                        new CollectionProgress.LastCheckedPosition(x, y, z);

                if (current.equals(progress.lastPosition)) {
                    return null; // Same block, no need to check
                }

                progress.lastPosition = current;

                Biome biome = moveEvent.getTo().getBlock().getBiome();
                if (requiredBiomes.contains(biome)) {
                    return biome;
                }
            }
            return null;
        });
    }

    public static CollectionHandler<String> dimensionHandler(Set<String> requiredDimensions) {
        return new CollectionHandler<>(Trigger.VISIT, requiredDimensions, (event, player, progress) -> {
            if (event instanceof PlayerChangedWorldEvent worldEvent) {
                return worldEvent.getPlayer().getWorld().getName();
            }
            return null;
        });
    }

    public static CollectionHandler<String> woodTypesHandler() {
        return new CollectionHandler<>(Trigger.OBTAIN_ITEM, MaterialCategory.getAllWoodCategories(), (event, player, progress) -> {
            if (event instanceof FoundItemEvent foundEvent) {
                if (!foundEvent.isSkipped()) {
                    Material item = foundEvent.getFoundItem().getType();
                    return MaterialCategory.getWoodCategory(item);
                }
            }
            return null;
        });
    }
}