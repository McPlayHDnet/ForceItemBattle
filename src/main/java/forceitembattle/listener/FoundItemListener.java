package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBack;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Rarity;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.service.PlayerStatsWrite;
import forceitembattle.util.GameBroadcast;
import forceitembattle.util.Text;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class FoundItemListener implements Listener {

    public final ForceItemBattle plugin;

    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getFoundItem();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        GameContext context = GameContext.of(this.plugin, forceItemPlayer);

        if (!event.isBackToBack()) {
            handleRegularFind(event, player, itemStack, forceItemPlayer, context);
        }

        boolean shouldApplyScoreAndSound = !context.runMode() || !event.isSkipped();

        if (shouldApplyScoreAndSound) {
            applyScoreAndSound(forceItemPlayer, itemStack, event, context);
        }

        long timeSpentMs = 0;
        if (!event.isBackToBack()) {
            long assignedAt = forceItemPlayer.activeItemAssignedAt();
            if (assignedAt > 0) {
                timeSpentMs = System.currentTimeMillis() - assignedAt;
            }
        }

        this.plugin.getGamemanager().advanceMaterials(forceItemPlayer, context);
        updateStats(forceItemPlayer, player, context, event.getFoundItem().getType(), event.isSkipped(), timeSpentMs);
        this.plugin.getScoreboardManager().updateAllPlayers();
        this.plugin.getBackToBackManager().handleAfterFind(forceItemPlayer, context);
        this.plugin.getRandomEventManager().handleFoundItem(event, forceItemPlayer);
    }

    private void handleRegularFind(FoundItemEvent event, Player player, ItemStack itemStack, ForceItemPlayer forceItemPlayer, GameContext context) {
        String action = event.isSkipped() ? "skipped" : "found";
        String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType());
        String materialName = CustomMaterials.nameOf(itemStack.getType());

        Component message = Text.of(
                String.format("<green>%s <gray>%s <reset><shadow:black:0.4>%s</shadow> <gold>%s",
                        player.getName(), action, unicode, materialName)
        );

        GameBroadcast.announce(message, forceItemPlayer, context);
    }

    private void applyScoreAndSound(ForceItemPlayer forceItemPlayer, ItemStack itemStack,
                                    FoundItemEvent event, GameContext context) {
        BackToBack back2Back = new BackToBack(event.isBackToBack());

        if (event.isBackToBack()) {
            BackToBackProbability probability = this.plugin.getBackToBackManager().calculateProbability(forceItemPlayer);
            back2Back.setPercentage(probability.percentage());
            back2Back.setRarity(probability.formatted());
            back2Back.setRarityType(probability.rarity());

            if (context.statsEnabled() && !context.runMode()) {
                trackRarity(forceItemPlayer, probability.rarity(), context);
            }
        }

        ForceItem forceItem = new ForceItem(
                itemStack.getType(),
                this.plugin.getTimerManager().formatSeconds(this.plugin.getTimerManager().getTimeLeft()),
                System.currentTimeMillis(),
                back2Back,
                event.isSkipped(),
                forceItemPlayer.player().getUniqueId()
        );

        forceItemPlayer.recordFoundItem(forceItem);
        forceItemPlayer.squad().forEach(member ->
                member.player().playSound(member.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1)
        );

        this.plugin.getGamemanager().evaluateLead();
    }

    private void trackRarity(ForceItemPlayer forceItemPlayer, Rarity rarity, GameContext context) {
        FibStatisticsClient statistics = this.plugin.getFibService().statistics();
        var raritiesUpdate = rarity.toRaritiesUpdate();

        PlayerStatsWrite.record(statistics, forceItemPlayer.player().getUniqueId(), forceItemPlayer,
                () -> FIBServiceClient.soloUpdate().raritiesAdd(raritiesUpdate),
                () -> FIBServiceClient.memberUpdate().raritiesAdd(raritiesUpdate));
    }

    private void updateStats(ForceItemPlayer forceItemPlayer, Player player,
                             GameContext context, Material foundMaterial, boolean isSkipped, long timeSpentMs) {
        if (!context.statsEnabled() || context.runMode()) {
            return;
        }

        FibStatisticsClient statistics = this.plugin.getFibService().statistics();
        String itemName = foundMaterial.name();

        // Item streak counts every obtained item, back-to-backs included; only a skip breaks it.
        if (isSkipped) {
            forceItemPlayer.setItemStreak(0);
        } else {
            forceItemPlayer.setItemStreak(forceItemPlayer.itemStreak() + 1);
        }

        // The shared team row carries the streak; in solo it rides along on the solo update below.
        if (context.teamGame() && !isSkipped) {
            forceItemPlayer.teammate().ifPresent(teammate -> statistics.updateTeamStatisticsAsync(
                    player.getUniqueId(),
                    teammate.player().getUniqueId(),
                    FIBServiceClient.teamUpdate().longestItemStreak(forceItemPlayer.itemStreak())
            ));
        }

        long finalTimeSpentMs = timeSpentMs;
        PlayerStatsWrite.record(statistics, player.getUniqueId(), forceItemPlayer,
                () -> {
                    var soloUpdate = FIBServiceClient.soloUpdate()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (!isSkipped) {
                        soloUpdate.longestItemStreak(forceItemPlayer.itemStreak());
                    }
                    if (finalTimeSpentMs > 0) {
                        soloUpdate.totalTimeSpentOnItemsAdd(finalTimeSpentMs);
                    }
                    return soloUpdate;
                },
                () -> {
                    var memberUpdate = FIBServiceClient.memberUpdate()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (finalTimeSpentMs > 0) {
                        memberUpdate.totalTimeSpentOnItemsAdd(finalTimeSpentMs);
                    }
                    return memberUpdate;
                });
    }
}
