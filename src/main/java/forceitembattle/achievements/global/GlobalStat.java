package forceitembattle.achievements.global;

import forceitembattle.model.Rarity;
import forceitembattle.model.StatsView;
import java.util.function.ToLongFunction;
import lombok.Getter;

@Getter
public enum GlobalStat {

    GAMES_PLAYED("games played", Aggregation.SUM, StatsView::gamesPlayed),
    GAMES_WON("games won", Aggregation.SUM, StatsView::gamesWon),
    TOTAL_ITEMS("items collected", Aggregation.SUM, StatsView::totalItemsFound),
    BACK_TO_BACKS("back-to-backs", Aggregation.SUM,
            view -> view.rarities() == null ? 0L : Rarity.total(view.rarities()));

    /**
     * How a stat's solo and team values fold into one global value.
     *
     * <p>Counters add. A "highest ever" or "longest streak" does not — its global value is
     * the better of the two sides, not their sum. Every stat here happens to be SUM today;
     * declaring it per stat anyway means a MAX-natured stat added later cannot be silently
     * summed into a wrong number.
     */
    public enum Aggregation {
        SUM {
            @Override
            long combine(long a, long b) {
                return a + b;
            }
        },
        MAX {
            @Override
            long combine(long a, long b) {
                return Math.max(a, b);
            }
        };

        abstract long combine(long a, long b);
    }

    /** Lower-case noun phrase for progress lines, e.g. "47 / 250 back-to-backs". */
    private final String label;
    private final Aggregation aggregation;
    private final ToLongFunction<StatsView> extractor;

    GlobalStat(String label, Aggregation aggregation, ToLongFunction<StatsView> extractor) {
        this.label = label;
        this.aggregation = aggregation;
        this.extractor = extractor;
    }

    public long read(StatsView view) {
        return extractor.applyAsLong(view);
    }

    /**
     * Folds a player's solo and team views into one global value. Either side may be
     * {@code null} — a player who has only ever played solo has no team stats at all.
     */
    public long combine(StatsView solo, StatsView team) {
        if (solo == null && team == null) {
            return 0L;
        }
        if (solo == null) {
            return read(team);
        }
        if (team == null) {
            return read(solo);
        }
        return aggregation.combine(read(solo), read(team));
    }
}
