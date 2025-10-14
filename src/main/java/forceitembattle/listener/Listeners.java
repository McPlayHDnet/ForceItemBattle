package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.stats.SeasonalStats;
import forceitembattle.manager.stats.StatsManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvSettingsPresets;
import forceitembattle.util.ForceItem;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import forceitembattle.util.PlayerStat;
import forceitembattle.util.Team;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.Objects;

@RequiredArgsConstructor
public class Listeners implements Listener {

    public final ForceItemBattle plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, null, 0, 0);
        if (this.plugin.getGamemanager().isMidGame() || this.plugin.getGamemanager().isPausedGame()) {
            if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.setGameMode(GameMode.SPECTATOR);

                /** todo
                this.plugin.getGamemanager().giveSpectatorItems(player);

                this.plugin.getGamemanager().forceItemPlayerMap().values().forEach(gamePlayers -> {
                    gamePlayers.player().hidePlayer(this.plugin, player);
                });
                **/
            } else {
                forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                forceItemPlayer.setPlayer(player);
                player.showBossBar(this.plugin.getTimer().getBossBar().get(event.getPlayer().getUniqueId()));
            }
        } else {

            this.plugin.getGamemanager().addPlayer(player, forceItemPlayer);

            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setGameMode(GameMode.ADVENTURE);

            player.getInventory().setItem(4, new ItemBuilder(Material.LIME_DYE).setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
            player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_PEARL).setDisplayName("<dark_gray>» <gray>Spectate game").getItemStack());

        }
        player.sendPlayerListHeader(this.plugin.getGamemanager().getMiniMessage().deserialize("<!shadow>\n\n\n\uebA0\n"));
        event.joinMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>» <yellow>" + player.getName() + " <green>joined"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.quitMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>« <yellow>" + playerQuitEvent.getPlayer().getName() + " <red>ragequit"));

        if(this.plugin.getGamemanager().isPreGame() || this.plugin.getGamemanager().isEndGame()) {
            this.plugin.getGamemanager().removePlayer(playerQuitEvent.getPlayer());
        }

        if(this.plugin.getGamemanager().isMidGame()) {
            playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();
        if (this.plugin.getGamemanager().isPausedGame()) {
            Location from = playerMoveEvent.getFrom();
            Location to = playerMoveEvent.getTo();

            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {

                double newX = from.getBlockX() + 0.5;
                double newZ = from.getBlockZ() + 0.5;
                double newYaw = playerMoveEvent.getPlayer().getLocation().getYaw();

                Location newLocation = new Location(from.getWorld(), newX, from.getY(), newZ, (float) newYaw, from.getPitch());
                playerMoveEvent.setTo(newLocation);
            }
            return;
        }
    }

    @EventHandler
    public void onTrade(PlayerTradeEvent playerTradeEvent) {
        Player player = playerTradeEvent.getPlayer();
        if (playerTradeEvent.getVillager() instanceof WanderingTrader wanderingTrader) {
            if (playerTradeEvent.getTrade().getResult().getType() != Material.NETHER_STAR) return;

            Boolean canBuy = this.plugin.getWanderingTraderTimer().getCanBuyWheel().get(player.getUniqueId());
            if (canBuy == null || canBuy) {
                this.plugin.getWanderingTraderTimer().getCanBuyWheel().put(player.getUniqueId(), Boolean.FALSE);
                player.closeInventory();
            } else {
                playerTradeEvent.setCancelled(true);
            }
        }
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

        boolean shouldApplyScoreAndSound = !context.isRunMode() || !event.isSkipped();

        if (shouldApplyScoreAndSound) {
            applyScoreAndSound(forceItemPlayer, itemStack, event, context);
        }

        updateMaterials(forceItemPlayer, event, context);
        updateStats(forceItemPlayer, player, context, event.isBackToBack());
        handleBackToBackCheck(forceItemPlayer, player, context);
    }

    private void handleRegularFind(FoundItemEvent event, Player player, ItemStack itemStack, ForceItemPlayer forceItemPlayer, GameContext context) {
        String action = event.isSkipped() ? "skipped" : "found";
        String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType());
        String materialName = plugin.getGamemanager().getMaterialName(itemStack.getType());

        Component message = plugin.getGamemanager().getMiniMessage().deserialize(
                String.format("<green>%s <gray>%s <reset><shadow:black:0.4>%s</shadow> <gold>%s",
                        player.getName(), action, unicode, materialName)
        );

        broadcastMessage(message, forceItemPlayer, context);
        updateBackToBackStats(forceItemPlayer, player, context);
    }

    private void broadcastMessage(Component message, ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.isEventDisabled()) {
            Bukkit.broadcast(message);
        } else if (context.isTeamGame()) {
            forceItemPlayer.currentTeam().getPlayers().forEach(p -> p.player().sendMessage(message));
        } else {
            forceItemPlayer.player().sendMessage(message);
        }
    }

    private void updateBackToBackStats(ForceItemPlayer forceItemPlayer, Player player, GameContext context) {
        if (!context.isStatsEnabled() || context.isRunMode()) {
            return;
        }

        int backToBacks = forceItemPlayer.backToBackStreak();
        if (backToBacks == 0) {
            return;
        }

        ForceItemPlayerStats playerStats = plugin.getStatsManager().loadPlayerStats(player.getName());
        SeasonalStats seasonalStats = playerStats.getSeasonStats(StatsManager.CURRENT_SEASON);

        if (seasonalStats.getBack2backStreak().getSolo() >= backToBacks) {
            return;
        }

        if (context.isTeamGame()) {
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> plugin.getStatsManager().updateTeamStats(
                            player.getName(),
                            teammate.player().getName(),
                            backToBacks,
                            PlayerStat.BACK_TO_BACK_STREAK
                    ));
        } else {
            plugin.getStatsManager().updateSoloStats(
                    player.getName(),
                    PlayerStat.BACK_TO_BACK_STREAK,
                    backToBacks
            );
        }
    }

    private void applyScoreAndSound(ForceItemPlayer forceItemPlayer, ItemStack itemStack,
                                    FoundItemEvent event, GameContext context) {
        ForceItem forceItem = new ForceItem(
                itemStack.getType(),
                plugin.getTimer().formatSeconds(plugin.getTimer().getTimeLeft()),
                System.currentTimeMillis(),
                event.isBackToBack(),
                event.isSkipped()
        );

        if (context.isTeamGame()) {
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

    private void updateMaterials(ForceItemPlayer forceItemPlayer, FoundItemEvent event, GameContext context) {
        if (context.isRunMode()) {
            updateSeededMaterials(forceItemPlayer, context);
        } else {
            updateRandomMaterials(forceItemPlayer, context);
        }
    }

    private void updateSeededMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        Material currentMaterial = plugin.getGamemanager().generateSeededMaterial();

        if (context.isTeamGame()) {
            plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                Team team = p.currentTeam();
                team.setPreviousMaterial(team.getCurrentMaterial());
                team.setCurrentMaterial(team.getNextMaterial());
                team.setNextMaterial(currentMaterial);
            });
        } else {
            plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                p.setPreviousMaterial(p.currentMaterial());
                p.setCurrentMaterial(p.getNextMaterial());
                p.setNextMaterial(currentMaterial);
            });
        }
    }

    private void updateRandomMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        Material nextMaterial = plugin.getGamemanager().generateMaterial();

        if (context.isTeamGame()) {
            Team team = forceItemPlayer.currentTeam();
            Material currentMaterial = team.getNextMaterial();
            team.setPreviousMaterial(team.getCurrentMaterial());
            team.setCurrentMaterial(currentMaterial);
            team.setNextMaterial(nextMaterial);
        } else {
            Material currentMaterial = forceItemPlayer.getNextMaterial();
            forceItemPlayer.setPreviousMaterial(forceItemPlayer.currentMaterial());
            forceItemPlayer.setCurrentMaterial(currentMaterial);
            forceItemPlayer.setNextMaterial(nextMaterial);
        }
    }

    private void updateStats(ForceItemPlayer forceItemPlayer, Player player,
                             GameContext context, boolean isBackToBack) {
        if (!context.isStatsEnabled() || context.isRunMode() || isBackToBack) {
            return;
        }

        if (context.isTeamGame()) {
            forceItemPlayer.currentTeam().getPlayers().stream()
                    .filter(teammate -> !teammate.equals(forceItemPlayer))
                    .forEach(teammate -> plugin.getStatsManager().updateTeamStats(
                            player.getName(),
                            teammate.player().getName(),
                            1,
                            PlayerStat.TOTAL_ITEMS
                    ));
        } else {
            plugin.getStatsManager().updateSoloStats(player.getName(), PlayerStat.TOTAL_ITEMS, 1);
        }
    }

    private void handleBackToBackCheck(ForceItemPlayer forceItemPlayer, Player player, GameContext context) {
        if (context.isRunMode()) {
            return;
        }

        Material currentMaterial = context.isTeamGame()
                ? forceItemPlayer.currentTeam().getCurrentMaterial()
                : forceItemPlayer.getCurrentMaterial();

        BackToBackResult result = checkForBackToBack(forceItemPlayer, currentMaterial, context);

        if (result.hasBackToBack()) {
            forceItemPlayer.setBackToBackStreak(forceItemPlayer.backToBackStreak() + 1);

            if (result.getTeammateWhoHasIt() != null) {
                ForceItemPlayer teammate = result.getTeammateWhoHasIt();
                teammate.setBackToBackStreak(teammate.backToBackStreak() + 1);
            }

            if (context.isTeamGame() && forceItemPlayer.currentTeam() != null) {
                Team team = forceItemPlayer.currentTeam();
                team.setBackToBackStreak(team.getBackToBackStreak() + 1);
            }

            triggerBackToBackEvent(forceItemPlayer, player, result, context);
        } else {
            forceItemPlayer.setBackToBackStreak(0);

            if (context.isTeamGame() && forceItemPlayer.currentTeam() != null) {
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

            String probability = getItemProbability(player, forceItemPlayer);
            String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType());
            String materialName = plugin.getGamemanager().getMaterialName(foundItem.getType());

            Component message;
            if (result.getTeammateWhoHasIt() != null) {
                // Teammate has the item
                ForceItemPlayer teammate = result.getTeammateWhoHasIt();
                message = plugin.getGamemanager().getMiniMessage().deserialize(
                        String.format("<green>%s <gray>was lucky that <green>%s <gray>already owns <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                                player.getName(), teammate.player().getName(), unicode, materialName, probability)
                );
            } else {
                // Player themselves has the item
                message = plugin.getGamemanager().getMiniMessage().deserialize(
                        String.format("<green>%s <gray>was lucky to already own <reset>%s <gold>%s <dark_gray>» <aqua>%s",
                                player.getName(), unicode, materialName, probability)
                );
            }

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

    private BackToBackResult checkForBackToBack(ForceItemPlayer player, Material targetMaterial, GameContext context) {
        Material previousMaterial = context.isTeamGame()
                ? player.currentTeam().getPreviousMaterial()
                : player.previousMaterial();

        if (previousMaterial == targetMaterial) {
            return new BackToBackResult(true, null);
        }

        if (hasItemInInventory(player.player().getInventory(), targetMaterial)) {
            return new BackToBackResult(true, null);
        }

        if (context.isBackpackEnabled()) {
            Inventory backpackInventory = context.isTeamGame()
                    ? plugin.getBackpack().getTeamBackpack(player.currentTeam())
                    : plugin.getBackpack().getPlayerBackpack(player.player());

            if (hasItemInInventory(backpackInventory, targetMaterial)) {
                return new BackToBackResult(true, null);
            }
        }

        if (context.isTeamGame() && player.currentTeam() != null) {
            for (ForceItemPlayer teammate : player.currentTeam().getPlayers()) {
                if (teammate.equals(player)) {
                    continue;
                }

                if (hasItemInInventory(teammate.player().getInventory(), targetMaterial)) {
                    return new BackToBackResult(true, teammate);
                }

                if (context.isBackpackEnabled()) {
                    Inventory teammateBackpack = plugin.getBackpack().getPlayerBackpack(teammate.player());
                    if (hasItemInInventory(teammateBackpack, targetMaterial)) {
                        return new BackToBackResult(true, teammate);
                    }
                }
            }
        }

        return new BackToBackResult(false, null);
    }

    private String getItemProbability(Player player, ForceItemPlayer forceItemPlayer) {
        int totalItemsInPool = plugin.getItemDifficultiesManager().getAvailableItems().size();
        boolean isBackpackEnabled = plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK);
        boolean isTeamGame = forceItemPlayer.currentTeam() != null;

        int totalItems = 0;
        int streak = forceItemPlayer.backToBackStreak();

        if (isTeamGame) {
            Team team = forceItemPlayer.currentTeam();

            for (ForceItemPlayer teammate : team.getPlayers()) {
                totalItems += countItemsInInventory(teammate.player().getInventory());
            }

            if (isBackpackEnabled) {
                Inventory teamBackpack = plugin.getBackpack().getTeamBackpack(team);
                totalItems += countItemsInInventory(teamBackpack);
            }

            streak = Math.max(streak, team.getBackToBackStreak());
        } else {
            totalItems = countItemsInInventory(player.getInventory());

            if (isBackpackEnabled) {
                Inventory backpack = plugin.getBackpack().getPlayerBackpack(player);
                totalItems += countItemsInInventory(backpack);
            }
        }

        double baseProbability = (double) totalItems / totalItemsInPool;

        Material prev = forceItemPlayer.getPreviousMaterial();
        Material current = forceItemPlayer.getCurrentMaterial();

        if (prev != null && current == prev) {
            baseProbability *= 0.05;
            streak += 1;
        }

        double probability = Math.pow(baseProbability, streak);

        String rarity;
        if (probability <= 0.001) {
            rarity = "<gradient:#FF00DD:#9905E3><b>RNGESUS</b></gradient>"; // ~0.1% or less
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1f);
        } else if (probability <= 0.01) {
            rarity = "<gold><b>LEGENDARY</b></gold>";
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f);
        } else {
            rarity = "<dark_purple><b>EPIC</b></dark_purple>";
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
        }

        DecimalFormat percentFormat = new DecimalFormat("#.##");
        String formattedProbability = percentFormat.format(probability * 100) + "%";

        return formattedProbability + " <dark_gray>(<reset>" + rarity + "<dark_gray>)";
    }

    private int countItemsInInventory(Inventory inventory) {
        if (inventory == null) {
            return 0;
        }

        int count = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || Gamemanager.isJoker(item) || Gamemanager.isBackpack(item)) {
                continue;
            }

            count++;

            if (Tag.SHULKER_BOXES.isTagged(item.getType())) {
                count += countItemsInShulkerBox(item);
            }

            if (Tag.ITEMS_BUNDLES.isTagged(item.getType())) {
                count += countItemsInBundles(item);
            }
        }

        return count;
    }

    private int countItemsInShulkerBox(ItemStack shulkerBox) {
        ItemMeta meta = shulkerBox.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return 0;
        }

        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox box)) {
            return 0;
        }

        int count = 0;
        for (ItemStack item : box.getInventory().getContents()) {
            if (item == null || Gamemanager.isJoker(item) || Gamemanager.isBackpack(item)) {
                continue;
            }

            count++;

            if (Tag.ITEMS_BUNDLES.isTagged(item.getType())) {
                count += countItemsInBundles(item);
            }
        }

        return count;
    }

    private int countItemsInBundles(ItemStack bundle) {
        ItemMeta meta = bundle.getItemMeta();
        if (!(meta instanceof BundleMeta bundleMeta)) {
            return 0;
        }

        if (!bundleMeta.hasItems()) {
            return 0;
        }

        return (int) bundleMeta.getItems().stream()
                .filter(item -> item != null && !Gamemanager.isJoker(item))
                .count();
    }

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

    /* Click-Event for my inventory builder */
    @EventHandler
    public void onInventoyClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack movedItem = event.getCurrentItem();

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (event.getHotbarButton() >= 0) {
                movedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            }
        }

        if (movedItem != null) {
            if (!event.getView().getTitle().equals("§8» §3Settings §8● §7Menu")) {
                if (Gamemanager.isBackpack(movedItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }


        if (event.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder) {
            inventoryBuilder.handleClick(event);
            return;
        }

    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        if (InvSettingsPresets.namingPhase == null) {
            return;
        }

        if (InvSettingsPresets.namingPhase.containsKey(asyncPlayerChatEvent.getPlayer().getUniqueId())) {
            asyncPlayerChatEvent.setCancelled(true);

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                GamePreset gamePreset = InvSettingsPresets.namingPhase.get(asyncPlayerChatEvent.getPlayer().getUniqueId());
                gamePreset.setPresetName(asyncPlayerChatEvent.getMessage());
                new InvSettingsPresets(this.plugin, gamePreset, this.plugin.getSettings()).open(asyncPlayerChatEvent.getPlayer());
                InvSettingsPresets.namingPhase.remove(asyncPlayerChatEvent.getPlayer().getUniqueId());
            });
        }
    }

    @EventHandler
    public void onChat_(AsyncChatEvent event) {
        Player player = event.getPlayer();

        event.setCancelled(true);
        Team currentTeam = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId()).currentTeam();

        if (
                !this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ||
                !this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM_CHAT) ||
                currentTeam == null
        ) {
            Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>" + player.getName() + " <dark_gray>» <white>" + PlainTextComponentSerializer.plainText().serialize(event.originalMessage())));
            return;
        }

        String message = "<green>Team</green> <gray>| <gold>" + player.getName() + " <dark_gray>» <white>" + PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        currentTeam.getPlayers().forEach(fibPlayer -> {
            Player p = fibPlayer.player();
            if (p == null || !p.isOnline()) {
                return;
            }

            p.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(message));
        });
    }

    @EventHandler
    public void onOffHand(PlayerSwapHandItemsEvent event) {
        if (Gamemanager.isBackpack(event.getMainHandItem()) ||
                Gamemanager.isBackpack(event.getOffHandItem())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player player)) {
            return;
        }

        if (inventoryCloseEvent.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder) {

            if (inventoryBuilder.handleClose(inventoryCloseEvent)) {
                Bukkit.getScheduler().runTask(this.plugin, () -> inventoryBuilder.open((Player) inventoryCloseEvent.getPlayer()));
                return;
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ForceItemPlayer gamePlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        String plainDeathMessage = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(event.deathMessage()));
        String plainPlayerName = PlainTextComponentSerializer.plainText().serialize(player.name());

        event.deathMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<red>\uD83D\uDC80<dark_gray>] " + plainDeathMessage.replace(plainPlayerName, "<gold>" + player.getName() + "<gray>")));
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
            event.getDrops().removeIf(Gamemanager::isBackpack);
        }

        // Automatically respawn player.
        Bukkit.getScheduler().runTaskLater(
                this.plugin,
                () -> event.getEntity().spigot().respawn(),
                1
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (keepInventory == null || !keepInventory) {
            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

            player.performCommand("fixskips -silent");
        }

        player.getInventory().setItem(8, Gamemanager.createBackpack());

    }

    @EventHandler
    public void onAchievementGrant(PlayerGrantAchievementEvent playerGrantAchievementEvent) {
        Player player = playerGrantAchievementEvent.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Achievements achievement = playerGrantAchievementEvent.getAchievement();

        if(forceItemPlayer.isSpectator()) return;

        forceItemPlayer.player().playSound(forceItemPlayer.player(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 1);
        Bukkit.getOnlinePlayers().forEach(players -> {
            players.sendMessage(Component.empty());
            players.sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>[<yellow>❋<dark_gray>] <gold>" + forceItemPlayer.player().getName() + " <gray>has made the achievement <hover:show_text:'<dark_aqua>" + achievement.getTitle() + "<newline><gray>" + achievement.getDescription() + "'><dark_aqua>[" + achievement.getTitle() + "]</hover>"));
            players.sendMessage(Component.empty());
        });

        //ForceItemBattle.getInstance().getAchievementManager().grantAchievement(ForceItemBattle.getInstance().getStatsManager().playerStats(forceItemPlayer.player().getName()), achievement);
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand armorStand && armorStand.isInvisible()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent entityPickupItemEvent) {
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        entityPickupItemEvent.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Gamemanager.isJoker(event.getItemDrop().getItemStack())
                || Gamemanager.isBackpack(event.getItemDrop().getItemStack())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.FOOD)) {
            return;
        }
        if (!this.plugin.getGamemanager().isPreGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        if (event.getTarget() == null) {
            return;
        }
        if (event.getTarget().getType() != EntityType.PLAYER) {
            return;
        }
        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPortalEvent(PlayerPortalEvent playerPortalEvent) {
        Player player = playerPortalEvent.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Travelling to other dimensions is disabled!"));
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT)) {
            event.message(null);
            return;
        }

        Advancement advancement = event.getAdvancement();

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(event.getPlayer().getUniqueId());
        if(forceItemPlayer.isSpectator()) return;

        if (advancement.key().namespace().equals("fib")) {
            String plainAdvancement = PlainTextComponentSerializer.plainText().serialize(advancement.displayName());
            String plainAdvancementDescription = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(advancement.getDisplay()).description());

            String advancementType = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "has completed the challenge" : "has made the advancement";
            String advancementTypeColor = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "<dark_purple>" : "<green>";

            event.message(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<yellow>⭐<dark_gray>] <gold>" + event.getPlayer().getName() + " <gray>" + advancementType + " <hover:show_text:'" + advancementTypeColor + plainAdvancement + "<newline>" + advancementTypeColor + plainAdvancementDescription + "'>" + advancementTypeColor + plainAdvancement + "</hover>"));
        } else {
            event.message(null);
        }

    }

    private static class GameContext {
        private final boolean teamGame;
        private final boolean runMode;
        private final boolean eventDisabled;
        private final boolean statsEnabled;
        private final boolean backpackEnabled;

        public GameContext(boolean teamGame, boolean runMode, boolean eventDisabled,
                           boolean statsEnabled, boolean backpackEnabled) {
            this.teamGame = teamGame;
            this.runMode = runMode;
            this.eventDisabled = eventDisabled;
            this.statsEnabled = statsEnabled;
            this.backpackEnabled = backpackEnabled;
        }

        public boolean isTeamGame() { return teamGame; }
        public boolean isRunMode() { return runMode; }
        public boolean isEventDisabled() { return eventDisabled; }
        public boolean isStatsEnabled() { return statsEnabled; }
        public boolean isBackpackEnabled() { return backpackEnabled; }
    }

    private static class BackToBackResult {
        private final boolean hasBackToBack;
        @Getter
        private final ForceItemPlayer teammateWhoHasIt;

        public BackToBackResult(boolean hasBackToBack, ForceItemPlayer teammateWhoHasIt) {
            this.hasBackToBack = hasBackToBack;
            this.teammateWhoHasIt = teammateWhoHasIt;
        }

        public boolean hasBackToBack() {
            return hasBackToBack;
        }

    }
}
