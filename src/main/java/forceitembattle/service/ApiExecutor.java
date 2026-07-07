package forceitembattle.service;

import forceitembattle.ForceItemBattle;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.openapitools.client.ApiException;

/**
 * Shared async plumbing for the FIBService sub-clients. Runs a blocking API call
 * off the main thread and delivers the result (or error) back on the main thread,
 * so callbacks are always Bukkit-safe. Held by composition rather than inheritance
 * so each sub-client stays independent.
 */
class ApiExecutor {

    private final ForceItemBattle plugin;

    ApiExecutor(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = apiCall.execute();
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(result));
            } catch (ApiException e) {
                Bukkit.getScheduler().runTask(plugin, () -> onError.accept(e));
            }
        });
    }

    void logError(ApiException e) {
        plugin.getLogger().log(Level.SEVERE, "[FIBService] API call failed (HTTP " + e.getCode() + "): " + e.getMessage(), e);
    }

    @FunctionalInterface
    interface ApiCall<T> {
        T execute() throws ApiException;
    }
}
