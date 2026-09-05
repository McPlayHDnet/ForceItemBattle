package forceitembattle.manager;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.InventorySearch;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Being handed an item you already own, and how unlikely that was.
 *
 * <p>The arithmetic is {@link BackToBackProbability}'s and the streak is the Score Owner's; what is
 * left here is gathering what a player holds and saying so.
 *
 * <p><b>One find, one number.</b> The odds are computed once — after the streak is bumped, over a
 * single snapshot of what is owned — and the same figure is announced and recorded. There used to be
 * two computations: {@code FoundItemResolver.score()} asked for one <em>before</em> the bump, and the
 * announcement asked again a tick later <em>after</em> it, so the percentage in a player's stats row
 * was systematically a chain shorter than the one they were shown. Three inventory walks became one
 * for the same reason.
 */
@RequiredArgsConstructor
public class BackToBackManager implements Manager {

    private final GameSettings settings;
    private final ItemDifficultiesManager items;
    private final BackpackManager backpacks;
    private final FIBServiceClient fibService;

    /**
     * Called after an item has been found <em>and the next one assigned</em>, since the chain is about
     * the item just handed out.
     *
     * @return the odds of the chain this find extended, or empty when it broke one
     */
    public Optional<BackToBackProbability> handleAfterFind(ForceItemPlayer forceItemPlayer,
                                                           GameContext context) {
        if (context.runMode()) {
            return Optional.empty();
        }

        Owned owned = gather(forceItemPlayer, forceItemPlayer.activeMaterial(), context);

        if (!owned.hasBackToBack()) {
            forceItemPlayer.scoreOwner().resetStreak();
            return Optional.empty();
        }

        // One holder, so there is nothing to keep in step. This used to be three writes — the finder,
        // whichever teammate happened to hold the item, and the team — mirrored by a reset that
        // zeroed every member instead, and the two had already drifted.
        forceItemPlayer.scoreOwner().bumpStreak();

        BackToBackProbability probability = oddsFor(forceItemPlayer, owned);

        // The service keeps the max, so reporting on every growth captures each chain's peak. After
        // the bump: the reporter reads the streak.
        updateStreakStats(forceItemPlayer, context);
        announce(forceItemPlayer, owned.teammateWhoHasIt(), probability, context);

        return Optional.of(probability);
    }

    /**
     * What this owner holds, and whether it includes the item they were just handed.
     *
     * @param teammateWhoHasIt set only when the teammate is the <em>only</em> holder — the message
     *                         credits them, and crediting someone for an item the finder already had
     *                         themselves would read as nonsense
     */
    private record Owned(Set<Material> materials, boolean hasBackToBack,
                         @Nullable ForceItemPlayer teammateWhoHasIt) {
    }

    /**
     * One pass over every inventory that counts. It was three: {@code check} walked them with
     * {@code contains}, then the odds walked them again with {@code collectUniqueMaterials}, a tick
     * later and so able to disagree about what was held.
     */
    private Owned gather(ForceItemPlayer forceItemPlayer, Material target, GameContext context) {
        Player player = forceItemPlayer.player();

        Set<Material> ownHalf = new HashSet<>();
        InventorySearch.collectUniqueMaterials(player.getInventory(), ownHalf);

        if (context.backpackEnabled()) {
            Inventory backpack = context.teamGame()
                    ? this.backpacks.getTeamBackpack(forceItemPlayer.currentTeam())
                    : this.backpacks.getPlayerBackpack(player);
            InventorySearch.collectUniqueMaterials(backpack, ownHalf);
        }

        ForceItemPlayer teammate = forceItemPlayer.teammate().orElse(null);
        Set<Material> teammateHalf = new HashSet<>();
        if (teammate != null) {
            InventorySearch.collectUniqueMaterials(teammate.player().getInventory(), teammateHalf);
        }

        boolean selfHasIt = forceItemPlayer.activePreviousMaterial() == target
                || ownHalf.contains(target);
        boolean teammateHasIt = teammateHalf.contains(target);

        // Union built in place: ownHalf is local, both halves have already been consulted above, and
        // the result is only ever read for its size.
        ownHalf.addAll(teammateHalf);

        return new Owned(ownHalf, selfHasIt || teammateHasIt,
                !selfHasIt && teammateHasIt ? teammate : null);
    }

    private BackToBackProbability oddsFor(ForceItemPlayer forceItemPlayer, Owned owned) {
        Material previous = forceItemPlayer.activePreviousMaterial();
        Material current = forceItemPlayer.activeMaterial();

        return BackToBackProbability.of(
                owned.materials().size(),
                this.items.getAvailableItems().size(),
                forceItemPlayer.backToBackStreak(),
                previous != null && current == previous);
    }

    private void announce(ForceItemPlayer forceItemPlayer, @Nullable ForceItemPlayer teammate,
                          BackToBackProbability probability, GameContext context) {
        Player player = forceItemPlayer.player();

        Scheduler.runLaterSync(() -> {
            ItemStack foundItem = new ItemStack(forceItemPlayer.activeMaterial());

            FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
            foundNextItemEvent.setFoundItem(foundItem);
            foundNextItemEvent.setBackToBack(true);
            foundNextItemEvent.setSkipped(false);

            String unicode = this.items.getUnicodeFromMaterial(true, foundItem.getType());
            String materialName = CustomMaterials.nameOf(foundItem.getType());

            Component message;
            if (teammate != null) {
                message = Text.of(String.format(
                        "<green>%s <gray>was lucky that <green>%s <gray>already owns <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                        player.getName(), teammate.player().getName(), unicode, materialName,
                        probability.formatted()));
            } else {
                message = Text.of(String.format(
                        "<green>%s <gray>was lucky to already own <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                        player.getName(), unicode, materialName, probability.formatted()));
            }

            probability.rarity().playSound(player);

            GameBroadcast.announce(message, forceItemPlayer, context);
            Bukkit.getPluginManager().callEvent(foundNextItemEvent);
        }, 1L);
    }

    private void updateStreakStats(ForceItemPlayer forceItemPlayer, GameContext context) {
        if (!context.statsEnabled() || context.runMode()) {
            return;
        }

        int streak = forceItemPlayer.backToBackStreak();

        // In a team game the peak is the team's and lands on both member rows; the write rules own
        // that routing, and since the streak moved onto the Score Owner both numbers are the same one.
        this.fibService.statisticsWrites().recordBackToBackPeak(
                forceItemPlayer, context.teamGame() && forceItemPlayer.currentTeam() != null,
                streak, streak);
    }
}
