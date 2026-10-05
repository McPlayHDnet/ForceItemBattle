package forceitembattle.collection;

import forceitembattle.service.FIBServiceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemRarityLoader {

    private static final long TTL_MS = 10 * 60 * 1000L;

    private final FIBServiceClient fibService;
    private final List<Consumer<ItemRarity>> pending = new ArrayList<>();
    private boolean loading;

    private ItemRarity cached;
    private long fetchedAt;

    public ItemRarityLoader(FIBServiceClient fibService) {
        this.fibService = fibService;
    }

    public void load(Consumer<ItemRarity> onLoaded) {
        if (this.cached != null && System.currentTimeMillis() - this.fetchedAt < TTL_MS) {
            onLoaded.accept(this.cached);
            return;
        }

        this.pending.add(onLoaded);
        if (this.loading) {
            return;
        }
        this.loading = true;

        this.fibService.matchHistory().itemRarity(
                rarity -> {
                    this.cached = rarity;
                    this.fetchedAt = System.currentTimeMillis();
                    deliver(rarity);
                },
                error -> deliver(this.cached != null ? this.cached : ItemRarity.empty()));
    }

    public void clear() {
        this.cached = null;
    }

    private void deliver(ItemRarity rarity) {
        this.loading = false;
        List<Consumer<ItemRarity>> waiting = new ArrayList<>(this.pending);
        this.pending.clear();
        waiting.forEach(consumer -> consumer.accept(rarity));
    }
}
