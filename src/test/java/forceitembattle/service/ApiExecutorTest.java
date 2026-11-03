package forceitembattle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import forceitembattle.util.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class ApiExecutorTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        Scheduler.init(MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    @Test
    void offlineFailsEveryCallWithoutSendingOrLogging() {
        Plugin plugin = mock(Plugin.class);
        ApiExecutor executor = new ApiExecutor(plugin, false);
        AtomicBoolean sent = new AtomicBoolean();
        List<ApiException> errors = new ArrayList<>();

        executor.runAsync(() -> {
            sent.set(true);
            return null;
        }, result -> { }, errors::add);
        this.server.getScheduler().performTicks(1);
        errors.forEach(executor::logError);

        assertFalse(sent.get());
        assertEquals(1, errors.size());
        verifyNoInteractions(plugin);
    }
}
