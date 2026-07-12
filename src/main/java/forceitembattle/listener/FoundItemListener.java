package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBack;
import forceitembattle.model.BackToBackProbability;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Rarity;
import forceitembattle.model.Team;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
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
            long assignedAt = context.teamGame()
                    ? forceItemPlayer.currentTeam().getLastItemAssignedAt()
                    : forceItemPlayer.lastItemAssignedAt();
            if (assignedAt > 0) {
                timeSpentMs = System.currentTimeMillis() - assignedAt;
            }
        }

        this.plugin.getGamemanager().advanceMaterials(forceItemPlayer, context);
        updateStats(forceItemPlayer, player, context, event.getFoundItem().getType(), event.isSkipped(), timeSpentMs);
        this.plugin.getScoreboardManager().updateAllPlayers();
        this.plugin.getBackToBackManager().handleAfterFind(forceItemPlayer, context);
    }

    private void handleRegularFind(FoundItemEvent event, Player player, ItemStack itemStack, ForceItemPlayer forceItemPlayer, GameContext context) {
        String action = event.isSkipped() ? "skipped" : "found";
        String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType());
        String materialName = this.plugin.getGamemanager().getMaterialName(itemStack.getType());

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

            if (context.statsEnabled() && !context.runMode()) {
                trackRarity(forceItemPlayer, probability.rarity(), context);
            }
        }

        ForceItem forceItem = new ForceItem(
                itemStack.getType(),
                this.plugin.getTimerManager().formatSeconds(this.plugin.getTimerManager().getTimeLeft()),
                System.currentTimeMillis(),
                back2Back,
                event.isSkipped()
        );

        if (context.teamGame()) {
            Team team = forceItemPlayer.currentTeam();
            team.setCurrentScore(team.getCurrentScore() + 1);
            team.addFoundItemToList(forceItem);
            team.getPlayers().forEach(p ->
                    p.player().playSound(p.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1)
            );
        } else {
            forceItemPlayer.setCurrentScore(forceItemPlayer.currentScore() + 1);
            forceItemPlayer.addFoundItemToList(forceItem);
            forceItemPlayer.player().playSound(
                    forceItemPlayer.player().getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BELL,
                    1,
                    1
            );
        }
    }

    private void trackRarity(ForceItemPlayer forceItemPlayer, Rarity rarity, GameContext context) {
        FibStatisticsClient statistics = this.plugin.getFibService().statistics();
        Player player = forceItemPlayer.player();

        var raritiesUpdate = rarity.toRaritiesUpdate();

        if (context.teamGame()) {
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> statistics.updateMemberStatisticsAsync(
                            player.getUniqueId(),
                            teammate.player().getUniqueId(),
                            player.getUniqueId(),
                            FIBServiceClient.memberUpdate().raritiesAdd(raritiesUpdate)
                    ));
        } else {
            statistics.updateSoloStatisticsAsync(player.getUniqueId(),
                    FIBServiceClient.soloUpdate().raritiesAdd(raritiesUpdate));
        }
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

        if (context.teamGame()) {
            if (!isSkipped) {
                int teamStreak = forceItemPlayer.itemStreak();
                statistics.updateTeamStatisticsAsync(
                        player.getUniqueId(),
                        forceItemPlayer.currentTeam().getPlayers().stream()
                                .filter(t -> !t.equals(forceItemPlayer)).findFirst()
                                .map(t -> t.player().getUniqueId()).orElse(player.getUniqueId()),
                        FIBServiceClient.teamUpdate().longestItemStreak(teamStreak)
                );
            }

            long finalTimeSpentMs = timeSpentMs;
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> {
                        var memberUpdate = FIBServiceClient.memberUpdate()
                                .totalItemsFoundAdd(1L)
                                .itemCountsAdd(Map.of(itemName, 1L));
                        if (finalTimeSpentMs > 0) {
                            memberUpdate.totalTimeSpentOnItemsAdd(finalTimeSpentMs);
                        }
                        statistics.updateMemberStatisticsAsync(
                                player.getUniqueId(),
                                teammate.player().getUniqueId(),
                                player.getUniqueId(),
                                memberUpdate
                        );
                    });
        } else {
            var soloUpdate = FIBServiceClient.soloUpdate()
                    .totalItemsFoundAdd(1L)
                    .itemCountsAdd(Map.of(itemName, 1L));
            if (!isSkipped) {
                soloUpdate.longestItemStreak(forceItemPlayer.itemStreak());
            }
            if (timeSpentMs > 0) {
                soloUpdate.totalTimeSpentOnItemsAdd(timeSpentMs);
            }
            statistics.updateSoloStatisticsAsync(player.getUniqueId(), soloUpdate);
        }
    }
}
