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
import forceitembattle.util.*;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Listeners implements Listener {

    public final ForceItemBattle plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, new ArrayList<>(), null, 0, 0);
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
        if (this.plugin.getGamemanager().isPreGame() || this.plugin.getGamemanager().isPausedGame()) {
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
        ForceItemPlayerStats playerStats = this.plugin.getStatsManager().loadPlayerStats(player.getName());
        SeasonalStats seasonalStats = playerStats.getSeasonStats(StatsManager.CURRENT_SEASON);

        boolean isTeamGame = this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM);
        boolean isRunMode = this.plugin.getSettings().isSettingEnabled(GameSetting.RUN);
        boolean isEventDisabled = !this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT);
        boolean isStatsEnabled = this.plugin.getSettings().isSettingEnabled(GameSetting.STATS);

        int backToBacks = forceItemPlayer.backToBackStreak();

        /**
         * this specific colorcode is inside the resource pack - credits: https://github.com/PuckiSilver/NoShadow
         * new Color(78, 92, 36) + unicode;
         */

        if (!event.isBackToBack()) {
            String action = event.isSkipped() ? "skipped" : "found";
            String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType());
            String materialName = plugin.getGamemanager().getMaterialName(itemStack.getType());
            Component message = plugin.getGamemanager().getMiniMessage().deserialize(
                    "<green>" + player.getName() + " <gray>" + action + " <reset>" + unicode + " <gold>" + materialName);

            if (isEventDisabled) {
                Bukkit.broadcast(message);
            } else if (isTeamGame) {
                forceItemPlayer.currentTeam().getPlayers().forEach(p -> p.player().sendMessage(message));
            } else {
                player.sendMessage(message);
            }

            if (isStatsEnabled && backToBacks != 0 && !isRunMode) {
                if (seasonalStats.getBack2backStreak().getSolo() < backToBacks) {
                    if (isTeamGame) {
                        for (ForceItemPlayer teammate : forceItemPlayer.currentTeam().getPlayers()) {
                            if (!teammate.equals(forceItemPlayer)) {
                                plugin.getStatsManager().updateTeamStats(
                                        player.getName(), teammate.player().getName(), backToBacks, PlayerStat.BACK_TO_BACK_STREAK);
                            }
                        }
                    } else {
                        plugin.getStatsManager().updateSoloStats(player.getName(), PlayerStat.BACK_TO_BACK_STREAK, backToBacks);
                    }
                }
            }

            forceItemPlayer.setBackToBackStreak(0);
        }

        boolean shouldApplyScoreAndSound = !isRunMode || (isRunMode && !event.isSkipped());

        if (isTeamGame) {
            Team team = forceItemPlayer.currentTeam();

            if (shouldApplyScoreAndSound) {
                team.setCurrentScore(team.getCurrentScore() + 1);
                team.addFoundItemToList(new ForceItem(itemStack.getType(),
                        plugin.getTimer().formatSeconds(plugin.getTimer().getTimeLeft()),
                        System.currentTimeMillis(), event.isBackToBack(), event.isSkipped()));
                team.getPlayers().forEach(p -> p.player().playSound(p.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1));
            }

            if (isRunMode) {
                Material currentMaterial = this.plugin.getGamemanager().generateSeededMaterial();

                plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                    Team t = p.currentTeam();
                    t.setCurrentMaterial(t.getNextMaterial());
                    t.setNextMaterial(currentMaterial);
                });
                return;
            } else {
                team.setCurrentMaterial(team.getNextMaterial());
                team.setNextMaterial(plugin.getGamemanager().generateMaterial());
            }

            // Stats update
            if (isStatsEnabled) {
                for (ForceItemPlayer teammate : team.getPlayers()) {
                    if (!teammate.equals(forceItemPlayer)) {
                        plugin.getStatsManager().updateTeamStats(
                                player.getName(), teammate.player().getName(), 1, PlayerStat.TOTAL_ITEMS);
                    }
                }
            }

            if (checkForBackToBack(forceItemPlayer, team.getCurrentMaterial())) {
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);
            } else if (!isRunMode) {
                return;
            }

        } else {
            // Solo mode
            if (shouldApplyScoreAndSound) {
                forceItemPlayer.setCurrentScore(forceItemPlayer.currentScore() + 1);
                forceItemPlayer.addFoundItemToList(new ForceItem(itemStack.getType(),
                        plugin.getTimer().formatSeconds(plugin.getTimer().getTimeLeft()),
                        System.currentTimeMillis(), event.isBackToBack(), event.isSkipped()));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
            }

            if (isRunMode) {
                Material currentMaterial = this.plugin.getGamemanager().generateSeededMaterial();

                plugin.getGamemanager().forceItemPlayerMap().values().forEach(p -> {
                    p.setCurrentMaterial(p.getNextMaterial());
                    p.setNextMaterial(currentMaterial);
                });
                return;
            } else {
                forceItemPlayer.setCurrentMaterial(forceItemPlayer.getNextMaterial());
                forceItemPlayer.setNextMaterial(plugin.getGamemanager().generateMaterial());
            }

            if (isStatsEnabled) {
                plugin.getStatsManager().updateSoloStats(player.getName(), PlayerStat.TOTAL_ITEMS, 1);
            }

            if (checkForBackToBack(forceItemPlayer, forceItemPlayer.currentMaterial())) {
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);
            } else if (!isRunMode) {
                return;
            }
        }

        // Trigger back-to-back event
        ItemStack foundItem = new ItemStack(forceItemPlayer.getCurrentMaterial());
        FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
        foundNextItemEvent.setFoundItem(foundItem);
        foundNextItemEvent.setBackToBack(true);
        foundNextItemEvent.setSkipped(false);

        String probability = getItemProbability(player, forceItemPlayer);
        String unicode = plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType());
        String materialName = plugin.getGamemanager().getMaterialName(foundItem.getType());
        Component message = plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>" + player.getName() + " <gray>was lucky to already own <reset>" + unicode +
                        " <gold>" + materialName + " <dark_gray>» <aqua>" + probability);

        if (isEventDisabled) {
            Bukkit.broadcast(message);
        } else if (isTeamGame) {
            forceItemPlayer.currentTeam().getPlayers().forEach(p -> p.player().sendMessage(message));
        } else {
            player.sendMessage(message);
        }

        Bukkit.getPluginManager().callEvent(foundNextItemEvent);
    }

    private boolean checkForBackToBack(ForceItemPlayer player, Material currentMaterial) {
        if (player.previousMaterial() == currentMaterial) return true;
        if (plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            if (hasItemInInventory(plugin.getBackpack().getPlayerBackpack(player.player()), currentMaterial)) return true;
        }
        return hasItemInInventory(player.player().getInventory(), currentMaterial);
    }

    private String getItemProbability(Player player, ForceItemPlayer forceItemPlayer) {
        int totalItemsInPool = this.plugin.getItemDifficultiesManager().getAvailableItems().size();
        int itemsInInventory = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && !Gamemanager.isJoker(item) && !Gamemanager.isBackpack(item))
                .map(ItemStack::getType)
                .toList().size();
        Inventory backpack = this.plugin.getBackpack().getBackpackForPlayer(player);
        int itemsInBackpack = Arrays.stream(backpack.getContents())
                .filter(item -> item != null && !Gamemanager.isJoker(item) && !Gamemanager.isBackpack(item))
                .map(ItemStack::getType)
                .toList().size();

        int itemsInShulkers = Stream.concat(
                        Arrays.stream(player.getInventory().getContents()),
                        Arrays.stream(backpack.getContents())
                ).filter(item -> item != null && Tag.SHULKER_BOXES.isTagged(item.getType()))
                .mapToInt(this::countItemsInShulkerBox)
                .sum();
        double probabilityDouble = Math.pow(((double) (itemsInInventory + itemsInBackpack + itemsInShulkers) / totalItemsInPool), forceItemPlayer.backToBackStreak());
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        return decimalFormat.format(probabilityDouble * 100) + "%";
    }

    private int countItemsInShulkerBox(ItemStack shulkerBox) {
        BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBox.getItemMeta();
        if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox box) {
            return (int) Arrays.stream(box.getInventory().getContents())
                    .filter(item -> item != null && !Gamemanager.isJoker(item) && !Gamemanager.isBackpack(item))
                    .count();
        }
        return 0;
    }

    public static boolean hasItemInInventory(Inventory inventory, Material targetMaterial) {
        for (ItemStack inventoryItem : inventory.getContents()) {
            if (inventoryItem == null) {
                continue;
            }

            if (Gamemanager.isBackpack(inventoryItem) || Gamemanager.isJoker(inventoryItem)) {
                continue;
            }

            if (inventoryItem.getType() == targetMaterial) {
                return true;
            }

            if (!(inventoryItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
                continue;
            }

            if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
                continue;
            }

            for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                if (shulkerItem == null) {
                    continue;
                }

                if (shulkerItem.getType() == targetMaterial) {
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

}
