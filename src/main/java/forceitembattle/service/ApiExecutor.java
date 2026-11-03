package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import forceitembattle.util.Scheduler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

class ApiExecutor {

    private static final ApiException OFFLINE = new ApiException("FIBService is not configured");

    private final Plugin plugin;
    private final boolean online;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FIBService-Worker");
        t.setDaemon(true);
        return t;
    });

    ApiExecutor(Plugin plugin, boolean online) {
        this.plugin = plugin;
        this.online = online;
    }

    /** Offline, every call fails through its own error path, so callers waiting on one still complete. */
    <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        if (!online) {
            Scheduler.runSync(() -> onError.accept(OFFLINE));
            return;
        }
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
        if (!online) {
            return;
        }
        plugin.getLogger().log(Level.SEVERE, "[FIBService] API call failed (HTTP " + e.getCode() + "): " + e.getMessage(), e);
    }

    @FunctionalInterface
    interface ApiCall<T> {
        T execute() throws ApiException;
    }
}
