package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.CollectionAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BiomeGroup;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.MaterialCategory;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class CollectionAchievementHandler<T> implements AchievementHandler<CollectionAchievementProgress<T>> {

    private final Trigger trigger;
    private final Set<T> requiredItems;
    private final ItemExtractor<T> extractor;
    public CollectionAchievementHandler(Trigger trigger, Set<T> requiredItems, ItemExtractor<T> extractor) {
        this.trigger = trigger;
        this.requiredItems = requiredItems;
        this.extractor = extractor;
    }

    // Factory methods
    public static CollectionAchievementHandler<BiomeGroup> biomeHandler(Set<BiomeGroup> requiredBiomes) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            if (event instanceof PlayerMoveEvent moveEvent) {
                // OPTIMIZATION: Only check when player moves to a new block
                int x = moveEvent.getTo().getBlockX();
                int y = moveEvent.getTo().getBlockY();
                int z = moveEvent.getTo().getBlockZ();

                CollectionAchievementProgress.LastCheckedPosition current =
                        new CollectionAchievementProgress.LastCheckedPosition(x, y, z);

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

    public static CollectionAchievementHandler<Biome> caveBiomeHandler(Set<Biome> requiredBiomes) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            if (event instanceof PlayerMoveEvent moveEvent) {
                int x = moveEvent.getTo().getBlockX();
                int y = moveEvent.getTo().getBlockY();
                int z = moveEvent.getTo().getBlockZ();

                CollectionAchievementProgress.LastCheckedPosition current =
                        new CollectionAchievementProgress.LastCheckedPosition(x, y, z);

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

    public static CollectionAchievementHandler<String> dimensionHandler(Set<String> requiredDimensions) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredDimensions, (event, player, progress) -> {
            if (event instanceof PlayerChangedWorldEvent worldEvent) {
                return worldEvent.getPlayer().getWorld().getName();
            }
            return null;
        });
    }

    public static CollectionAchievementHandler<String> woodTypesHandler() {
        return new CollectionAchievementHandler<>(Trigger.OBTAIN_ITEM, MaterialCategory.getAllWoodCategories(), (event, player, progress) -> {
            if (event instanceof FoundItemEvent foundEvent) {
                if (!foundEvent.isSkipped()) {
                    Material item = foundEvent.getFoundItem().getType();
                    return MaterialCategory.getWoodCategory(item);
                }
            }
            return null;
        });
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * The set this achievement needs fully collected (used by progress inspection).
     */
    public Set<T> getRequiredItems() {
        return requiredItems;
    }

    @Override
    public boolean check(Event event, CollectionAchievementProgress<T> progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        T item = extractor.extract(event, forceItemPlayer, progress);
        if (item != null) {
            progress.collected.add(item);
            return progress.collected.containsAll(requiredItems);
        }
        return false;
    }

    @Override
    public CollectionAchievementProgress<T> createProgress() {
        return new CollectionAchievementProgress<>();
    }

    public interface ItemExtractor<T> {
        T extract(Event event, ForceItemPlayer player, CollectionAchievementProgress<T> progress);
    }
}
