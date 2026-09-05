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
import forceitembattle.model.RoundClock;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.Text;
import forceitembattle.util.TimeFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;

/**
 * What happens when a player obtains their force item. {@code FoundItemListener} is the adapter that
 * unwraps a Bukkit event into a {@link Find}; everything a find <em>means</em> is here.
 *
 * <p>The order in {@link #resolve} is load-bearing. Two constraints hold it in place:
 *
 * <ul>
 *   <li>{@link FindOutcome} is computed <b>first</b>, because it measures how long the item took and
 *       the advance overwrites the assignment stamp it reads.</li>
 *   <li>The back-to-back check runs <b>after</b> the advance, because it asks whether the
 *       <em>new</em> item is already owned. Earlier, it inspects the item just found — owned by
 *       definition — and every find becomes a chain.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class FoundItemResolver implements Manager {

    private final GameSettings settings;
    private final Gamemanager gamemanager;
    private final ForceItemAssignment assignment;
    private final ScoreboardManager scoreboardManager;
    private final BackToBackManager backToBackManager;
    private final RandomEventManager randomEventManager;
    private final RoundClock roundClock;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final FIBServiceClient fibService;

    /**
     * Re-entrant by design: the back-to-back check can schedule another find a tick later when the
     * next item is already owned, which is how a chain runs.
     */
    public void resolve(Find find) {
        // The event permits a find with no stack, which Find carries through as a null material.
        // Nothing below can do anything with one — an item is announced and recorded by name.
        if (find.material() == null) {
            return;
        }

        ForceItemPlayer finder = find.finder();
        GameContext context = GameContext.of(this.settings, finder);
        FindOutcome outcome = FindOutcome.of(find, context, System.currentTimeMillis());

        if (outcome.announces()) {
            announce(find, context);
        }

        this.assignment.advanceFor(finder, context.runMode());

        // After the advance, because a chain is about the item just handed out; before score(),
        // because score() records the odds this returns. Those two constraints are why score() sits
        // below the advance rather than above it, which is where it used to be — it asked the
        // back-to-back manager for the odds itself, before the streak had been bumped, so the
        // percentage it recorded was systematically one chain short of the one announced.
        Optional<BackToBackProbability> backToBack =
                this.backToBackManager.handleAfterFind(finder, context);

        if (outcome.scores()) {
            score(find, context, backToBack);
        }

        if (outcome.recordsStats()) {
            recordStats(find, outcome, context);
        }

        this.scoreboardManager.updateAllPlayers();
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

    private void score(Find find, GameContext context, Optional<BackToBackProbability> odds) {
        ForceItemPlayer finder = find.finder();
        BackToBack backToBack = new BackToBack(find.backToBack());

        if (find.backToBack()) {
            odds.ifPresent(probability -> {
                backToBack.setPercentage(probability.percentage());
                backToBack.setRarity(probability.formatted());
                backToBack.setRarityType(probability.rarity());

                if (context.statsEnabled() && !context.runMode()) {
                    trackRarity(finder, probability.rarity());
                }
            });
        }

        ForceItem forceItem = new ForceItem(
                find.material(),
                TimeFormat.humanised(this.roundClock.secondsLeft()),
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
        this.fibService.statisticsWrites().recordRarity(finder.player().getUniqueId(), finder, rarity);
    }

    private void recordStats(Find find, FindOutcome outcome, GameContext context) {
        ForceItemPlayer finder = find.finder();
        finder.setItemStreak(outcome.newItemStreak());

        this.fibService.statisticsWrites().recordFind(
                finder,
                context.teamGame(),
                find.material().name(),
                find.skipped(),
                finder.itemStreak(),
                outcome.timeSpentMs());
    }
}
