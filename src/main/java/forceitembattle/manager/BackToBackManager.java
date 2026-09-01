package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Rarity;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.InventorySearch;
import forceitembattle.util.Scheduler;
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

    /** Called after an item has been found <em>and the next one assigned</em>. */
    public void handleAfterFind(ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.runMode()) {
            return;
        }

        BackToBackResult result = check(forceItemPlayer, forceItemPlayer.activeMaterial(), context);

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

        // The service keeps the max, so reporting on every growth captures each chain's peak. Must
        // come after the team-streak bump above — the reporter reads it in team games.
        updateStreakStats(forceItemPlayer, context);

        triggerBackToBackEvent(forceItemPlayer, result, context);
    }

    public BackToBackProbability calculateProbability(ForceItemPlayer forceItemPlayer) {
        Player player = forceItemPlayer.player();
        int totalItemsInPool = this.plugin.getItemDifficultiesManager().getAvailableItems().size();
        boolean backpackEnabled = this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK);
        boolean teamGame = forceItemPlayer.currentTeam() != null;

        // Everything the owner of this streak already holds — both members' inventories in a team.
        Set<Material> uniqueMaterials = new HashSet<>();
        for (ForceItemPlayer member : forceItemPlayer.squad()) {
            InventorySearch.collectUniqueMaterials(member.player().getInventory(), uniqueMaterials);
        }

        int streak = forceItemPlayer.backToBackStreak();

        if (teamGame) {
            Team team = forceItemPlayer.currentTeam();
            if (backpackEnabled) {
                InventorySearch.collectUniqueMaterials(this.plugin.getBackpackManager().getTeamBackpack(team), uniqueMaterials);
            }
            streak = team.getBackToBackStreak();
        } else if (backpackEnabled) {
            InventorySearch.collectUniqueMaterials(this.plugin.getBackpackManager().getPlayerBackpack(player), uniqueMaterials);
        }

        Material previous = forceItemPlayer.activePreviousMaterial();
        Material current = forceItemPlayer.activeMaterial();

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

    /** Whether the target material is already owned — by the player, their backpack, or a teammate. */
    private BackToBackResult check(ForceItemPlayer forceItemPlayer, Material targetMaterial, GameContext context) {
        if (forceItemPlayer.activePreviousMaterial() == targetMaterial) {
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

        ForceItemPlayer teammate = forceItemPlayer.teammate().orElse(null);
        if (teammate != null && InventorySearch.contains(teammate.player().getInventory(), targetMaterial)) {
            return new BackToBackResult(true, teammate);
        }

        return new BackToBackResult(false, null);
    }

    private void triggerBackToBackEvent(ForceItemPlayer forceItemPlayer, BackToBackResult result, GameContext context) {
        Player player = forceItemPlayer.player();

        Scheduler.runLaterSync(() -> {
            ItemStack foundItem = new ItemStack(forceItemPlayer.activeMaterial());

            FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
            foundNextItemEvent.setFoundItem(foundItem);
            foundNextItemEvent.setBackToBack(true);
            foundNextItemEvent.setSkipped(false);

            BackToBackProbability probability = calculateProbability(forceItemPlayer);
            String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType());
            String materialName = CustomMaterials.nameOf(foundItem.getType());

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

        Team team = forceItemPlayer.currentTeam();

        // The peak recorded in a team game is the team's, not this player's, and lands on both
        // members. The stats client owns that; this only says which two numbers are in play.
        this.plugin.getFibService().statistics().recordBackToBackPeak(
                forceItemPlayer,
                context.teamGame() && team != null,
                forceItemPlayer.backToBackStreak(),
                team == null ? 0 : team.getBackToBackStreak());
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
