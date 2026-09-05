package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.BiomeGroup;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.CollectionAchievementProgress;
import forceitembattle.collection.MaterialCategory;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
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

    /**
     * The biome the player just walked into, or null when this move did not cross a block boundary
     * (or is not a move at all). Records the position it checked, so a biome lookup — far too hot to
     * do per move event — happens at most once per block.
     */
    private static Biome biomeAtNewBlock(Event event, CollectionAchievementProgress<?> progress) {
        if (!(event instanceof PlayerMoveEvent moveEvent)) {
            return null;
        }

        CollectionAchievementProgress.LastCheckedPosition current =
                new CollectionAchievementProgress.LastCheckedPosition(
                        moveEvent.getTo().getBlockX(),
                        moveEvent.getTo().getBlockY(),
                        moveEvent.getTo().getBlockZ());

        if (current.equals(progress.lastPosition)) {
            return null;
        }
        progress.lastPosition = current;

        return moveEvent.getTo().getBlock().getBiome();
    }

    public static CollectionAchievementHandler<BiomeGroup> biomeHandler(Set<BiomeGroup> requiredBiomes) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            Biome biome = biomeAtNewBlock(event, progress);
            return biome == null ? null : BiomeGroup.of(biome);
        });
    }

    public static CollectionAchievementHandler<Biome> caveBiomeHandler(Set<Biome> requiredBiomes) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredBiomes, (event, player, progress) -> {
            Biome biome = biomeAtNewBlock(event, progress);
            return biome != null && requiredBiomes.contains(biome) ? biome : null;
        });
    }

    public static CollectionAchievementHandler<Dimension> dimensionHandler(Set<Dimension> requiredDimensions) {
        return new CollectionAchievementHandler<>(Trigger.VISIT, requiredDimensions, (event, player, progress) -> {
            if (event instanceof PlayerChangedWorldEvent worldEvent) {
                return Dimension.of(worldEvent.getPlayer());
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
    public boolean check(Event event, CollectionAchievementProgress<T> progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
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
