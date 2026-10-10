package forceitembattle.collection;

import forceitembattle.service.FIBServiceClient;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** An error delivers an empty map but is not cached: an empty collection is a real answer, so the next load retries. */
public class FoundItemsLoader {

    private final FIBServiceClient fibService;
    private final Map<UUID, Map<String, CollectedItem>> cache;

    public FoundItemsLoader(FIBServiceClient fibService, Map<UUID, Map<String, CollectedItem>> cache) {
        this.fibService = fibService;
        this.cache = cache;
    }

    public void load(UUID playerUuid, Consumer<Map<String, CollectedItem>> onLoaded) {
        Map<String, CollectedItem> cached = this.cache.get(playerUuid);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }

        this.fibService.matchHistory().foundItems(playerUuid,
                collected -> {
                    this.cache.put(playerUuid, collected);
                    onLoaded.accept(collected);
                },
                error -> onLoaded.accept(Map.of()));
    }

}
