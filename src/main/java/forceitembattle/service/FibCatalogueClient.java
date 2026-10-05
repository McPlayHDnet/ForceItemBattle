package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibCatalogueControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementCatalogueUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibCatalogueAchievementSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibItemCatalogueUpdateRequestDto;
import forceitembattle.achievements.Achievements;
import forceitembattle.collection.CollectionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.plugin.Plugin;

/**
 * Publishes the item pool and achievement list so consumers can show "412 of 900"; the plugin stays
 * the source of truth. Fire-and-forget: a failed publish leaves the previous catalogue in place.
 */
public class FibCatalogueClient {

    private final FibCatalogueControllerApi api;
    private final ApiExecutor executor;
    private final Plugin plugin;

    // Cycle: CollectionManager is built from this client; read only inside publishAsync.
    private final Supplier<CollectionManager> collection;

    FibCatalogueClient(FibCatalogueControllerApi api, ApiExecutor executor, Plugin plugin,
                       Supplier<CollectionManager> collection) {
        this.api = api;
        this.executor = executor;
        this.plugin = plugin;
        this.collection = collection;
    }

    /** Publishes both catalogues. Each is independent -- one failing doesn't hold up the other. */
    public void publishAsync() {
        publishItemsAsync(executor::logError);
        publishAchievementsAsync(executor::logError);
    }

    public void publishItemsAsync(Consumer<ApiException> onError) {
        List<String> items = new ArrayList<>(this.collection.get().getCollectionCatalogue());
        if (items.isEmpty()) {
            // The service rejects an empty catalogue anyway; caught here so the log names the real
            // cause rather than an HTTP 400.
            this.plugin.getLogger().warning("[FIBService] Item catalogue is empty; skipping publish");
            return;
        }
        FibItemCatalogueUpdateRequestDto request = new FibItemCatalogueUpdateRequestDto().items(items);
        this.executor.runAsync(() -> {
            this.api.updateItems(request);
            return null;
        }, result -> { }, onError);
    }

    public void publishAchievementsAsync(Consumer<ApiException> onError) {
        List<FibCatalogueAchievementSubmitDto> achievements = new ArrayList<>();
        for (Achievements achievement : Achievements.values()) {
            achievements.add(new FibCatalogueAchievementSubmitDto()
                    // name(), not an ordinal or display string: it is the identifier the unlock rows
                    // use, so renaming a constant breaks that join.
                    .achievementId(achievement.name())
                    .title(achievement.getTitle())
                    .description(achievement.getDescription())
                    .scope(achievement.getScope().name()));
        }
        FibAchievementCatalogueUpdateRequestDto request =
                new FibAchievementCatalogueUpdateRequestDto().achievements(achievements);
        this.executor.runAsync(() -> {
            this.api.updateAchievements(request);
            return null;
        }, result -> { }, onError);
    }
}
