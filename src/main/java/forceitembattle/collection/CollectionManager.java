package forceitembattle.collection;

import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.Manager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.service.FIBServiceClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class CollectionManager implements Manager {

    private final ItemDifficultiesManager itemDifficultiesManager;

    private final FoundItemsCache foundItemsCache;
    private final FoundItemsLoader foundItemsLoader;
    private final ItemRarityCache itemRarityCache;
    private final ItemRarityLoader itemRarityLoader;
    
    private final Map<Material, String> displayNames = new ConcurrentHashMap<>();

    /** Namespaced keys of every collectable item. Session-static, built on first use. */
    private Set<String> collectionCatalogue;

    /** The catalogue split into display categories, sorted within each. Session-static. */
    private Map<CollectionCategory, List<Material>> collectionBuckets;

    public CollectionManager(ItemDifficultiesManager itemDifficultiesManager, FIBServiceClient fibService) {
        this.itemDifficultiesManager = itemDifficultiesManager;
        this.foundItemsCache = new FoundItemsCache();
        this.foundItemsLoader = new FoundItemsLoader(fibService, this.foundItemsCache);
        this.itemRarityCache = new ItemRarityCache();
        this.itemRarityLoader = new ItemRarityLoader(fibService, this.itemRarityCache);
    }

    @Override
    public void disable() {
        this.foundItemsCache.clear();
        this.itemRarityCache.clear();
    }

    public String displayNameOf(Material material) {
        return this.displayNames.computeIfAbsent(material, CustomMaterials::nameOf);
    }

    /**
     * Every collectable item's namespaced key — the target set the collection achievements are
     * measured against, and the "everything" axis of the book.
     *
     * <p>Built lazily rather than in {@link #enable()} on purpose: it reads the item registry, so
     * computing it on first use avoids depending on manager registration order.
     */
    public Set<String> getCollectionCatalogue() {
        if (this.collectionCatalogue == null) {
            this.collectionCatalogue = this.itemDifficultiesManager.getCollectableItems().stream()
                    .map(material -> material.getKey().asString())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return this.collectionCatalogue;
    }

    /**
     * The same catalogue, bucketed for display and sorted within each category. Built once and
     * shared by the book and every category page, so nothing re-buckets per open.
     */
    public Map<CollectionCategory, List<Material>> getCollectionBuckets() {
        if (this.collectionBuckets == null) {
            Map<CollectionCategory, List<Material>> buckets = new EnumMap<>(CollectionCategory.class);
            for (CollectionCategory category : CollectionCategory.values()) {
                buckets.put(category, new ArrayList<>());
            }
            for (Material material : this.itemDifficultiesManager.getCollectableItems()) {
                buckets.get(CollectionCategory.categoryOf(material)).add(material);
            }
            buckets.values().forEach(list -> list.sort(Comparator.comparing(material -> material.getKey().asString())));
            this.collectionBuckets = buckets;
        }
        return this.collectionBuckets;
    }
}
