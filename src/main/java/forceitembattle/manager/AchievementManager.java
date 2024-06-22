package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.*;
import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AchievementManager {

    private final ForceItemBattle forceItemBattle;

    private final Map<Achievements, AchievementProgress> achievementProgressMap;
    public final Map<UUID, PlayerProgress> playerProgressMap = new HashMap<>();

    public AchievementManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.achievementProgressMap = new HashMap<>();
        for(Achievements achievements : Achievements.values()) {
            this.achievementProgressMap.put(achievements, new AchievementProgress());
        }
    }

    public void handleEvent(Player player, Event event, Trigger trigger) {
        UUID uuid = player.getUniqueId();
        this.playerProgressMap.putIfAbsent(uuid, new PlayerProgress());
        PlayerProgress playerProgress = this.playerProgressMap.get(uuid);
        ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid);
        ForceItemPlayerStats playerStats = this.forceItemBattle.getStatsManager().playerStats(player.getName());

        if (!ForceItemBattle.getInstance().getGamemanager().isMidGame()) {
            return;
        }

        for (Achievements achievements : Achievements.values()) {
            if (playerStats.achievementsDone().contains(achievements.getTitle())) continue;
            Condition condition = achievements.getCondition();
            if (condition.getTrigger() == trigger) {
                AchievementProgress achievementProgress = playerProgress.getProgress(achievements);
                boolean achieved = false;
                switch (trigger) {
                    case OBTAIN_ITEM -> achieved = this.checkObtainItemCondition(event, achievementProgress, condition);
                    case OBTAIN_ITEM_IN_TIME -> achieved = this.checkObtainItemInTimeCondition(event, achievementProgress, condition);
                    case BACK_TO_BACK -> achieved = this.checkBackToBackCondition(event, achievementProgress, condition);
                    case EATING -> achieved = this.checkEatingCondition(event, achievementProgress, condition);
                    case DYING -> achieved = this.checkDyingCondition(event, achievementProgress, condition);
                    case SKIP_ITEM -> achieved = this.checkSkipItemCondition(event, achievementProgress, condition);
                    case TRADING -> achieved = this.checkTradingCondition(event, achievementProgress, condition);
                    case VISIT -> achieved = this.checkVisitCondition(event, achievementProgress, condition);
                    case LOOT -> achieved = this.checkLootCondition(event, achievementProgress, condition);
                    case ACHIEVEMENT -> achieved = this.checkAchievementCondition(event, achievementProgress, condition);
                }

                if(achieved) {
                    Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(player, achievements));
                    if(this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                        if(!trigger.isAchieveableInTeams()) return;
                        for(ForceItemPlayer teamPlayers : forceItemPlayer.currentTeam().getPlayers()) {
                            if(teamPlayers == forceItemPlayer) continue;
                            Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(teamPlayers.player(), achievements));
                        }
                    }
                }
            }
        }
    }

    private boolean checkObtainItemCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        boolean itemMatchesDimension = condition.getDimensions() == null || condition.getDimensions().stream()
                .anyMatch(dimension -> isItemFoundInDimension(foundItemEvent.getFoundItem().getType(), dimension));

        if (itemMatchesDimension) {
            if (foundItemEvent.isSkipped() && condition.isNoSkip()) {
                progress.resetConsecutiveCount();
                return false;
            }

            if (condition.isConsecutive()) {
                progress.incrementConsecutiveCount();
                return progress.getConsecutiveCount() == condition.getAmount();
            } else {
                progress.incrementItemCount();
                return progress.getItemCount() == condition.getAmount();
            }
        } else {
            if (condition.isConsecutive()) {
                progress.resetConsecutiveCount();
            }
        }

        return false;
    }

    private boolean checkObtainItemInTimeCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = (currentTime - progress.getStartTime()) / 1000;

        if (condition.isNoSkip() && foundItemEvent.isSkipped() || foundItemEvent.isBackToBack()) {
            progress.resetItemCount();
            progress.setStartTime(currentTime);
            return false;
        }

        if (condition.getWithinSeconds() > 0 && elapsedTime <= condition.getWithinSeconds()) {
            progress.incrementItemCount();
        } else if (condition.getTimeFrame() > 0 && elapsedTime >= condition.getTimeFrame()) {
            progress.incrementItemCount();
        } else {
            progress.resetItemCount();
            progress.setStartTime(currentTime);
        }

        return progress.getItemCount() == condition.getAmount();
    }

    private boolean checkBackToBackCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        if (!foundItemEvent.isBackToBack()) {
            progress.resetBack2BackCount();
            return false;
        }

        if (condition.isSameItem() && foundItemEvent.getFoundItem().getType() != progress.getLastItemType()) {
            progress.resetBack2BackCount();
        } else {
            progress.incrementBack2BackCount();
        }

        progress.setLastItemType(foundItemEvent.getFoundItem().getType());

        return progress.getBack2BackCount() == condition.getAmount();
    }

    private boolean checkSkipItemCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        if(!foundItemEvent.isSkipped()) {
            progress.resetSkipCount();
            return false;
        }
        progress.incrementSkipCount();
        return !condition.isConsecutive() || progress.getSkipCount() == condition.getAmount();
    }

    private boolean checkDyingCondition(Event event, AchievementProgress progress, Condition condition) {
        if(!(event instanceof PlayerDeathEvent playerDeathEvent)) {
            return false;
        }

        progress.incrementDeathCount();
        if(!this.forceItemBattle.getGamemanager().isEndGame()) {
            return false;
        }

        return progress.getDeathCounter() == condition.getAmount();
    }

    private boolean checkEatingCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerItemConsumeEvent playerItemConsumeEvent)) {
            return false;
        }

        ItemMeta itemMeta = playerItemConsumeEvent.getItem().getItemMeta();
        if (playerItemConsumeEvent.getItem().getType() == condition.getCustomItem().getMaterial()) {
            if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == condition.getCustomItem().getCustomModelData()) {
                progress.incrementItemCount();
                return progress.getItemCount() == condition.getAmount();
            }
        }

        return false;
    }

    private boolean checkTradingCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerTradeEvent playerTradeEvent)) {
            return false;
        }

        if(playerTradeEvent.getVillager() instanceof WanderingTrader) {
            progress.incrementItemCount();
            return progress.getItemCount() == condition.getAmount();
        }
        return false;
    }

    private boolean checkVisitCondition(Event event, AchievementProgress progress, Condition condition) {
        if (event instanceof PlayerMoveEvent playerMoveEvent) {
            if(condition.getBiomeList() == null) {
                return false;
            }
            Biome toBiome = playerMoveEvent.getTo().getBlock().getBiome();
            BiomeGroup enteredBiomeGroup = this.getBiomeGroup(toBiome);

            if (enteredBiomeGroup != null) {
                progress.addVisitedBiome(enteredBiomeGroup);
                return progress.getVisitedBiomes().containsAll(condition.getBiomeList());
            }
        } else if (event instanceof PlayerChangedWorldEvent playerChangedWorldEvent) {
            if(condition.getDimensions() == null) {
                return false;
            }
            String currentWorld = playerChangedWorldEvent.getPlayer().getWorld().getName();

            if (!currentWorld.isEmpty()) {
                progress.addVisitedDimension(currentWorld);
                return progress.getVisitedDimensions().containsAll(condition.getDimensions());
            }
        } else {
            return false;
        }
        return false;
    }

    private boolean checkLootCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerInteractEvent playerInteractEvent)) {
            return false;
        }

        Player player = playerInteractEvent.getPlayer();
        ForceItemPlayer forceItemPlayer = ForceItemBattle.getInstance().getGamemanager().getForceItemPlayer(player.getUniqueId());
        Block clickedBlock = playerInteractEvent.getClickedBlock();

        if (clickedBlock == null) {
            return false;
        }

        BlockState blockState = clickedBlock.getState();
        if (!(blockState instanceof Chest chest)) {
            return false;
        }

        if (!chest.hasLootTable()) {
            return false;
        }

        for (ItemStack item : chest.getInventory().getContents()) {
            if (item == null) continue;

            System.out.println(item.getItemMeta().displayName());

            if (matchesCustomItem(item, condition) || matchesCurrentMaterial(item, forceItemPlayer)) {
                progress.incrementItemCount();
                return progress.getItemCount() == condition.getAmount();
            }
        }

        return false;
    }

    private boolean checkAchievementCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerGrantAchievementEvent playerGrantAchievementEvent)) {
            return false;
        }

        ForceItemPlayerStats playerStats = this.forceItemBattle.getStatsManager().playerStats(playerGrantAchievementEvent.getPlayer().getName());
        return playerStats.achievementsDone().size() == Achievements.values().length;
    }

    private boolean isItemFoundInDimension(Material itemType, String dimension) {
        return switch (dimension) {
            case "world" -> this.forceItemBattle.getItemDifficultiesManager().getOverworldItems().contains(itemType);
            case "world_nether" -> this.forceItemBattle.getItemDifficultiesManager().getNetherItems().contains(itemType);
            case "world_the_end" -> this.forceItemBattle.getItemDifficultiesManager().getEndItems().contains(itemType);
            default -> false;
        };
    }

    private BiomeGroup getBiomeGroup(Biome biome) {
        for(BiomeGroup biomeGroup : BiomeGroup.values()) {
            if(biomeGroup.getBiomes().contains(biome)) {
                return biomeGroup;
            }
        }
        return null;
    }

    private boolean matchesCustomItem(ItemStack item, Condition condition) {
        CustomItem customItem = condition.getCustomItem();

        if (customItem == null) {
            return false;
        }

        String plainDisplayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());

        return plainDisplayName.contains(customItem.getCheckedName()) ||
                (item.getItemMeta().hasCustomModelData() &&
                        item.getItemMeta().getCustomModelData() == customItem.getCustomModelData());
    }

    private boolean matchesCurrentMaterial(ItemStack item, ForceItemPlayer forceItemPlayer) {
        return item.getType() == forceItemPlayer.currentMaterial();
    }

    public AchievementProgress getProgress(Achievements achievement) {
        return this.achievementProgressMap.get(achievement);
    }

    public void grantAchievement(ForceItemPlayerStats playerStats, Achievements achievement) {
        if(playerStats.achievementsDone().contains(achievement.getTitle())) {
            return;
        }

        List<String> achievementsDone = playerStats.achievementsDone();
        achievementsDone.add(achievement.getTitle());
        playerStats.setAchievementsDone(achievementsDone);

        this.forceItemBattle.getStatsManager().saveStats();
    }

    public void revokeAchievement(ForceItemPlayerStats playerStats, Achievements achievement) {
        if(!playerStats.achievementsDone().contains(achievement.getTitle())) {
            return;
        }

        List<String> achievementsDone = playerStats.achievementsDone();
        achievementsDone.remove(achievement.getTitle());
        playerStats.setAchievementsDone(achievementsDone);

        this.forceItemBattle.getStatsManager().saveStats();
    }
}
