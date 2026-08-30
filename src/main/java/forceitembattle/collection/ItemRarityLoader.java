package forceitembattle.collection;

import forceitembattle.service.FIBServiceClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemRarityLoader {

    private final FIBServiceClient fibService;
    private final ItemRarityCache cache;
    private final List<Consumer<ItemRarity>> pending = new ArrayList<>();
    private boolean loading;

    public ItemRarityLoader(FIBServiceClient fibService, ItemRarityCache cache) {
        this.fibService = fibService;
        this.cache = cache;
    }

    public void load(Consumer<ItemRarity> onLoaded) {
        if (this.cache.isFresh()) {
            onLoaded.accept(this.cache.get());
            return;
        }

        this.pending.add(onLoaded);
        if (this.loading) {
            return;
        }
        this.loading = true;

        this.fibService.matchHistory().itemRarity(
                rarity -> {
                    this.cache.put(rarity);
                    deliver(rarity);
                },
                error -> {
                    ItemRarity fallback = this.cache.get();
                    deliver(fallback != null ? fallback : ItemRarity.empty());
                });
    }

    private void deliver(ItemRarity rarity) {
        this.loading = false;
        List<Consumer<ItemRarity>> waiting = new ArrayList<>(this.pending);
        this.pending.clear();
        waiting.forEach(consumer -> consumer.accept(rarity));
    }

}
