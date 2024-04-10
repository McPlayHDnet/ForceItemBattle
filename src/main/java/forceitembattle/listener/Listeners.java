package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvSettingsPresets;
import forceitembattle.util.*;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.Color;
import java.util.ArrayList;

public class Listeners implements Listener {

    public ForceItemBattle plugin;

    public Listeners(ForceItemBattle forceItemBattle) {
        this.plugin = forceItemBattle;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, new ArrayList<>(), null, 0, 0);
        if (this.plugin.getGamemanager().isMidGame()) {
            if(!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
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

        }
        event.joinMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>» <yellow>" + player.getName() + " <green>joined"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.quitMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>« <yellow>" + playerQuitEvent.getPlayer().getName() + " <red>ragequit"));
        playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
    }

    /* Found-/Skip Item */
    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if(entityPickupItemEvent.getEntity() instanceof Player player) {
            if(this.plugin.getGamemanager().isMidGame()) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                ItemStack pickedItem = entityPickupItemEvent.getItem().getItemStack();
                Material currentMaterial = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

                if(pickedItem.getType() == currentMaterial) {
                    FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                    foundItemEvent.setFoundItem(pickedItem);
                    foundItemEvent.setSkipped(false);

                    Bukkit.getPluginManager().callEvent(foundItemEvent);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if(inventoryClickEvent.getClickedInventory() instanceof CraftingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof SmithingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof FurnaceInventory ||
                inventoryClickEvent.getClickedInventory() instanceof BrewerInventory) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = inventoryClickEvent.getCurrentItem();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if (clickedItem == null) {
            return;
        }

        if (inventoryClickEvent.getView().getTitle().startsWith("§8●")) {
            return; //prevents from getting the needed item onClick inside the recipe
        }

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        if(this.plugin.getGamemanager().isPreGame() || this.plugin.getGamemanager().isPausedGame()) {
            Location from = playerMoveEvent.getFrom();
            Location to = playerMoveEvent.getTo();

            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {

                double newX = from.getBlockX() + 0.5;
                double newZ = from.getBlockZ() + 0.5;
                double newYaw = playerMoveEvent.getPlayer().getLocation().getYaw();

                Location newLocation = new Location(from.getWorld(), newX, from.getY(), newZ, (float) newYaw, from.getPitch());
                playerMoveEvent.setTo(newLocation);
            }
        }
    }


    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getFoundItem();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        /**
         * this specific colorcode is inside the resource pack - credits: https://github.com/PuckiSilver/NoShadow
         * new Color(78, 92, 36) + unicode;
         */

        if (!event.isBackToBack()) {
            Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<green>" + player.getName() + " <gray>" + (event.isSkipped() ? "skipped" : "found") + " <reset>" + this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType()) + " <gold>" + "<lang:" + itemStack.translationKey() + ">"));
        }

        if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            forceItemPlayer.currentTeam().setCurrentScore(forceItemPlayer.currentTeam().getCurrentScore() + 1);
            forceItemPlayer.currentTeam().addFoundItemToList(new ForceItem(itemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), event.isSkipped()));
            forceItemPlayer.currentTeam().setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());

            forceItemPlayer.currentTeam().getPlayers().forEach(players -> players.player().playSound(players.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1));

            boolean foundNextItem = false;

            if (forceItemPlayer.currentTeam().getPreviousMaterial() == forceItemPlayer.currentTeam().getCurrentMaterial()) {
                foundNextItem = true;

            } else if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) &&
                    hasItemInInventory(this.plugin.getBackpack().getTeamBackpack(forceItemPlayer.currentTeam()), forceItemPlayer.currentTeam().getCurrentMaterial())) {
                foundNextItem = true;

            } else {
                for(ForceItemPlayer teamPlayers : forceItemPlayer.currentTeam().getPlayers()) {
                    if(this.hasItemInInventory(teamPlayers.player().getInventory(), forceItemPlayer.currentTeam().getCurrentMaterial())) {
                        foundNextItem = true;
                    }
                }
            }

            if (!foundNextItem) {
                return;
            }

        } else {
            forceItemPlayer.setCurrentScore(forceItemPlayer.currentScore() + 1);
            forceItemPlayer.addFoundItemToList(new ForceItem(itemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), event.isSkipped()));
            forceItemPlayer.setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                forceItemPlayer.updateItemDisplay();
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.TOTAL_ITEMS, this.plugin.getStatsManager().playerStats(player.getName()), 1);
            }

            boolean foundNextItem = false;

            if (forceItemPlayer.previousMaterial() == forceItemPlayer.currentMaterial()) {
                foundNextItem = true;

            } else if (hasItemInInventory(player.getInventory(), forceItemPlayer.currentMaterial())) {
                foundNextItem = true;

            } else if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) &&
                    hasItemInInventory(this.plugin.getBackpack().getPlayerBackpack(player), forceItemPlayer.currentMaterial())) {
                foundNextItem = true;

            }

            if (!foundNextItem) {
                return;
            }
        }

        // Handle finding item back to back

        ItemStack foundItem = new ItemStack((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial()));

        // forceItemPlayer.setCurrentScore(forceItemPlayer.currentScore() + 1);
        // forceItemPlayer.addFoundItemToList(new ForceItem(foundItem.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), false));
        // forceItemPlayer.setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());
        // player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
        // if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
        //     forceItemPlayer.updateItemDisplay();
        // }
        // if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
        //     this.plugin.getStatsManager().addToStats(PlayerStat.TOTAL_ITEMS, this.plugin.getStatsManager().playerStats(player.getName()), 1);
        // }

        FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
        foundNextItemEvent.setFoundItem(foundItem);
        foundNextItemEvent.setBackToBack(true);
        foundNextItemEvent.setSkipped(false);

        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>" + player.getName() + " <gray>was lucky to already own <reset>" + this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType()) + " <gold>" + "<lang:" + foundItem.translationKey() + ">"));
        Bukkit.getPluginManager().callEvent(foundNextItemEvent);
    }

    private boolean hasItemInInventory(Inventory inventory, Material targetMaterial) {
        for (ItemStack inventoryItem : inventory.getContents()) {
            if (inventoryItem == null) {
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

    @EventHandler
    public void onClick(PlayerInteractEvent e) { // triggered if a joker is used
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame()) return;
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) return;
        if(e.getItem() == null) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.BUNDLE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    this.plugin.getBackpack().openTeamBackpack(forceItemPlayer.currentTeam(), player);
                } else {
                    this.plugin.getBackpack().openPlayerBackpack(player);
                }
                return;
            }
        }

        if (!Gamemanager.isJoker(e.getItem())) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        int jokers = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getRemainingJokers() : forceItemPlayer.remainingJokers());
        if (jokers <= 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>No more skips left."));
            player.getInventory().remove(Gamemanager.getJokerMaterial());
            return;
        }

        jokers--;

        int foundSlot = e.getPlayer()
                .getInventory()
                .first(Gamemanager.getJokerMaterial());

        ItemStack stack = player.getInventory().getItem(foundSlot);
        assert stack != null;
        if (stack.getAmount() > 1) {
            stack.setAmount(jokers);
        } else {
            stack.setType(Material.AIR);
        }
        Material mat = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        player.getInventory().setItem(foundSlot, stack);

        player.getInventory().addItem(new ItemStack(mat));
        if (!player.getInventory().contains(mat)) player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(mat));
        this.plugin.getTimer().sendActionBar();

        if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            forceItemPlayer.currentTeam().setRemainingJokers(jokers);
        } else {
            forceItemPlayer.setRemainingJokers(jokers);
        }

        FoundItemEvent foundItemEvent = new FoundItemEvent(player);
        foundItemEvent.setFoundItem(new ItemStack(mat));
        foundItemEvent.setSkipped(true);

        Bukkit.getPluginManager().callEvent(foundItemEvent);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent playerBucketEmptyEvent) {
        Player player = playerBucketEmptyEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = playerBucketEmptyEvent.getItemStack();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if(clickedItem == null) return;

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent playerBucketFillEvent) {
        Player player = playerBucketFillEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = playerBucketFillEvent.getItemStack();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if(clickedItem == null) return;

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onBucketEntity(PlayerBucketEntityEvent playerBucketEntityEvent) {
        Player player = playerBucketEntityEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = playerBucketEntityEvent.getEntityBucket();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onCrafting(CraftItemEvent craftItemEvent) {
        Player player = (Player) craftItemEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if(craftItemEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || craftItemEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack clickedItem = craftItemEvent.getCurrentItem();
            Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

            if(clickedItem == null) return;

            if (clickedItem.getType() == currentItem) {
                FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                foundItemEvent.setFoundItem(clickedItem);
                foundItemEvent.setSkipped(false);

                Bukkit.getPluginManager().callEvent(foundItemEvent);
            }
        }


    }

    @EventHandler
    public void onSmith(SmithItemEvent smithItemEvent) {
        Player player = (Player) smithItemEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if(smithItemEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || smithItemEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack clickedItem = smithItemEvent.getCurrentItem();
            Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

            if(clickedItem == null) return;

            if (clickedItem.getType() == currentItem) {
                FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                foundItemEvent.setFoundItem(clickedItem);
                foundItemEvent.setSkipped(false);

                Bukkit.getPluginManager().callEvent(foundItemEvent);
            }
        }


    }

    /* Click-Event for my inventory builder */
    @EventHandler
    public void onInventoyClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack movedItem = event.getCurrentItem();

        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            if (event.getHotbarButton() >= 0) {
                movedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            }
        }

        if (movedItem != null) {
            if(!event.getView().getTitle().equals("§8» §3Settings §8● §7Menu")) {
                if (/*Gamemanager.isJoker(movedItem) || */movedItem.getType() == Material.BUNDLE) {
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
        if(InvSettingsPresets.namingPhase == null) return;

        if(InvSettingsPresets.namingPhase.containsKey(asyncPlayerChatEvent.getPlayer().getUniqueId())) {
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
    public void onConsume(PlayerItemConsumeEvent playerItemConsumeEvent) {
        Player player = playerItemConsumeEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = playerItemConsumeEvent.getItem();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onOffHand(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if(playerSwapHandItemsEvent.getMainHandItem() == null || playerSwapHandItemsEvent.getOffHandItem() == null) return;
        if( //Gamemanager.isJoker(playerSwapHandItemsEvent.getMainHandItem()) ||
                playerSwapHandItemsEvent.getMainHandItem().getType() == Material.BUNDLE ||
                // Gamemanager.isJoker(playerSwapHandItemsEvent.getOffHandItem()) ||
                playerSwapHandItemsEvent.getOffHandItem().getType() == Material.BUNDLE)
            playerSwapHandItemsEvent.setCancelled(true);
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
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
            event.getDrops().removeIf(Gamemanager::isBackpack);
        }

        ForceItemPlayer gamePlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        gamePlayer.removeItemDisplay();

        // Automatically respawn player.
        Bukkit.getScheduler().runTaskLater(
                this.plugin,
                () -> event.getEntity().spigot().respawn(),
                1
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        Player player = playerRespawnEvent.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack jokers = Gamemanager.getJokers((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getRemainingJokers() : forceItemPlayer.remainingJokers()));
        if ((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getRemainingJokers() : forceItemPlayer.remainingJokers()) > 0) {
            // This would work, but players can also move jokers into different
            // containers like chests, that should not matter though, as they
            // can't use more than was set.
            addJokersIfMissing(player, jokers);
        }

        player.getInventory().setItem(8, new ItemBuilder(Material.BUNDLE).setDisplayName("<dark_gray>» <yellow>Backpack").getItemStack());

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
            forceItemPlayer.createItemDisplay();
        }
    }

    private void addJokersIfMissing(Player player, ItemStack jokers) {
        int slot = player.getInventory().first(Gamemanager.getJokerMaterial());

        if (slot != -1) {
            // Already has the jokers in their inventory.
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Inventory backpack = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? plugin.getBackpack().getTeamBackpack(forceItemPlayer.currentTeam()) : plugin.getBackpack().getPlayerBackpack(player));
        int backpackSlot = backpack == null? -1 : backpack.first(Gamemanager.getJokerMaterial());

        if (backpackSlot != -1) {
            // Already has the jokers in their backpack.
            return;
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(jokers);
        } else if (backpack != null && backpack.firstEmpty() != -1) {
            backpack.addItem(jokers);
        } else {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You have no space in your inventory for jokers! <white>Make some space and uhmmmm die))"));
            // TODO : handle this somehow yes?
        }
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractAtEntityEvent playerInteractAtEntityEvent) {
        if(playerInteractAtEntityEvent.getRightClicked() instanceof ArmorStand armorStand) {
            if(armorStand.isInvisible()) playerInteractAtEntityEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent entityPickupItemEvent) {
        if(this.plugin.getGamemanager().isMidGame()) return;
        entityPickupItemEvent.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Gamemanager.isJoker(event.getItemDrop().getItemStack())
                || event.getItemDrop().getItemStack().getType() == Material.BUNDLE) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.FOOD)) return;
        if(!this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Gamemanager.isJoker(event.getBlock().getType())) {
            event.setCancelled(true);
        }
        if (this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (this.plugin.getGamemanager().isMidGame()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) return;
        if (event.getTarget() == null) return;
        if (event.getTarget().getType() != EntityType.PLAYER) return;
        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPortalEvent(PlayerPortalEvent playerPortalEvent) {
        Player player = playerPortalEvent.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Travelling to other dimensions is disabled!"));
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }
}
