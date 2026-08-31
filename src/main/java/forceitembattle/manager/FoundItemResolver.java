package forceitembattle.manager;

import forceitembattle.model.BackToBack;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Find;
import forceitembattle.model.FindOutcome;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Rarity;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.service.PlayerStatsWrite;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.Text;
import forceitembattle.util.TimeFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * What happens when a player obtains their force item.
 *
 * <h2>The order is the point</h2>
 *
 * Nine things happen on a find and several of them read state the others destroy, so the sequence
 * below is load-bearing rather than incidental:
 *
 * <ol>
 *   <li>{@link FindOutcome} is computed <b>first</b>. It measures how long the item took, which
 *       means reading the assignment stamp before step 5 overwrites it.</li>
 *   <li>The find is announced, unless a back-to-back is about to announce itself with better
 *       information.</li>
 *   <li>The point and the bell land on the score owner, and the lead is re-evaluated.</li>
 *   <li>The owner advances to its next item.</li>
 *   <li>Stats are written, using the elapsed time measured in step 1 — which is why it had to be
 *       measured there, since the advance has already overwritten the stamp by now.</li>
 *   <li>The back-to-back check runs <b>after</b> the advance, because it asks whether the
 *       <em>new</em> item is already owned. Run it earlier and it inspects the item just found,
 *       which is owned by definition, and every find becomes a chain.</li>
 *   <li>The running random event gets its look last, so it sees a settled world.</li>
 * </ol>
 *
 * <p>{@code FoundItemListener} is now the adapter that unwraps a Bukkit event into a
 * {@link Find}; everything a find <em>means</em> is here.
 *
 * <h2>On the constructor</h2>
 *
 * <p>Eight collaborators is a lot, and that is the honest width of a find. Taking a
 * {@code ForceItemBattle} instead made the same fan-out reach through one field, where it measured
 * nothing and could grow unnoticed. If this list gets longer, that is the signal it always was.
 */
@RequiredArgsConstructor
public class FoundItemResolver implements Manager {

    private final GameSettings settings;
    private final Gamemanager gamemanager;
    private final ScoreboardManager scoreboardManager;
    private final BackToBackManager backToBackManager;
    private final RandomEventManager randomEventManager;
    private final TimerManager timerManager;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final FIBServiceClient fibService;

    /**
     * Resolves one find, start to finish.
     *
     * <p>Re-entrant by design: step 6 can schedule another find a tick later when the next item is
     * already owned, which is how a back-to-back chain runs.
     */
    public void resolve(Find find) {
        ForceItemPlayer finder = find.finder();
        GameContext context = GameContext.of(this.settings, finder);
        FindOutcome outcome = FindOutcome.of(find, context, System.currentTimeMillis());

        if (outcome.announces()) {
            announce(find, context);
        }

        if (outcome.scores()) {
            score(find, context);
        }

        this.gamemanager.advanceMaterials(finder, context);

        if (outcome.recordsStats()) {
            recordStats(find, outcome, context);
        }

        this.scoreboardManager.updateAllPlayers();
        this.backToBackManager.handleAfterFind(finder, context);
        this.randomEventManager.handleFoundItem(find);
    }

    private void announce(Find find, GameContext context) {
        String action = find.skipped() ? "skipped" : "found";
        String unicode = this.itemDifficultiesManager.getUnicodeFromMaterial(true, find.material());

        Component message = Text.of(String.format(
                "<green>%s <gray>%s <reset><shadow:black:0.4>%s</shadow> <gold>%s",
                find.player().getName(), action, unicode, CustomMaterials.nameOf(find.material())));

        GameBroadcast.announce(message, find.finder(), context);
    }

    /** The point, the bell for everyone it belongs to, and the lead re-check. */
    private void score(Find find, GameContext context) {
        ForceItemPlayer finder = find.finder();
        BackToBack backToBack = new BackToBack(find.backToBack());

        if (find.backToBack()) {
            BackToBackProbability probability = this.backToBackManager.calculateProbability(finder);
            backToBack.setPercentage(probability.percentage());
            backToBack.setRarity(probability.formatted());
            backToBack.setRarityType(probability.rarity());

            if (context.statsEnabled() && !context.runMode()) {
                trackRarity(finder, probability.rarity());
            }
        }

        ForceItem forceItem = new ForceItem(
                find.material(),
                TimeFormat.humanised(this.timerManager.getTimeLeft()),
                System.currentTimeMillis(),
                backToBack,
                find.skipped(),
                find.player().getUniqueId());

        finder.recordFoundItem(forceItem);
        finder.squad().forEach(member ->
                member.player().playSound(member.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1));

        this.gamemanager.evaluateLead();
    }

    private void trackRarity(ForceItemPlayer finder, Rarity rarity) {
        this.fibService.statistics().recordRarity(finder.player().getUniqueId(), finder, rarity);
    }

    private void recordStats(Find find, FindOutcome outcome, GameContext context) {
        ForceItemPlayer finder = find.finder();
        finder.setItemStreak(outcome.newItemStreak());

        this.fibService.statistics().recordFind(
                finder,
                context.teamGame(),
                find.material().name(),
                find.skipped(),
                finder.itemStreak(),
                outcome.timeSpentMs());
    }
}
