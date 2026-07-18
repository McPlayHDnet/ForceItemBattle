package forceitembattle.collection;

public class ItemRarityCache {

    private static final long TTL_MS = 10 * 60 * 1000L;

    private volatile ItemRarity data;
    private volatile long fetchedAt;

    /** The snapshot, fresh or not. Null until the first successful load. */
    public ItemRarity get() {
        return this.data;
    }

    public boolean isFresh() {
        return this.data != null && System.currentTimeMillis() - this.fetchedAt < TTL_MS;
    }

    public void put(ItemRarity rarity) {
        this.data = rarity;
        this.fetchedAt = System.currentTimeMillis();
    }

    public void clear() {
        this.data = null;
        this.fetchedAt = 0L;
    }
}
