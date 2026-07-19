package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Rarity;
import forceitembattle.model.Team;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.InventorySearch;
import forceitembattle.util.Text;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class BackToBackManager implements Manager {

    private final ForceItemBattle plugin;

    /**
     * Called after an item has been found and the next one assigned: decides whether
     * the new item is already owned, updates every streak, and kicks off the chain.
     */
    public void handleAfterFind(ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.runMode()) {
            return;
        }

        Material currentMaterial = context.teamGame()
                ? forceItemPlayer.currentTeam().getCurrentMaterial()
                : forceItemPlayer.getCurrentMaterial();

        BackToBackResult result = check(forceItemPlayer, currentMaterial, context);

        if (!result.hasBackToBack()) {
            resetStreaks(forceItemPlayer, result, context);
            return;
        }

        forceItemPlayer.setBackToBackStreak(forceItemPlayer.backToBackStreak() + 1);

        if (result.teammateWhoHasIt() != null) {
            ForceItemPlayer teammate = result.teammateWhoHasIt();
            teammate.setBackToBackStreak(teammate.backToBackStreak() + 1);
        }

        if (context.teamGame() && forceItemPlayer.currentTeam() != null) {
            Team team = forceItemPlayer.currentTeam();
            team.setBackToBackStreak(team.getBackToBackStreak() + 1);
        }

        // Report the running streak the instant it grows; the service keeps the max,
        // so each chain's peak is captured. (Must come after the team-streak bump above —
        // the reporter reads the shared team streak in team games.)
        updateStreakStats(forceItemPlayer, context);

        triggerBackToBackEvent(forceItemPlayer, result, context);
    }

    /**
     * How improbable the player's current back-to-back was, with the display string
     * and rarity that go with it.
     */
    public BackToBackProbability calculateProbability(ForceItemPlayer forceItemPlayer) {
        Player player = forceItemPlayer.player();
        int totalItemsInPool = this.plugin.getItemDifficultiesManager().getAvailableItems().size();
        boolean backpackEnabled = this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK);
        boolean teamGame = forceItemPlayer.currentTeam() != null;

        Set<Material> uniqueMaterials = new HashSet<>();
        int streak = forceItemPlayer.backToBackStreak();

        if (teamGame) {
            Team team = forceItemPlayer.currentTeam();

            for (ForceItemPlayer teammate : team.getPlayers()) {
                InventorySearch.collectUniqueMaterials(teammate.player().getInventory(), uniqueMaterials);
            }

            if (backpackEnabled) {
                InventorySearch.collectUniqueMaterials(this.plugin.getBackpackManager().getTeamBackpack(team), uniqueMaterials);
            }

            streak = team.getBackToBackStreak();
        } else {
            InventorySearch.collectUniqueMaterials(player.getInventory(), uniqueMaterials);

            if (backpackEnabled) {
                InventorySearch.collectUniqueMaterials(this.plugin.getBackpackManager().getPlayerBackpack(player), uniqueMaterials);
            }
        }

        Material previous = forceItemPlayer.getPreviousMaterial();
        Material current = forceItemPlayer.getCurrentMaterial();

        double baseProbability = Math.min((double) uniqueMaterials.size() / totalItemsInPool, 1.0); // 100% cap
        double probability = Math.pow(baseProbability, streak);
        double probabilityPercent = probability * 100;

        Rarity rarity = Rarity.classify(probability, previous != null && current == previous);
        String formatted = formatPercent(probabilityPercent)
                + " <dark_gray>(<reset>" + rarity.label() + "<dark_gray>)";

        return new BackToBackProbability(probabilityPercent, rarity, formatted);
    }

    private void resetStreaks(ForceItemPlayer forceItemPlayer, BackToBackResult result, GameContext context) {
        forceItemPlayer.setBackToBackStreak(0);

        if (result.teammateWhoHasIt() != null) {
            result.teammateWhoHasIt().setBackToBackStreak(0);
        }

        if (context.teamGame() && forceItemPlayer.currentTeam() != null) {
            Team team = forceItemPlayer.currentTeam();
            team.setBackToBackStreak(0);
            team.getPlayers().forEach(member -> member.setBackToBackStreak(0));
        }
    }

    /**
     * Whether the target material is already owned — by the player, their backpack,
     * or (in team games) a teammate.
     */
    private BackToBackResult check(ForceItemPlayer forceItemPlayer, Material targetMaterial, GameContext context) {
        Material previousMaterial = context.teamGame()
                ? forceItemPlayer.currentTeam().getPreviousMaterial()
                : forceItemPlayer.previousMaterial();

        if (previousMaterial == targetMaterial) {
            return new BackToBackResult(true, null);
        }

        if (InventorySearch.contains(forceItemPlayer.player().getInventory(), targetMaterial)) {
            return new BackToBackResult(true, null);
        }

        if (context.backpackEnabled()) {
            Inventory backpackInventory = context.teamGame()
                    ? this.plugin.getBackpackManager().getTeamBackpack(forceItemPlayer.currentTeam())
                    : this.plugin.getBackpackManager().getPlayerBackpack(forceItemPlayer.player());

            if (InventorySearch.contains(backpackInventory, targetMaterial)) {
                return new BackToBackResult(true, null);
            }
        }

        if (context.teamGame() && forceItemPlayer.currentTeam() != null) {
            for (ForceItemPlayer teammate : forceItemPlayer.currentTeam().getPlayers()) {
                if (teammate.equals(forceItemPlayer)) {
                    continue;
                }

                if (InventorySearch.contains(teammate.player().getInventory(), targetMaterial)) {
                    return new BackToBackResult(true, teammate);
                }
            }
        }

        return new BackToBackResult(false, null);
    }

    private void triggerBackToBackEvent(ForceItemPlayer forceItemPlayer, BackToBackResult result, GameContext context) {
        Player player = forceItemPlayer.player();

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            ItemStack foundItem = new ItemStack(forceItemPlayer.getCurrentMaterial());

            FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
            foundNextItemEvent.setFoundItem(foundItem);
            foundNextItemEvent.setBackToBack(true);
            foundNextItemEvent.setSkipped(false);

            BackToBackProbability probability = calculateProbability(forceItemPlayer);
            String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType());
            String materialName = this.plugin.getGamemanager().getMaterialName(foundItem.getType());

            Component message;
            if (result.teammateWhoHasIt() != null) {
                ForceItemPlayer teammate = result.teammateWhoHasIt();
                message = Text.of(String.format(
                        "<green>%s <gray>was lucky that <green>%s <gray>already owns <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                        player.getName(), teammate.player().getName(), unicode, materialName, probability.formatted()));
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

        FibStatisticsClient statistics = this.plugin.getFibService().statistics();
        Player player = forceItemPlayer.player();

        if (!context.teamGame()) {
            statistics.updateSoloStatisticsAsync(
                    player.getUniqueId(),
                    FIBServiceClient.soloUpdate().highestB2BStreak(forceItemPlayer.backToBackStreak())
            );
            return;
        }

        Team team = forceItemPlayer.currentTeam();
        if (team == null) {
            return;
        }

        ForceItemPlayer teammate = team.getPlayers().stream()
                .filter(t -> !t.equals(forceItemPlayer))
                .findFirst()
                .orElse(null);
        if (teammate == null) {
            return;
        }

        // The b2b streak is a shared team stat — record the shared peak for BOTH members.
        int teamStreak = team.getBackToBackStreak();
        statistics.updateMemberStatisticsAsync(
                player.getUniqueId(), teammate.player().getUniqueId(), player.getUniqueId(),
                FIBServiceClient.memberUpdate().highestB2BStreak(teamStreak));
        statistics.updateMemberStatisticsAsync(
                player.getUniqueId(), teammate.player().getUniqueId(), teammate.player().getUniqueId(),
                FIBServiceClient.memberUpdate().highestB2BStreak(teamStreak));
    }

    private String formatPercent(double probabilityPercent) {
        DecimalFormat df;

        if (probabilityPercent >= 1) {
            df = new DecimalFormat("0.##");
        } else {
            int leadingZeros = 0;
            double temp = probabilityPercent;
            while (temp < 1 && leadingZeros < 15) {
                temp *= 10;
                leadingZeros++;
            }
            df = new DecimalFormat("0." + "#".repeat(Math.max(0, leadingZeros + 2)));
        }

        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(probabilityPercent) + "%";
    }

    private record BackToBackResult(boolean hasBackToBack, ForceItemPlayer teammateWhoHasIt) {
    }
}
