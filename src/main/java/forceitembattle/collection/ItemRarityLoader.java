package forceitembattle.collection;

import de.threeseconds.openapi.fibservice.client.model.FibCollectionRarityDto;
import de.threeseconds.openapi.fibservice.client.model.FibItemRarityDto;
import forceitembattle.ForceItemBattle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemRarityLoader {

    private final ForceItemBattle plugin;
    private final ItemRarityCache cache;
    private final List<Consumer<ItemRarity>> pending = new ArrayList<>();
    private boolean loading;

    public ItemRarityLoader(ForceItemBattle plugin, ItemRarityCache cache) {
        this.plugin = plugin;
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

        this.plugin.getFibService().matchHistory().getCollectionRarityAsync(
                dto -> {
                    ItemRarity rarity = convert(dto);
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

    private static ItemRarity convert(FibCollectionRarityDto dto) {
        Map<String, Long> counts = new HashMap<>();
        if (dto.getItems() != null) {
            for (FibItemRarityDto item : dto.getItems()) {
                counts.put(item.getItemName(), item.getPlayerCount() != null ? item.getPlayerCount() : 0L);
            }
        }
        return new ItemRarity(counts, dto.getTotalPlayers() != null ? dto.getTotalPlayers() : 0L);
    }
}
