package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import forceitembattle.ForceItemBattle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;

class ApiExecutor {

    private final ForceItemBattle plugin;
    
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FIBService-Worker");
        t.setDaemon(true);
        return t;
    });

    ApiExecutor(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        worker.execute(() -> {
            try {
                T result = apiCall.execute();
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(result));
            } catch (ApiException e) {
                Bukkit.getScheduler().runTask(plugin, () -> onError.accept(e));
            }
        });
    }

    void shutdown() {
        worker.shutdown();
    }

    void logError(ApiException e) {
        plugin.getLogger().log(Level.SEVERE, "[FIBService] API call failed (HTTP " + e.getCode() + "): " + e.getMessage(), e);
    }

    @FunctionalInterface
    interface ApiCall<T> {
        T execute() throws ApiException;
    }
}
