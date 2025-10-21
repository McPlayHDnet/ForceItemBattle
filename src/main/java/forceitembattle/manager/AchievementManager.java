package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.*;
import forceitembattle.util.*;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AchievementManager {

    private final ForceItemBattle forceItemBattle;
    private final Map<UUID, PlayerProgress> playerProgressMap = new HashMap<>();
    private final Map<Team, PlayerProgress> teamProgressMap = new HashMap<>();
    private final AchievementStorage achievementStorage;

    // Rare mob drops
    private static final Set<Material> RARE_MOB_DROPS = Set.of(
            Material.TRIDENT,
            Material.WITHER_SKELETON_SKULL
    );

    public AchievementManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.achievementStorage = new AchievementStorage(forceItemBattle);
    }

    public void handleEvent(Player player, Event event, Trigger trigger) {
        UUID uuid = player.getUniqueId();

        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid);
        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            return;
        }

        this.playerProgressMap.putIfAbsent(uuid, new PlayerProgress());
        PlayerProgress playerProgress = this.playerProgressMap.get(uuid);

        // Handle team progress if in team mode
        PlayerProgress teamProgress = null;
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) && forceItemPlayer.currentTeam() != null) {
            Team team = forceItemPlayer.currentTeam();
            this.teamProgressMap.putIfAbsent(team, new PlayerProgress());
            teamProgress = this.teamProgressMap.get(team);
        }

        // Check each achievement
        for (Achievements achievement : Achievements.values()) {
            Condition condition = achievement.getCondition();

            if (condition.getTrigger() != trigger) {
                continue;
            }

            // Determine which progress to use (team or player)
            PlayerProgress progressToUse = playerProgress;
            boolean useTeamProgress = trigger.isAchieveableInTeams() &&
                    !condition.isPlayerBased() &&
                    teamProgress != null;

            if (useTeamProgress) {
                progressToUse = teamProgress;

                // For team achievements, check if ALL team members already have it
                // If everyone has it, no need to keep checking
                boolean allTeamMembersHaveIt = true;
                for (ForceItemPlayer teamMember : forceItemPlayer.currentTeam().getPlayers()) {
                    if (!this.achievementStorage.hasAchievement(teamMember.player().getUniqueId(), achievement)) {
                        allTeamMembersHaveIt = false;
                        break;
                    }
                }

                if (allTeamMembersHaveIt) {
                    continue;
                }
            } else {
                // For player-based achievements, skip if player already has it
                if (this.achievementStorage.hasAchievement(uuid, achievement)) {
                    continue;
                }
            }

            AchievementProgress achievementProgress = progressToUse.getProgress(achievement);
            boolean achieved = false;

            switch (trigger) {
                case OBTAIN_ITEM ->
                        achieved = this.checkObtainItemCondition(event, achievementProgress, condition, forceItemPlayer);
                case OBTAIN_ITEM_IN_TIME ->
                        achieved = this.checkObtainItemInTimeCondition(event, achievementProgress, condition, forceItemPlayer);
                case BACK_TO_BACK -> achieved = this.checkBackToBackCondition(event, achievementProgress, condition);
                case EATING -> achieved = this.checkEatingCondition(event, achievementProgress, condition);
                case DYING -> achieved = this.checkDyingCondition(event, achievementProgress, condition);
                case SKIP_ITEM -> achieved = this.checkSkipItemCondition(event, achievementProgress, condition);
                case TRADING -> achieved = this.checkTradingCondition(event, achievementProgress, condition);
                case VISIT -> achieved = this.checkVisitCondition(event, achievementProgress, condition);
                case LOOT -> achieved = this.checkLootCondition(event, achievementProgress, condition, forceItemPlayer);
                case BEEHIVE_HARVEST -> achieved = this.checkBeehiveHarvestCondition(event, achievementProgress, condition);
                case INVENTORY_FULL ->
                        achieved = this.checkInventoryFullCondition(event, achievementProgress, condition, forceItemPlayer);
                case ACHIEVEMENT ->
                        achieved = this.checkAchievementCondition(event, achievementProgress, condition, uuid);
            }

            if (achieved) {
                this.grantAchievement(player, achievement, useTeamProgress, forceItemPlayer);
            }
        }
    }

    private void grantAchievement(Player player, Achievements achievement, boolean isTeamAchievement, ForceItemPlayer forceItemPlayer) {
        // Save achievement for the player
        this.achievementStorage.addAchievement(player.getUniqueId(), achievement);

        // Fire event for the player
        Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(player, achievement));

        // If team achievement, grant to all team members
        if (isTeamAchievement && forceItemPlayer.currentTeam() != null) {
            for (ForceItemPlayer teamMember : forceItemPlayer.currentTeam().getPlayers()) {
                if (!teamMember.equals(forceItemPlayer)) {
                    this.achievementStorage.addAchievement(teamMember.player().getUniqueId(), achievement);
                    Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(teamMember.player(), achievement));
                }
            }
        }
    }

    private boolean checkObtainItemCondition(Event event, AchievementProgress progress, Condition condition, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        // Track the last FOUND item (not skipped, not B2B) for Déjà Vu achievement
        if (!foundItemEvent.isSkipped() && !foundItemEvent.isBackToBack()) {
            progress.setLastItemType(foundItemEvent.getFoundItem().getType());
        }

        Material itemType = foundItemEvent.getFoundItem().getType();

        // Check dimension condition
        boolean itemMatchesDimension = condition.getDimensions() == null ||
                condition.getDimensions().stream()
                        .anyMatch(dimension -> isItemFoundInDimension(itemType, dimension));

        // Check wood types
        if (condition.isWoodTypes()) {
            if (MaterialCategory.isWoodType(itemType)) {
                progress.addWoodType(itemType);
            }

            // Get unique wood categories collected
            Set<String> woodCategories = new HashSet<>();
            for (Material wood : progress.getWoodTypesCollected()) {
                String category = MaterialCategory.getWoodCategory(wood);
                if (category != null) {
                    woodCategories.add(category);
                }
            }

            return woodCategories.size() >= MaterialCategory.getRequiredWoodCategoriesCount();
        }

        // Check stone types
        if (condition.isStoneTypes()) {
            if (MaterialCategory.isStoneType(itemType)) {
                progress.addStoneType(itemType);
                if (condition.isConsecutive()) {
                    progress.incrementConsecutiveCount();
                    if (progress.getConsecutiveCount() >= condition.getAmount()) {
                        return true;
                    }
                }
            } else {
                progress.resetConsecutiveCount();
                progress.getStoneTypesCollected().clear();
            }
            return false;
        }

        // Check rare mob drops
        if (condition.isRareMobDrop()) {
            if (RARE_MOB_DROPS.contains(itemType)) {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
            return false;
        }

        if (itemMatchesDimension) {
            if (foundItemEvent.isSkipped() && condition.isNoSkip()) {
                progress.resetConsecutiveCount();
                return false;
            }

            if (condition.isConsecutive()) {
                progress.incrementConsecutiveCount();
                return progress.getConsecutiveCount() >= condition.getAmount();
            } else {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
        } else {
            if (condition.isConsecutive()) {
                progress.resetConsecutiveCount();
            }
        }

        return false;
    }

    private boolean checkObtainItemInTimeCondition(Event event, AchievementProgress progress, Condition condition, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long gameStartTime = this.forceItemBattle.getGamemanager().getGameStartTime();
        long elapsedGameTime = (currentTime - gameStartTime) / 1000;
        long elapsedItemTime = (currentTime - progress.getLastItemTime()) / 1000;

        // First player to collect an item (GLOBALLY across all players)
        if (condition.isFirstPlayer()) {
            // Check if ANY player has collected an item yet
            boolean anyPlayerCollectedItem = false;
            for (PlayerProgress p : this.playerProgressMap.values()) {
                if (p.getProgress(Achievements.EARLY_BIRD).isFirstItemCollected()) {
                    anyPlayerCollectedItem = true;
                    break;
                }
            }

            // If no one has collected yet and this is not a skip or b2b
            if (!anyPlayerCollectedItem && !foundItemEvent.isSkipped() && !foundItemEvent.isBackToBack()) {
                progress.setFirstItemCollected(true);
                return true;
            }
            return false;
        }

        // Close call - item collected within last X seconds of round
        if (condition.getCloseCallSeconds() > 0) {
            long roundDuration = this.forceItemBattle.getConfig().getLong("settings.roundDuration") * 60;
            long timeRemaining = roundDuration - elapsedGameTime;

            if (!foundItemEvent.isSkipped() && timeRemaining <= condition.getCloseCallSeconds() && timeRemaining > 0) {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
            return false;
        }

        // Procrastinator - skip after X seconds
        if (condition.getSkipAfterSeconds() > 0) {
            long timeSinceReceived = (currentTime - progress.getItemReceivedTime()) / 1000;
            if (foundItemEvent.isSkipped() && timeSinceReceived >= condition.getSkipAfterSeconds()) {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
            return false;
        }

        if (condition.isNoSkip() && foundItemEvent.isSkipped() || foundItemEvent.isBackToBack()) {
            progress.resetItemCount();
            progress.setStartTime(currentTime);
            progress.setItemReceivedTime(currentTime);
            return false;
        }

        // Within X seconds from start
        if (condition.getWithinSeconds() > 0 && elapsedGameTime <= condition.getWithinSeconds()) {
            progress.incrementItemCount();
            progress.setLastItemTime(currentTime);
            return progress.getItemCount() >= condition.getAmount();
        } else if (condition.getWithinSeconds() > 0 && elapsedGameTime > condition.getWithinSeconds()) {
            return false;
        }

        // Took X seconds to find
        if (condition.getTimeFrame() > 0 && elapsedItemTime >= condition.getTimeFrame()) {
            progress.incrementItemCount();
            progress.setLastItemTime(currentTime);
            return progress.getItemCount() >= condition.getAmount();
        } else if (condition.getTimeFrame() > 0 && elapsedItemTime < condition.getTimeFrame()) {
            progress.resetItemCount();
            progress.setStartTime(currentTime);
            return false;
        }

        return progress.getItemCount() >= condition.getAmount();
    }

    private boolean checkBackToBackCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        if (!foundItemEvent.isBackToBack()) {
            progress.resetBack2BackCount();
            return false;
        }

        Material currentItem = foundItemEvent.getFoundItem().getType();

        // Accidental Genius - skip then get the same item via B2B
        if (condition.isSkippedThenGot()) {
            // ← CHANGE: Get from ForceItemPlayer instead of AchievementProgress
            Player player = foundItemEvent.getPlayer();
            ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager()
                    .getForceItemPlayer(player.getUniqueId());

            if (forceItemPlayer == null) {
                return false;
            }

            Material lastSkipped = forceItemPlayer.getLastSkippedMaterial();

            if (foundItemEvent.isPreviousItemWasSkipped() &&
                    lastSkipped != null &&
                    lastSkipped == currentItem) {
                progress.incrementBack2BackCount();
                forceItemPlayer.setLastSkippedMaterial(null); // Reset after achievement
                return progress.getBack2BackCount() >= condition.getAmount();
            }
            return false;
        }

        // Déjà Vu - same item b2b (player FOUND it previously, NOT skipped)
        if (condition.isSameItem()) {
            // For Déjà Vu, we should ALSO use per-player tracking
            // ← CHANGE: Get from ForceItemPlayer
            Player player = foundItemEvent.getPlayer();
            ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager()
                    .getForceItemPlayer(player.getUniqueId());

            if (forceItemPlayer == null) {
                return false;
            }

            // Use lastItemType from progress (this is fine since it's set per-player in checkObtainItemCondition)
            if (!foundItemEvent.isPreviousItemWasSkipped() &&
                    progress.getLastItemType() != null &&
                    currentItem == progress.getLastItemType()) {
                progress.incrementBack2BackCount();
                return progress.getBack2BackCount() >= condition.getAmount();
            } else {
                progress.resetBack2BackCount();
                return false;
            }
        } else {
            // Regular B2B counting
            progress.incrementBack2BackCount();
            return progress.getBack2BackCount() >= condition.getAmount();
        }
    }


    private boolean checkSkipItemCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof FoundItemEvent foundItemEvent)) {
            return false;
        }

        if (!foundItemEvent.isSkipped()) {
            progress.resetSkipCount();
            return false;
        }

        // Track the skipped item for "Accidental Genius" achievement
        progress.setLastSkippedItem(foundItemEvent.getFoundItem().getType());

        // Check if skip happened within X seconds of receiving item (for "Fuck this")
        if (condition.getWithinSeconds() > 0) {
            long currentTime = System.currentTimeMillis();
            long timeSinceReceived = (currentTime - progress.getItemReceivedTime()) / 1000;

            if (timeSinceReceived <= condition.getWithinSeconds()) {
                progress.incrementSkipCount();
                return progress.getSkipCount() >= condition.getAmount();
            }
            return false;
        }

        // Regular consecutive skip tracking (for "Unlucky")
        progress.incrementSkipCount();
        return condition.isConsecutive() ? progress.getSkipCount() >= condition.getAmount() : false;
    }

    private boolean checkDyingCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerDeathEvent)) {
            return false;
        }

        progress.incrementDeathCount();

        // Check immediately - we want to track deaths during the game
        // Achievement will only be granted at game end if death count matches condition
        return false; // Don't trigger yet, wait for game end
    }

    private boolean checkEatingCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerItemConsumeEvent playerItemConsumeEvent)) {
            return false;
        }

        if (condition.getCustomItem() != null) {
            ItemStack item = playerItemConsumeEvent.getItem();
            if (matchesCustomItem(item, condition)) {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
        }

        return false;
    }

    private boolean checkTradingCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerTradeEvent playerTradeEvent)) {
            return false;
        }

        if (playerTradeEvent.getVillager() instanceof WanderingTrader) {
            progress.incrementTradeCount();
            return progress.getTradeCount() >= condition.getAmount();
        }

        return false;
    }

    private boolean checkVisitCondition(Event event, AchievementProgress progress, Condition condition) {
        // Dimension visit
        if (event instanceof PlayerChangedWorldEvent playerChangedWorldEvent) {
            String worldName = playerChangedWorldEvent.getPlayer().getWorld().getName();
            progress.addVisitedDimension(worldName);

            if (condition.getDimensions() != null) {
                return progress.getVisitedDimensions().containsAll(condition.getDimensions());
            }
        }

        // Biome visit
        if (event instanceof PlayerMoveEvent playerMoveEvent) {
            if (playerMoveEvent.getFrom().getBlock().equals(playerMoveEvent.getTo().getBlock())) {
                return false;
            }

            Biome biome = playerMoveEvent.getTo().getBlock().getBiome();
            BiomeGroup biomeGroup = getBiomeGroup(biome);

            if (biomeGroup != null) {
                progress.addVisitedBiome(biomeGroup);

                if (condition.getBiomeGroups() != null) {
                    return progress.getVisitedBiomes().containsAll(condition.getBiomeGroups());
                }
            }
        }

        return false;
    }

    private boolean checkLootCondition(Event event, AchievementProgress progress, Condition condition, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof PlayerInteractEvent playerInteractEvent)) {
            return false;
        }

        Block clickedBlock = playerInteractEvent.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Chest)) {
            return false;
        }

        Chest chest = (Chest) clickedBlock.getState();
        Inventory chestInventory = chest.getInventory();

        // Check for specific material looted
        if (condition.getMaterial() != null) {
            for (ItemStack item : chestInventory.getContents()) {
                if (item != null && item.getType() == condition.getMaterial()) {
                    progress.addLootedMaterial(item.getType());
                    int count = (int) progress.getLootedMaterials().stream()
                            .filter(m -> m == condition.getMaterial())
                            .count();

                    if (count >= condition.getAmount()) {
                        return true;
                    }
                }
            }
            return false;
        }

        // Check for needed item (current item player needs)
        if (condition.isNeededItem()) {
            Material neededMaterial = forceItemPlayer.getCurrentMaterial();
            for (ItemStack item : chestInventory.getContents()) {
                if (item != null && item.getType() == neededMaterial) {
                    progress.incrementLootCount();
                    return progress.getLootCount() >= condition.getAmount();
                }
            }
            return false;
        }

        // Check for custom items (legendary, cavendish, etc.)
        if (condition.getCustomItem() != null) {
            for (ItemStack item : chestInventory.getContents()) {
                if (item != null && matchesCustomItem(item, condition)) {
                    progress.incrementLootCount();
                    return progress.getLootCount() >= condition.getAmount();
                }
            }
        }

        return false;
    }

    private boolean checkInventoryFullCondition(Event event, AchievementProgress progress, Condition condition, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent)) {
            return false;
        }

        Player player = forceItemPlayer.player();
        Inventory inventory = player.getInventory();

        // Check only the main inventory slots (0-35), NOT armor (36-39) or offhand (40)
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
        }

        // Check backpack if enabled
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            Inventory backpack = this.forceItemBattle.getBackpack().getBackpackForPlayer(player);
            if (backpack != null) {
                for (ItemStack item : backpack.getContents()) {
                    if (item == null || item.getType() == Material.AIR) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check for beehive harvest achievement (Honey Honey)
     * Triggers when player uses shears on a beehive at honey level 5
     */
    private boolean checkBeehiveHarvestCondition(Event event, AchievementProgress progress, Condition condition) {
        if (!(event instanceof PlayerInteractEvent playerInteractEvent)) {
            return false;
        }

        // Must be right-click action
        if (playerInteractEvent.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Check if player is using shears
        ItemStack item = playerInteractEvent.getItem();
        if (item == null || item.getType() != Material.SHEARS) {
            return false;
        }

        // Check if clicked block is a beehive or bee nest
        Block clickedBlock = playerInteractEvent.getClickedBlock();
        if (clickedBlock == null) {
            return false;
        }

        // Support both BEEHIVE and BEE_NEST
        if (clickedBlock.getType() != Material.BEEHIVE && clickedBlock.getType() != Material.BEE_NEST) {
            return false;
        }

        // Check if beehive has honey level 5
        if (clickedBlock.getBlockData() instanceof org.bukkit.block.data.type.Beehive beehive) {
            if (beehive.getHoneyLevel() == beehive.getMaximumHoneyLevel()) {
                progress.incrementItemCount();
                return progress.getItemCount() >= condition.getAmount();
            }
        }

        return false;
    }

    private boolean checkAchievementCondition(Event event, AchievementProgress progress, Condition condition, UUID playerUUID) {
        if (!(event instanceof PlayerGrantAchievementEvent)) {
            return false;
        }

        // Check if player has completed all other achievements
        int totalAchievements = Achievements.values().length;
        int completedAchievements = this.achievementStorage.getPlayerAchievements(playerUUID).size();

        // -1 because we don't count the "Completionist" achievement itself
        return completedAchievements >= (totalAchievements - 1);
    }

    private boolean isItemFoundInDimension(Material itemType, String dimension) {
        return switch (dimension) {
            case "world" -> this.forceItemBattle.getItemDifficultiesManager().getOverworldItems().contains(itemType);
            case "world_nether" ->
                    this.forceItemBattle.getItemDifficultiesManager().getNetherItems().contains(itemType);
            case "world_the_end" -> this.forceItemBattle.getItemDifficultiesManager().getEndItems().contains(itemType);
            default -> false;
        };
    }

    private BiomeGroup getBiomeGroup(Biome biome) {
        for (BiomeGroup biomeGroup : BiomeGroup.values()) {
            if (biomeGroup.getBiomes().contains(biome)) {
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

        ItemMeta meta = item.getItemMeta();

        // Check display name if specified
        if (customItem.getCheckedName() != null) {
            String plainDisplayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
            if (!plainDisplayName.contains(customItem.getCheckedName())) {
                return false;
            }
        }

        // Check string-based custom model data (new format) if specified
        if (customItem.getCustomModelDataString() != null) {
            try {
                // Use Paper's data component API for string-based custom model data
                io.papermc.paper.datacomponent.item.CustomModelData cmd =
                        item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CUSTOM_MODEL_DATA);

                if (cmd == null) {
                    return false;
                }

                // Check if the custom model data contains the specified string
                java.util.List<String> strings = cmd.strings();
                if (strings == null || !strings.contains(customItem.getCustomModelDataString())) {
                    return false;
                }
            } catch (Exception e) {
                // Fallback if Paper API not available or error occurs
                return false;
            }
        }

        // Check integer custom model data (legacy format) if specified
        if (customItem.getCustomModelData() > 0) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customItem.getCustomModelData()) {
                return false;
            }
        }

        // Check material if specified
        if (customItem.getMaterial() != null) {
            if (item.getType() != customItem.getMaterial()) {
                return false;
            }
        }

        return true;
    }

    public void resetProgress() {
        this.playerProgressMap.clear();
        this.teamProgressMap.clear();
    }

    public void onNewItemReceived(UUID playerUUID) {
        PlayerProgress progress = this.playerProgressMap.get(playerUUID);
        if (progress != null) {
            for (AchievementProgress achievementProgress : progress.getAllProgress().values()) {
                achievementProgress.setItemReceivedTime(System.currentTimeMillis());
            }
        }
    }

    // Check achievements at game end
    public void checkGameEndAchievements() {
        if (!this.forceItemBattle.getGamemanager().isEndGame()) {
            return;
        }

        // Check "Chicot" achievement for all players
        for (UUID uuid : this.playerProgressMap.keySet()) {
            ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid);
            if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
                continue;
            }

            // Check if already has achievement
            if (this.achievementStorage.hasAchievement(uuid, Achievements.CHICOT)) {
                continue;
            }

            PlayerProgress playerProgress = this.playerProgressMap.get(uuid);
            AchievementProgress chicotProgress = playerProgress.getProgress(Achievements.CHICOT);

            // If death count is 0, grant achievement
            if (chicotProgress.getDeathCounter() == 0) {
                Player player = forceItemPlayer.player();
                if (player != null && player.isOnline()) {
                    Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(player, Achievements.CHICOT));
                } else {
                    // Player offline, just save it
                    this.achievementStorage.addAchievement(uuid, Achievements.CHICOT);
                }
            }
        }
    }

    public AchievementStorage getAchievementStorage() {
        return this.achievementStorage;
    }

    public PlayerProgress getPlayerProgress(UUID uuid) {
        return this.playerProgressMap.get(uuid);
    }

    public PlayerProgress getTeamProgress(Team team) {
        return this.teamProgressMap.get(team);
    }
}