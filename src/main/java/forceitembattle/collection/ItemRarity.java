package forceitembattle.collection;

import java.util.Map;

public record ItemRarity(Map<String, Long> playerCounts, long totalPlayers) {

    public static ItemRarity empty() {
        return new ItemRarity(Map.of(), 0L);
    }

    public long playersWith(String itemName) {
        return this.playerCounts.getOrDefault(itemName, 0L);
    }

    /** Share of players holding this item, 0-100. Zero when nobody has played yet. */
    public double percentWith(String itemName) {
        if (this.totalPlayers <= 0) {
            return 0.0;
        }
        return (double) playersWith(itemName) / this.totalPlayers * 100.0;
    }
}
