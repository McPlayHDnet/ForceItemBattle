package forceitembattle.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibMatchHistoryClient;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link FoundItemsLoader}: read-through caching for a player's collection.
 *
 * <p>Testable without a server or a plugin because the read side of the service seam was finished —
 * this used to take a {@code ForceItemBattle} purely to reach {@code getFibService()}, and now takes
 * the service itself.
 *
 * <p>The rule worth pinning is the one its own comment calls out: <b>a failure is delivered as an
 * empty map but is never cached</b>. An empty collection is a real answer ("collected nothing"), so
 * caching a transient error would stall achievement progress until the next match rather than
 * retrying.
 */
class FoundItemsLoaderTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private FIBServiceClient service;
    private FibMatchHistoryClient matchHistory;
    private FoundItemsCache cache;
    private FoundItemsLoader loader;

    @BeforeEach
    void setUp() {
        this.service = mock(FIBServiceClient.class);
        this.matchHistory = mock(FibMatchHistoryClient.class);
        when(this.service.matchHistory()).thenReturn(this.matchHistory);
        this.cache = new FoundItemsCache();
        this.loader = new FoundItemsLoader(this.service, this.cache);
    }

    private static Map<String, CollectedItem> collection() {
        return Map.of("DIRT", new CollectedItem(Instant.EPOCH, 3L));
    }

    /** Makes the service hand back a collection. */
    private void serviceReturns(Map<String, CollectedItem> collected) {
        doAnswer(invocation -> {
            invocation.getArgument(1, Consumer.class).accept(collected);
            return null;
        }).when(this.matchHistory).foundItems(eq(PLAYER), any(), any());
    }

    /** Makes the service fail. */
    private void serviceFails() {
        doAnswer(invocation -> {
            invocation.getArgument(2, Consumer.class).accept(null);
            return null;
        }).when(this.matchHistory).foundItems(eq(PLAYER), any(), any());
    }

    @Test
    void aMissFetchesAndDelivers() {
        serviceReturns(collection());
        AtomicReference<Map<String, CollectedItem>> delivered = new AtomicReference<>();

        this.loader.load(PLAYER, delivered::set);

        assertNotNull(delivered.get());
        assertEquals(3L, delivered.get().get("DIRT").timesCollected());
    }

    @Test
    void aMissCachesWhatItFetched() {
        serviceReturns(collection());

        this.loader.load(PLAYER, loaded -> { });

        assertNotNull(this.cache.get(PLAYER));
    }

    /** A hit is delivered without going near the service. */
    @Test
    void aHitDoesNotCallTheService() {
        this.cache.put(PLAYER, collection());
        AtomicReference<Map<String, CollectedItem>> delivered = new AtomicReference<>();

        this.loader.load(PLAYER, delivered::set);

        assertNotNull(delivered.get());
        verify(this.matchHistory, never()).foundItems(any(), any(), any());
    }

    @Test
    void aHitDeliversTheCachedInstance() {
        Map<String, CollectedItem> cached = collection();
        this.cache.put(PLAYER, cached);
        AtomicReference<Map<String, CollectedItem>> delivered = new AtomicReference<>();

        this.loader.load(PLAYER, delivered::set);

        assertSame(cached, delivered.get());
    }

    @Test
    void aFailureStillDeliversSomething() {
        serviceFails();
        AtomicReference<Map<String, CollectedItem>> delivered = new AtomicReference<>();

        this.loader.load(PLAYER, delivered::set);

        assertNotNull(delivered.get(), "the caller must not be left hanging");
        assertTrue(delivered.get().isEmpty());
    }

    /**
     * The rule this loader's comment exists for. An empty collection is indistinguishable from a
     * failed fetch once cached, so caching the failure would freeze the player's collection — and
     * their collection achievements — until something else invalidated it.
     */
    @Test
    void aFailureIsNotCached() {
        serviceFails();

        this.loader.load(PLAYER, loaded -> { });

        assertNull(this.cache.get(PLAYER), "a transient failure must not become the cached answer");
    }

    @Test
    void aFailureIsRetriedOnTheNextLoad() {
        serviceFails();
        this.loader.load(PLAYER, loaded -> { });

        serviceReturns(collection());
        AtomicReference<Map<String, CollectedItem>> delivered = new AtomicReference<>();
        this.loader.load(PLAYER, delivered::set);

        assertEquals(3L, delivered.get().get("DIRT").timesCollected());
    }

    @Test
    void invalidatingForcesAReFetch() {
        serviceReturns(collection());
        this.loader.load(PLAYER, loaded -> { });
        this.cache.invalidate(PLAYER);

        assertNull(this.cache.get(PLAYER));
    }
}
