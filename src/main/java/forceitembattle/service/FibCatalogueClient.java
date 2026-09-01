package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibCatalogueControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementCatalogueUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibCatalogueAchievementSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibItemCatalogueUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Achievements;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Catalogue domain of FIBService: publishes what the game <em>defines</em>, so consumers can turn
 * "412 collected" into "412 of 900" without keeping their own copy of the item pool or achievement
 * list. The plugin stays the single source of truth for both; the service is only a mirror.
 *
 * <p>Neither list is rebuilt here — items come straight from
 * {@code CollectionManager#getCollectionCatalogue()} and achievements from the enum. Re-deriving
 * either creates a second definition that can silently disagree with what players see in game.
 *
 * <p>Pushed on enable and fire-and-forget: a failed publish leaves the previous catalogue in place,
 * and a stale denominator is cosmetic where a missing one breaks every completion percentage.
 */
public class FibCatalogueClient {

    private final FibCatalogueControllerApi api;
    private final ApiExecutor executor;
    private final ForceItemBattle plugin;

    FibCatalogueClient(FibCatalogueControllerApi api, ApiExecutor executor, ForceItemBattle plugin) {
        this.api = api;
        this.executor = executor;
        this.plugin = plugin;
    }

    /** Publishes both catalogues. Each is independent -- one failing doesn't hold up the other. */
    public void publishAsync() {
        publishItemsAsync(executor::logError);
        publishAchievementsAsync(executor::logError);
    }

    public void publishItemsAsync(Consumer<ApiException> onError) {
        List<String> items = new ArrayList<>(this.plugin.getCollectionManager().getCollectionCatalogue());
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
