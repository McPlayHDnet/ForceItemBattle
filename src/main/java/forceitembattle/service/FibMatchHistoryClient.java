package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibMatchControllerApi;
import de.threeseconds.openapi.fibservice.client.model.FibCollectionRarityDto;
import de.threeseconds.openapi.fibservice.client.model.FibFoundItemStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.collection.FoundItemsCache;
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.manager.AchievementManager;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.openapitools.client.ApiException;

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
        // The match write is the chokepoint that changes what each participant has collected, so it
        // invalidates their cached global stats -- the same read-through / write-invalidate contract
        // the stat writes use, and what the (future) collect-all achievement read will hang off.
        invalidateParticipants(request);
        executor.runAsync(() -> {
            api.submitMatch(matchId, request);
            return null;
        }, result -> onSuccess.run(), onError);
    }

    public void getFoundItemStatsAsync(UUID playerUuid, Consumer<List<FibFoundItemStatsDto>> onSuccess) {
        getFoundItemStatsAsync(playerUuid, onSuccess, executor::logError);
    }

    public void getFoundItemStatsAsync(UUID playerUuid, Consumer<List<FibFoundItemStatsDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getFoundItemStats(playerUuid), onSuccess, onError);
    }

    public void getCollectionRarityAsync(Consumer<FibCollectionRarityDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(api::getCollectionRarity, onSuccess, onError);
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
}
