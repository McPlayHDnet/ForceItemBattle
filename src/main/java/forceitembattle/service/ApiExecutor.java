package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import forceitembattle.util.Scheduler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

class ApiExecutor {

    private final Plugin plugin;
    
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FIBService-Worker");
        t.setDaemon(true);
        return t;
    });

    ApiExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        worker.execute(() -> {
            try {
                T result = apiCall.execute();
                Scheduler.runSync(() -> onSuccess.accept(result));
            } catch (ApiException e) {
                Scheduler.runSync(() -> onError.accept(e));
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
