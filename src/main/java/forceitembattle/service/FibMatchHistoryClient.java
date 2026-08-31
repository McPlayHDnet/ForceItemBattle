package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibMatchControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibFoundItemStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.collection.FoundItemsCache;
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.manager.AchievementManager;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import forceitembattle.collection.CollectedItem;
import forceitembattle.collection.ItemRarity;
import java.util.Map;

/**
 * Match-history domain of FIBService: submits one finished game (participants, teams, item
 * activity and the settings snapshot) as a single idempotent PUT keyed on matchId. Wraps
 * {@link FibMatchControllerApi} and shares transport/async plumbing via the {@link ApiExecutor}
 * handed in by the owning {@link FIBServiceClient}.
 */
public class FibMatchHistoryClient {

    private final FibMatchControllerApi api;
    private final ApiExecutor executor;
    private final ForceItemBattle plugin;

    FibMatchHistoryClient(FibMatchControllerApi api, ApiExecutor executor, ForceItemBattle plugin) {
        this.api = api;
        this.executor = executor;
        this.plugin = plugin;
    }

    public void submitMatchAsync(UUID matchId, FibMatchSubmitRequestDto request, Runnable onSuccess) {
        submitMatchAsync(matchId, request, onSuccess, executor::logError);
    }

    public void submitMatchAsync(UUID matchId, FibMatchSubmitRequestDto request, Runnable onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> {
            api.submitMatch(matchId, request);
            return null;
        }, result -> {
            // Invalidated after the write, not before: until the PUT lands the cached found-set still
            // matches the DB, so clearing early only opens a window for an in-flight read to re-cache
            // pre-match data with no invalidation left to follow it. Runs before onSuccess so the
            // collection-achievement evaluation hanging off that callback reads through to fresh data.
            invalidateParticipants(request);
            onSuccess.run();
        }, onError);
    }

    void getFoundItemStatsAsync(UUID playerUuid, Consumer<List<FibFoundItemStatsDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getFoundItemStats(playerUuid), onSuccess, onError);
    }

    private void invalidateParticipants(FibMatchSubmitRequestDto request) {
        if (request.getParticipants() == null) {
            return;
        }
        AchievementManager achievements = this.plugin.getAchievementManager();
        GlobalStatsCache globalStats = achievements.getGlobalStatsCache();
        FoundItemsCache foundItems = this.plugin.getCollectionManager().getFoundItemsCache();
        request.getParticipants().forEach(participant -> {
            UUID playerUuid = participant.getPlayerUuid();
            if (playerUuid != null) {
                globalStats.invalidate(playerUuid);
                foundItems.invalidate(playerUuid);
            }
        });
    }

    // --- the read side, in the game's words --------------------------------------------------

    /** This player's collection, keyed by item name. */
    public void foundItems(UUID playerUuid, Consumer<Map<String, CollectedItem>> onSuccess,
                           Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getFoundItemStats(playerUuid),
                dtos -> onSuccess.accept(ReadModel.collectedItems(dtos)), onError);
    }

    /** How many players hold each item, for the collection screen's rarity line. */
    public void itemRarity(Consumer<ItemRarity> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getCollectionRarity(),
                dto -> onSuccess.accept(ReadModel.itemRarity(dto)), onError);
    }
}
