package forceitembattle.achievements.global;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import forceitembattle.model.Rarity;
import forceitembattle.model.StatsView;
import java.util.function.ToLongFunction;
import lombok.Getter;

@Getter
public enum GlobalStat {

    GAMES_PLAYED("games played", sum(StatsView::gamesPlayed)),
    GAMES_WON("games won", sum(StatsView::gamesWon)),
    TOTAL_ITEMS("items collected", sum(StatsView::totalItemsFound)),
    DEATHS("deaths", sum(StatsView::deaths)),
    BLOCKS_TRAVELLED("blocks travelled", sum(StatsView::blocksTravelled)),
    WHEEL_OF_FORTUNE_USES("Wheel of Fortune spins", sum(StatsView::wheelOfFortuneUses)),
    ANTIMATTER_TELEPORTS("Antimatter Teleporter uses", sum(StatsView::antimatterTeleports)),

    BACK_TO_BACKS("back-to-backs", sum(view -> Rarity.total(view.rarities()))),
    RARE_BACK_TO_BACKS("rare back-to-backs", sum(view -> Rarity.RARE.count(view.rarities()))),
    EPIC_BACK_TO_BACKS("epic back-to-backs", sum(view -> Rarity.EPIC.count(view.rarities()))),
    LEGENDARY_BACK_TO_BACKS("legendary back-to-backs", sum(view -> Rarity.LEGENDARY.count(view.rarities()))),
    RNGESUS_BACK_TO_BACKS("RNGesus back-to-backs", sum(view -> Rarity.RNGESUS.count(view.rarities()))),
    EXTRAORDINARY_BACK_TO_BACKS("extraordinary back-to-backs", sum(view -> Rarity.EXTRAORDINARY.count(view.rarities()))),

    HIGHEST_WIN_STREAK("consecutive wins", player(FibPlayerStatsDto::getHighestWinStreak));

    /** Lower-case noun phrase for progress lines, e.g. "47 / 250 back-to-backs". */
    private final String label;
    private final ToLongFunction<GlobalStatSources> extractor;

    GlobalStat(String label, ToLongFunction<GlobalStatSources> extractor) {
        this.label = label;
        this.extractor = extractor;
    }

    public long read(GlobalStatSources sources) {
        return extractor.applyAsLong(sources);
    }

    /** Counters: the global value is both modes added together. */
    private static ToLongFunction<GlobalStatSources> sum(ToLongFunction<StatsView> fromView) {
        return sources -> readView(sources.solo(), fromView) + readView(sources.team(), fromView);
    }

    /** Bests and per-round streaks: the global value is the better of the two modes, never their sum. */
    private static ToLongFunction<GlobalStatSources> max(ToLongFunction<StatsView> fromView) {
        return sources -> Math.max(readView(sources.solo(), fromView), readView(sources.team(), fromView));
    }

    /** Genuinely mode-independent: read straight off the player-scoped row. */
    private static ToLongFunction<GlobalStatSources> player(ToLongFunction<FibPlayerStatsDto> fromPlayer) {
        return sources -> sources.player() == null ? 0L : fromPlayer.applyAsLong(sources.player());
    }

    private static long readView(StatsView view, ToLongFunction<StatsView> fromView) {
        return view == null ? 0L : fromView.applyAsLong(view);
    }
}
