package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvSettingsPresets;
import forceitembattle.util.*;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;

public class Listeners implements Listener {

    private final ForceItemBattle plugin;

    public Listeners(ForceItemBattle forceItemBattle) {
        this.plugin = forceItemBattle;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, new ArrayList<>(), null, 0, 0);
        if (this.plugin.getGamemanager().isMidGame()) {
            if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                this.plugin.getTimer().getBossBar().get(event.getPlayer().getUniqueId()).addPlayer(event.getPlayer());
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
        event.setJoinMessage("§a» §e" + player.getName() + " §ajoined");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.setQuitMessage("§c« §e" + playerQuitEvent.getPlayer().getName() + " §cragequit");
        playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
    }

    /* Found-/Skip Item */
    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if (entityPickupItemEvent.getEntity() instanceof Player player && (this.plugin.getGamemanager().isMidGame())) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack pickedItem = entityPickupItemEvent.getItem().getItemStack();
            Material currentMaterial = forceItemPlayer.getCurrentMaterial();

            if (pickedItem.getType() == currentMaterial) {
                FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                foundItemEvent.setFoundItem(pickedItem);
                foundItemEvent.setSkipped(false);

                Bukkit.getPluginManager().callEvent(foundItemEvent);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (inventoryClickEvent.getClickedInventory() instanceof CraftingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof SmithingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof FurnaceInventory ||
                inventoryClickEvent.getClickedInventory() instanceof BrewerInventory) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = inventoryClickEvent.getCurrentItem();
        Material currentItem = forceItemPlayer.getCurrentMaterial();

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
        if (this.plugin.getGamemanager().isPreGame() || this.plugin.getGamemanager().isPausedGame()
                && (playerMoveEvent.getFrom().getX() != playerMoveEvent.getTo().getX() || playerMoveEvent.getFrom().getZ() != playerMoveEvent.getTo().getZ())) {
            playerMoveEvent.setTo(playerMoveEvent.getFrom());
        }
    }


    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent foundItemEvent) {
        Player player = foundItemEvent.getPlayer();
        ItemStack itemStack = foundItemEvent.getFoundItem();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (!foundItemEvent.isBackToBack()) {
            forceItemPlayer.setCurrentScore(forceItemPlayer.getCurrentScore() + 1);
            forceItemPlayer.addFoundItemToList(new ForceItem(itemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), foundItemEvent.isSkipped()));
            forceItemPlayer.setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());
            Bukkit.broadcastMessage("§a" + player.getName() + " §7" + (foundItemEvent.isSkipped() ? "setSkipped" : "found") + " §6" + WordUtils.capitalize(itemStack.getType().name().toLowerCase().replace("_", " ")));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                forceItemPlayer.updateItemDisplay();
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.TOTAL_ITEMS, this.plugin.getStatsManager().playerStats(player.getName()), 1);
            }

        }

        ItemStack foundInventoryItemStack = null;

        if (forceItemPlayer.getPreviousMaterial() == forceItemPlayer.getCurrentMaterial()) {
            foundInventoryItemStack = new ItemStack(forceItemPlayer.getCurrentMaterial());
        }

        if (foundInventoryItemStack == null) {
            for (ItemStack inventoryItemStacks : player.getInventory().getContents()) {
                if (inventoryItemStacks == null) continue;
                if (inventoryItemStacks.getType() == forceItemPlayer.getCurrentMaterial()) {
                    foundInventoryItemStack = inventoryItemStacks;
                }

                if (inventoryItemStacks.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                    if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                        for (ItemStack shulkerItems : shulkerBox.getInventory().getContents()) {
                            if (shulkerItems == null) continue;
                            if (shulkerItems.getType() == forceItemPlayer.getCurrentMaterial())
                                foundInventoryItemStack = shulkerItems;
                        }
                    }
                }
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) && foundInventoryItemStack == null) {
                for (ItemStack backpackItemStacks : this.plugin.getBackpack().getPlayerBackpack(player).getContents()) {
                    if (backpackItemStacks == null) continue;
                    if (backpackItemStacks.getType() == forceItemPlayer.getCurrentMaterial())
                        foundInventoryItemStack = backpackItemStacks;

                    if (backpackItemStacks.getItemMeta() instanceof BlockStateMeta blockStateMeta && (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
                        for (ItemStack shulkerItems : shulkerBox.getInventory().getContents()) {
                            if (shulkerItems == null) continue;
                            if (shulkerItems.getType() == forceItemPlayer.getCurrentMaterial())
                                foundInventoryItemStack = shulkerItems;
                        }

                    }
                }
            }
        }

        if (foundInventoryItemStack != null) {
            forceItemPlayer.setCurrentScore(forceItemPlayer.getCurrentScore() + 1);
            forceItemPlayer.addFoundItemToList(new ForceItem(foundInventoryItemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), false));
            forceItemPlayer.setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
                forceItemPlayer.updateItemDisplay();
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.TOTAL_ITEMS, this.plugin.getStatsManager().playerStats(player.getName()), 1);
            }

            foundItemEvent.setFoundItem(foundInventoryItemStack);
            foundItemEvent.setBackToBack(true);
            foundItemEvent.setSkipped(false);

            Bukkit.broadcastMessage("§a" + player.getName() + " §7was lucky to already own §6" + WordUtils.capitalize(foundInventoryItemStack.getType().name().toLowerCase().replace("_", " ")));
            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) { // triggered if a joker is used
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame()) return;
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) return;
        if (e.getItem() == null) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.BUNDLE && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) {
            this.plugin.getBackpack().openPlayerBackpack(player);
            return;
        }

        if (!Gamemanager.isJoker(e.getItem())) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        int jokers = forceItemPlayer.getRemainingJokers();
        if (jokers <= 0) {
            player.sendMessage("§cNo more skips left.");
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
        Material mat = forceItemPlayer.getCurrentMaterial();

        player.getInventory().setItem(foundSlot, stack);
        player.getInventory().addItem(new ItemStack(mat));
        if (!player.getInventory().contains(mat))
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(mat));
        this.plugin.getTimer().sendActionBar();

        forceItemPlayer.setRemainingJokers(jokers);

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
        Material currentItem = forceItemPlayer.getCurrentMaterial();

        if (clickedItem == null) return;

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
        Material currentItem = forceItemPlayer.getCurrentMaterial();

        if (clickedItem == null) return;

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
        Material currentItem = forceItemPlayer.getCurrentMaterial();

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

        if (craftItemEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || craftItemEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack clickedItem = craftItemEvent.getCurrentItem();
            Material currentItem = forceItemPlayer.getCurrentMaterial();

            if (clickedItem == null) return;

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

        if (smithItemEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || smithItemEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack clickedItem = smithItemEvent.getCurrentItem();
            Material currentItem = forceItemPlayer.getCurrentMaterial();

            if (clickedItem == null) return;

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

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack movedItem = event.getCurrentItem();

        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD && (event.getHotbarButton() >= 0)) {
            movedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());

        }

        if (movedItem != null && (!event.getView().getTitle().equals("§8» §3Settings §8● §7Menu")) && (/*Gamemanager.isJoker(movedItem) || */movedItem.getType() == Material.BUNDLE)) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder) {
            inventoryBuilder.handleClick(event);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        if (InvSettingsPresets.getNamingPhase() == null) return;

        if (InvSettingsPresets.getNamingPhase().containsKey(asyncPlayerChatEvent.getPlayer().getUniqueId())) {
            asyncPlayerChatEvent.setCancelled(true);

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                GamePreset gamePreset = InvSettingsPresets.getNamingPhase().get(asyncPlayerChatEvent.getPlayer().getUniqueId());
                gamePreset.setPresetName(asyncPlayerChatEvent.getMessage());
                new InvSettingsPresets(this.plugin, gamePreset, this.plugin.getSettings()).open(asyncPlayerChatEvent.getPlayer());
                InvSettingsPresets.getNamingPhase().remove(asyncPlayerChatEvent.getPlayer().getUniqueId());
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
        Material currentItem = forceItemPlayer.getCurrentMaterial();

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onOffHand(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if (playerSwapHandItemsEvent.getMainHandItem() == null || playerSwapHandItemsEvent.getOffHandItem() == null)
            return;
        if ( //Gamemanager.isJoker(playerSwapHandItemsEvent.getMainHandItem()) ||
                playerSwapHandItemsEvent.getMainHandItem().getType() == Material.BUNDLE ||
                        // Gamemanager.isJoker(playerSwapHandItemsEvent.getOffHandItem()) ||
                        playerSwapHandItemsEvent.getOffHandItem().getType() == Material.BUNDLE)
            playerSwapHandItemsEvent.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player)) {
            return;
        }

        if (inventoryCloseEvent.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder && (inventoryBuilder.handleClose(inventoryCloseEvent))) {
            Bukkit.getScheduler().runTask(this.plugin, () -> inventoryBuilder.open((Player) inventoryCloseEvent.getPlayer()));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
        }

        ForceItemPlayer gamePlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        gamePlayer.removeItemDisplay();

        // Automatically respawn getPlayer.
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
        ItemStack jokers = Gamemanager.getJokers(forceItemPlayer.getRemainingJokers());
        if (forceItemPlayer.getRemainingJokers() > 0) {
            // This would work, but players can also move getJokers into different
            // containers like chests, that should not matter though, as they
            // can't use more than was set.
            addJokersIfMissing(player, jokers);
        }

        player.getInventory().setItem(8, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» §eBackpack").getItemStack());

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.NETHER)) {
            forceItemPlayer.createItemDisplay();
        }
    }

    private void addJokersIfMissing(Player player, ItemStack jokers) {
        int slot = player.getInventory().first(Gamemanager.getJokerMaterial());

        if (slot != -1) {
            // Already has the getJokers in their inventory.
            return;
        }

        Inventory backpack = plugin.getBackpack().getPlayerBackpack(player);
        int backpackSlot = backpack == null ? -1 : backpack.first(Gamemanager.getJokerMaterial());

        if (backpackSlot != -1) {
            // Already has the getJokers in their backpack.
            return;
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(jokers);
        } else if (backpack != null && backpack.firstEmpty() != -1) {
            backpack.addItem(jokers);
        } else {
            player.sendMessage("§cYou have no space in your inventory for getJokers! §fMake some space and uhmmmm die))");
            // TODO : handle this somehow yes?
        }
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractAtEntityEvent playerInteractAtEntityEvent) {
        if (playerInteractAtEntityEvent.getRightClicked() instanceof ArmorStand armorStand && (armorStand.isInvisible())) {
            playerInteractAtEntityEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent entityPickupItemEvent) {
        if (this.plugin.getGamemanager().isMidGame()) return;
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
        if (!this.plugin.getGamemanager().isMidGame()) return;
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.FOOD)) return;
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
            player.sendMessage("§cTravelling to other dimensions is disabled!");
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }
}
