package forceitembattle.listener;

import forceitembattle.util.Text;
import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.stats.FIBServiceHelper;
import forceitembattle.util.*;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class FoundItemListener implements Listener {

    public final ForceItemBattle plugin;

    public static boolean hasItemInInventory(Inventory inventory, Material targetMaterial) {
        if (inventory == null) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (containsMaterial(item, targetMaterial)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsMaterial(ItemStack item, Material targetMaterial) {
        if (item == null || Gamemanager.isBackpack(item) || Gamemanager.isJoker(item)) {
            return false;
        }

        if (item.getType() == targetMaterial) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                if (containsMaterial(shulkerItem, targetMaterial)) {
                    return true;
                }
            }
        }

        if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            for (ItemStack bundleItem : bundleMeta.getItems()) {
                if (containsMaterial(bundleItem, targetMaterial)) {
                    return true;
                }
            }
        }

        return false;
    }

    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getFoundItem();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        GameContext context = new GameContext(
                forceItemPlayer.currentTeam() != null,
                plugin.getSettings().isSettingEnabled(GameSetting.RUN),
                !plugin.getSettings().isSettingEnabled(GameSetting.EVENT),
                plugin.getSettings().isSettingEnabled(GameSetting.STATS),
                plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)
        );

        if (!event.isBackToBack()) {
            handleRegularFind(event, player, itemStack, forceItemPlayer, context);
        }

        boolean shouldApplyScoreAndSound = !context.runMode() || !event.isSkipped();

        if (shouldApplyScoreAndSound) {
            applyScoreAndSound(forceItemPlayer, itemStack, event, context);
        }

        long timeSpentMs = 0;
        if (!event.isBackToBack()) {
            boolean isTeam = forceItemPlayer.currentTeam() != null;
            long assignedAt = isTeam ? forceItemPlayer.currentTeam().getLastItemAssignedAt() : forceItemPlayer.lastItemAssignedAt();
            if (assignedAt > 0) {
                timeSpentMs = System.currentTimeMillis() - assignedAt;
            }
        }

        updateMaterials(forceItemPlayer, event, context);
        updateStats(forceItemPlayer, player, context, event.isBackToBack(), event.getFoundItem().getType(), event.isSkipped(), timeSpentMs);
        this.plugin.getScoreboardManager().updateAllPlayers();
        handleBackToBackCheck(forceItemPlayer, player, context);
    }

    private void handleRegularFind(FoundItemEvent event, Player player, ItemStack itemStack, ForceItemPlayer forceItemPlayer, GameContext context) {
        String action = event.isSkipped() ? "skipped" : "found";
        String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType());
        String materialName = plugin.getGamemanager().getMaterialName(itemStack.getType());

        Component message = Text.of(
                String.format("<green>%s <gray>%s <reset><shadow:black:0.4>%s</shadow> <gold>%s",
                        player.getName(), action, unicode, materialName)
        );

        broadcastMessage(message, forceItemPlayer, context);
        updateBackToBackStats(forceItemPlayer, player, context);
    }

    private void broadcastMessage(Component message, ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.eventDisabled()) {
            Bukkit.broadcast(message);
        } else if (context.teamGame()) {
            forceItemPlayer.currentTeam().getPlayers().forEach(p -> p.player().sendMessage(message));
        } else {
            forceItemPlayer.player().sendMessage(message);
        }
    }

    private void updateBackToBackStats(ForceItemPlayer forceItemPlayer, Player player, GameContext context) {
        if (!context.statsEnabled() || context.runMode()) {
            return;
        }

        int backToBacks = forceItemPlayer.backToBackStreak();
        if (backToBacks == 0) {
            return;
        }

        FIBServiceHelper fibServiceHelper = plugin.getFibServiceHelper();

        if (context.teamGame()) {
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> {
                        fibServiceHelper.updateMemberStatisticsAsync(
                                player.getUniqueId(),
                                teammate.player().getUniqueId(),
                                player.getUniqueId(),
                                FIBServiceHelper.memberUpdate().highestB2BStreak(backToBacks)
                        );
                    });
        } else {
            fibServiceHelper.updateSoloStatisticsAsync(
                    player.getUniqueId(),
                    FIBServiceHelper.soloUpdate().highestB2BStreak(backToBacks)
            );
        }
    }

    private void applyScoreAndSound(ForceItemPlayer forceItemPlayer, ItemStack itemStack,
                                    FoundItemEvent event, GameContext context) {
        BackToBack back2Back = new BackToBack(event.isBackToBack());

        if (event.isBackToBack()) {
            BackToBackProbability probability = calculateBack2BackProbability(forceItemPlayer, context);
            back2Back.setPercentage(probability.percentage());
            back2Back.setRarity(probability.formatted());

            if (context.statsEnabled() && !context.runMode()) {
                trackRarity(forceItemPlayer, probability.rarity(), context);
            }
        }

        ForceItem forceItem = new ForceItem(
                itemStack.getType(),
                plugin.getTimer().formatSeconds(plugin.getTimer().getTimeLeft()),
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
        FIBServiceHelper helper = plugin.getFibServiceHelper();
        Player player = forceItemPlayer.player();

        var raritiesUpdate = rarity.toRaritiesUpdate();

        if (context.teamGame()) {
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> helper.updateMemberStatisticsAsync(
                            player.getUniqueId(),
                            teammate.player().getUniqueId(),
                            player.getUniqueId(),
                            FIBServiceHelper.memberUpdate().raritiesAdd(raritiesUpdate)
                    ));
        } else {
            helper.updateSoloStatisticsAsync(player.getUniqueId(),
                    FIBServiceHelper.soloUpdate().raritiesAdd(raritiesUpdate));
        }
    }

    private void updateMaterials(ForceItemPlayer forceItemPlayer, FoundItemEvent event, GameContext context) {
        if (context.runMode()) {
            updateSeededMaterials(forceItemPlayer, context);
        } else {
            updateRandomMaterials(forceItemPlayer, context);
        }
    }

    private void updateSeededMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        Material currentMaterial = plugin.getGamemanager().generateSeededMaterial();
        long now = System.currentTimeMillis();

        if (context.teamGame()) {
            plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                Team team = p.currentTeam();
                team.setPreviousMaterial(team.getCurrentMaterial());
                team.setCurrentMaterial(team.getNextMaterial());
                team.setNextMaterial(currentMaterial);
                team.setLastItemAssignedAt(now);
            });
        } else {
            plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                p.setPreviousMaterial(p.currentMaterial());
                p.setCurrentMaterial(p.getNextMaterial());
                p.setNextMaterial(currentMaterial);
                p.setLastItemAssignedAt(now);
            });
        }
    }

    private void updateRandomMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        Material nextMaterial = plugin.getGamemanager().generateMaterial();
        long now = System.currentTimeMillis();

        if (context.teamGame()) {
            Team team = forceItemPlayer.currentTeam();
            Material currentMaterial = team.getNextMaterial();
            team.setPreviousMaterial(team.getCurrentMaterial());
            team.setCurrentMaterial(currentMaterial);
            team.setNextMaterial(nextMaterial);
            team.setLastItemAssignedAt(now);
        } else {
            Material currentMaterial = forceItemPlayer.getNextMaterial();
            forceItemPlayer.setPreviousMaterial(forceItemPlayer.currentMaterial());
            forceItemPlayer.setCurrentMaterial(currentMaterial);
            forceItemPlayer.setNextMaterial(nextMaterial);
            forceItemPlayer.setLastItemAssignedAt(now);
        }
    }

    private void updateStats(ForceItemPlayer forceItemPlayer, Player player,
                             GameContext context, boolean isBackToBack, Material foundMaterial, boolean isSkipped, long timeSpentMs) {
        if (!context.statsEnabled() || context.runMode() || isBackToBack) {
            return;
        }

        FIBServiceHelper fibServiceHelper = plugin.getFibServiceHelper();
        String itemName = foundMaterial.name();

        if (isSkipped) {
            forceItemPlayer.setItemStreak(0);
        } else {
            forceItemPlayer.setItemStreak(forceItemPlayer.itemStreak() + 1);
        }

        if (context.teamGame()) {
            int teamStreak = forceItemPlayer.itemStreak();
            if (!isSkipped) {
                fibServiceHelper.updateTeamStatisticsAsync(
                        player.getUniqueId(),
                        forceItemPlayer.currentTeam().getPlayers().stream()
                                .filter(t -> !t.equals(forceItemPlayer)).findFirst()
                                .map(t -> t.player().getUniqueId()).orElse(player.getUniqueId()),
                        FIBServiceHelper.teamUpdate().longestItemStreak(teamStreak)
                );
            }

            long finalTimeSpentMs = timeSpentMs;
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> {
                        var memberUpdate = FIBServiceHelper.memberUpdate()
                                .totalItemsFoundAdd(1L)
                                .itemCountsAdd(Map.of(itemName, 1L));
                        if (finalTimeSpentMs > 0) {
                            memberUpdate.totalTimeSpentOnItemsAdd(finalTimeSpentMs);
                        }
                        fibServiceHelper.updateMemberStatisticsAsync(
                                player.getUniqueId(),
                                teammate.player().getUniqueId(),
                                player.getUniqueId(),
                                memberUpdate
                        );
                    });
        } else {
            var soloUpdate = FIBServiceHelper.soloUpdate()
                    .totalItemsFoundAdd(1L)
                    .itemCountsAdd(Map.of(itemName, 1L));
            if (!isSkipped) {
                soloUpdate.longestItemStreak(forceItemPlayer.itemStreak());
            }
            if (timeSpentMs > 0) {
                soloUpdate.totalTimeSpentOnItemsAdd(timeSpentMs);
            }
            fibServiceHelper.updateSoloStatisticsAsync(player.getUniqueId(), soloUpdate);
        }
    }

    private void handleBackToBackCheck(ForceItemPlayer forceItemPlayer, Player player, GameContext context) {
        if (context.runMode()) {
            return;
        }

        Material currentMaterial = context.teamGame()
                ? forceItemPlayer.currentTeam().getCurrentMaterial()
                : forceItemPlayer.getCurrentMaterial();

        BackToBackResult result = checkForBackToBack(forceItemPlayer, currentMaterial, context);

        if (result.hasBackToBack()) {
            forceItemPlayer.setBackToBackStreak(forceItemPlayer.backToBackStreak() + 1);

            if (result.teammateWhoHasIt() != null) {
                ForceItemPlayer teammate = result.teammateWhoHasIt();
                teammate.setBackToBackStreak(teammate.backToBackStreak() + 1);
            }

            if (context.teamGame() && forceItemPlayer.currentTeam() != null) {
                Team team = forceItemPlayer.currentTeam();
                team.setBackToBackStreak(team.getBackToBackStreak() + 1);
            }

            triggerBackToBackEvent(forceItemPlayer, player, result, context);
        } else {
            forceItemPlayer.setBackToBackStreak(0);

            if (result.teammateWhoHasIt() != null) {
                ForceItemPlayer teammate = result.teammateWhoHasIt();
                teammate.setBackToBackStreak(0);
            }

            if (context.teamGame() && forceItemPlayer.currentTeam() != null) {
                forceItemPlayer.currentTeam().setBackToBackStreak(0);
            }
        }
    }

    private void triggerBackToBackEvent(ForceItemPlayer forceItemPlayer, Player player, BackToBackResult result, GameContext context) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack foundItem = new ItemStack(forceItemPlayer.getCurrentMaterial());
            FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
            foundNextItemEvent.setFoundItem(foundItem);
            foundNextItemEvent.setBackToBack(true);
            foundNextItemEvent.setSkipped(false);

            BackToBackProbability probability = calculateBack2BackProbability(forceItemPlayer, context);
            String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType());
            String materialName = plugin.getGamemanager().getMaterialName(foundItem.getType());

            Component message;
            if (result.teammateWhoHasIt() != null) {
                // Teammate has the item
                ForceItemPlayer teammate = result.teammateWhoHasIt();
                message = Text.of(
                        String.format("<green>%s <gray>was lucky that <green>%s <gray>already owns <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                                player.getName(), teammate.player().getName(), unicode, materialName, probability.formatted())
                );
            } else {
                // Player themselves has the item
                message = Text.of(
                        String.format("<green>%s <gray>was lucky to already own <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                                player.getName(), unicode, materialName, probability.formatted())
                );
            }

            probability.rarity().playSound(player);

            GameContext broadcastContext = new GameContext(
                    forceItemPlayer.currentTeam() != null,
                    false,
                    !plugin.getSettings().isSettingEnabled(GameSetting.EVENT),
                    false,
                    false
            );

            broadcastMessage(message, forceItemPlayer, broadcastContext);
            Bukkit.getPluginManager().callEvent(foundNextItemEvent);
        }, 1L);
    }

    private BackToBackProbability calculateBack2BackProbability(ForceItemPlayer forceItemPlayer, GameContext context) {
        Player player = forceItemPlayer.player();
        int totalItemsInPool = plugin.getItemDifficultiesManager().getAvailableItems().size();
        boolean isBackpackEnabled = plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK);
        boolean isTeamGame = forceItemPlayer.currentTeam() != null;

        Set<Material> uniqueMaterials = new HashSet<>();
        int streak = forceItemPlayer.backToBackStreak();

        if (isTeamGame) {
            Team team = forceItemPlayer.currentTeam();

            for (ForceItemPlayer teammate : team.getPlayers()) {
                collectUniqueMaterials(teammate.player().getInventory(), uniqueMaterials);
            }

            if (isBackpackEnabled) {
                Inventory teamBackpack = plugin.getBackpack().getTeamBackpack(team);
                collectUniqueMaterials(teamBackpack, uniqueMaterials);
            }

            streak = Math.max(streak, team.getBackToBackStreak());
        } else {
            collectUniqueMaterials(player.getInventory(), uniqueMaterials);

            if (isBackpackEnabled) {
                Inventory backpack = plugin.getBackpack().getPlayerBackpack(player);
                collectUniqueMaterials(backpack, uniqueMaterials);
            }
        }

        int totalItems = uniqueMaterials.size();

        Material prev = forceItemPlayer.getPreviousMaterial();
        Material current = forceItemPlayer.getCurrentMaterial();

        double baseProbability = (double) totalItems / totalItemsInPool;
        baseProbability = Math.min(baseProbability, 1.0); // 100% cap

        double probability = Math.pow(baseProbability, streak);
        double probabilityPercent = probability * 100;

        Rarity rarity = Rarity.classify(probability, prev != null && current == prev);

        String formattedProbability;
        if (probabilityPercent >= 1) {
            DecimalFormat df = new DecimalFormat("0.##");
            df.setRoundingMode(RoundingMode.HALF_UP);
            formattedProbability = df.format(probabilityPercent) + "%";
        } else {
            int leadingZeros = 0;
            double temp = probabilityPercent;
            while (temp < 1 && leadingZeros < 15) {
                temp *= 10;
                leadingZeros++;
            }

            int totalDecimals = leadingZeros + 2;

            DecimalFormat df = new DecimalFormat("0." + "#".repeat(Math.max(0, totalDecimals)));
            df.setRoundingMode(RoundingMode.HALF_UP);
            formattedProbability = df.format(probabilityPercent) + "%";
        }

        String formatted = formattedProbability + " <dark_gray>(<reset>" + rarity.label() + "<dark_gray>)";

        return new BackToBackProbability(probabilityPercent, rarity, formatted);
    }

    private BackToBackResult checkForBackToBack(ForceItemPlayer player, Material targetMaterial, GameContext context) {
        Material previousMaterial = context.teamGame()
                ? player.currentTeam().getPreviousMaterial()
                : player.previousMaterial();

        if (previousMaterial == targetMaterial) {
            return new BackToBackResult(true, null);
        }

        if (hasItemInInventory(player.player().getInventory(), targetMaterial)) {
            return new BackToBackResult(true, null);
        }

        if (context.backpackEnabled()) {
            Inventory backpackInventory = context.teamGame()
                    ? plugin.getBackpack().getTeamBackpack(player.currentTeam())
                    : plugin.getBackpack().getPlayerBackpack(player.player());

            if (hasItemInInventory(backpackInventory, targetMaterial)) {
                return new BackToBackResult(true, null);
            }
        }

        if (context.teamGame() && player.currentTeam() != null) {
            for (ForceItemPlayer teammate : player.currentTeam().getPlayers()) {
                if (teammate.equals(player)) {
                    continue;
                }

                if (hasItemInInventory(teammate.player().getInventory(), targetMaterial)) {
                    return new BackToBackResult(true, teammate);
                }
            }
        }

        return new BackToBackResult(false, null);
    }

    private void collectUniqueMaterials(Inventory inventory, Set<Material> uniqueMaterials) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || Gamemanager.isJoker(item) || Gamemanager.isBackpack(item)) {
                continue;
            }

            Material type = item.getType();
            uniqueMaterials.add(type);

            if (Tag.SHULKER_BOXES.isTagged(type)) {
                collectUniqueMaterialsFromShulkerBox(item, uniqueMaterials);
            }

            if (Tag.ITEMS_BUNDLES.isTagged(type)) {
                collectUniqueMaterialsFromBundle(item, uniqueMaterials);
            }
        }
    }

    private void collectUniqueMaterialsFromShulkerBox(ItemStack shulkerBox, Set<Material> uniqueMaterials) {
        ItemMeta meta = shulkerBox.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return;
        }

        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox box)) {
            return;
        }

        for (ItemStack item : box.getInventory().getContents()) {
            if (item == null || Gamemanager.isJoker(item) || Gamemanager.isBackpack(item)) {
                continue;
            }

            Material type = item.getType();
            uniqueMaterials.add(type);

            if (Tag.ITEMS_BUNDLES.isTagged(type)) {
                collectUniqueMaterialsFromBundle(item, uniqueMaterials);
            }
        }
    }

    private void collectUniqueMaterialsFromBundle(ItemStack bundle, Set<Material> uniqueMaterials) {
        ItemMeta meta = bundle.getItemMeta();
        if (!(meta instanceof BundleMeta bundleMeta)) {
            return;
        }

        if (!bundleMeta.hasItems()) {
            return;
        }

        for (ItemStack item : bundleMeta.getItems()) {
            if (item == null || Gamemanager.isJoker(item)) {
                continue;
            }

            uniqueMaterials.add(item.getType());
        }
    }

    private record GameContext(boolean teamGame, boolean runMode, boolean eventDisabled,
                               boolean statsEnabled, boolean backpackEnabled) {
    }

    private record BackToBackResult(boolean hasBackToBack, ForceItemPlayer teammateWhoHasIt) {
    }
}