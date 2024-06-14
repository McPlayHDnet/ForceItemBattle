package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.AchievementInventory;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvSettingsPresets;
import forceitembattle.util.*;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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
        if (this.plugin.getGamemanager().isMidGame() || this.plugin.getGamemanager().isPausedGame()) {
            if(!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
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
        Player player = playerMoveEvent.getPlayer();
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
            return;
        }

        if(this.plugin.getGamemanager().isMidGame()) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {

                if(achievement.checkRequirements(player, playerMoveEvent)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
                }
            });
        }
    }


    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getFoundItem();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ForceItemPlayerStats playerStats = this.plugin.getStatsManager().playerStats(player.getName());

        /**
         * this specific colorcode is inside the resource pack - credits: https://github.com/PuckiSilver/NoShadow
         * new Color(78, 92, 36) + unicode;
         */

        if (!event.isBackToBack()) {
            Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<green>" + player.getName() + " <gray>" + (event.isSkipped() ? "skipped" : "found") + " <reset>" + this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, itemStack.getType()) + " <gold>" + this.plugin.getGamemanager().getMaterialName(itemStack.getType())));
            if(forceItemPlayer.backToBackStreak() != 0) {
                if(playerStats.back2backStreak() < forceItemPlayer.backToBackStreak()) {
                    this.plugin.getStatsManager().addToStats(PlayerStat.BACK_TO_BACK_STREAK, playerStats, forceItemPlayer.backToBackStreak());
                }
            }
            forceItemPlayer.setBackToBackStreak(0);
        }
        int backToBacks = forceItemPlayer.backToBackStreak();

        if(this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            forceItemPlayer.currentTeam().setCurrentScore(forceItemPlayer.currentTeam().getCurrentScore() + 1);
            forceItemPlayer.currentTeam().addFoundItemToList(new ForceItem(itemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), System.currentTimeMillis(), event.isBackToBack(), event.isSkipped()));
            forceItemPlayer.currentTeam().setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());

            forceItemPlayer.currentTeam().getPlayers().forEach(players -> players.player().playSound(players.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1));

            boolean foundNextItem = false;

            if (forceItemPlayer.currentTeam().getPreviousMaterial() == forceItemPlayer.currentTeam().getCurrentMaterial()) {
                foundNextItem = true;
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);

            } else if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) &&
                    hasItemInInventory(this.plugin.getBackpack().getTeamBackpack(forceItemPlayer.currentTeam()), forceItemPlayer.currentTeam().getCurrentMaterial())) {
                foundNextItem = true;
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);

            } else {
                for(ForceItemPlayer teamPlayers : forceItemPlayer.currentTeam().getPlayers()) {
                    if(hasItemInInventory(teamPlayers.player().getInventory(), forceItemPlayer.currentTeam().getCurrentMaterial())) {
                        foundNextItem = true;
                        forceItemPlayer.setBackToBackStreak(backToBacks + 1);
                    }
                }
            }

            this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
                if(achievement.checkRequirements(player, event)) {
                    for(ForceItemPlayer teamPlayers : forceItemPlayer.currentTeam().getPlayers()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(teamPlayers), 0L);
                    }
                }
            });

            if (!foundNextItem) {
                return;
            }

        } else {
            forceItemPlayer.setCurrentScore(forceItemPlayer.currentScore() + 1);
            forceItemPlayer.addFoundItemToList(new ForceItem(itemStack.getType(), this.plugin.getTimer().formatSeconds(this.plugin.getTimer().getTime()), System.currentTimeMillis(), event.isBackToBack(), event.isSkipped()));
            forceItemPlayer.setCurrentMaterial(this.plugin.getGamemanager().generateMaterial());

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
                forceItemPlayer.updateItemDisplay();
            }

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                this.plugin.getStatsManager().addToStats(PlayerStat.TOTAL_ITEMS, this.plugin.getStatsManager().playerStats(player.getName()), 1);
            }

            boolean foundNextItem = false;

            if (forceItemPlayer.previousMaterial() == forceItemPlayer.currentMaterial()) {
                foundNextItem = true;
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);

            } else if (hasItemInInventory(player.getInventory(), forceItemPlayer.currentMaterial())) {
                foundNextItem = true;
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);

            } else if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) &&
                    hasItemInInventory(this.plugin.getBackpack().getPlayerBackpack(player), forceItemPlayer.currentMaterial())) {
                foundNextItem = true;
                forceItemPlayer.setBackToBackStreak(backToBacks + 1);
            }

            this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
                if(achievement.checkRequirements(player, event)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
                }
            });

            if (!foundNextItem) {
                return;
            }
        }

        // Handle finding item back to back

        ItemStack foundItem = new ItemStack((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial()));

        FoundItemEvent foundNextItemEvent = new FoundItemEvent(player);
        foundNextItemEvent.setFoundItem(foundItem);
        foundNextItemEvent.setBackToBack(true);
        foundNextItemEvent.setSkipped(false);

        int totalItemsInPool = this.plugin.getItemDifficultiesManager().getAvailableItems().size();
        int itemsInInventory = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && !item.getType().isAir() && item.getType() != Material.BARRIER && item.getType() != Material.BUNDLE)
                .map(ItemStack::getType)
                .toList().size();
        Inventory backpack = this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? this.plugin.getBackpack().getTeamBackpack(forceItemPlayer.currentTeam()) : this.plugin.getBackpack().getPlayerBackpack(player);
        int itemsInBackpack = Arrays.stream(backpack.getContents()).filter(item -> item != null && !item.getType().isAir() && item.getType() != Material.BARRIER && item.getType() != Material.BUNDLE).map(ItemStack::getType).toList().size();
        int itemsInShulkerPlayer = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && !item.getType().isAir() && item.getType().name().contains("SHULKER_BOX"))
                .mapToInt(this::countItemsInShulkerBox)
                .sum();
        int itemsInShulkerBackpack = Arrays.stream(backpack.getContents())
                .filter(item -> item != null && !item.getType().isAir() && item.getType().name().contains("SHULKER_BOX"))
                .mapToInt(this::countItemsInShulkerBox)
                .sum();
        double probabilityDouble = Math.pow(((double) (itemsInInventory + itemsInBackpack + itemsInShulkerPlayer + itemsInShulkerBackpack) / totalItemsInPool), forceItemPlayer.backToBackStreak());
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>" + player.getName() + " <gray>was lucky to already own <reset>" + this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, foundItem.getType()) +
                        " <gold>" + this.plugin.getGamemanager().getMaterialName(foundItem.getType()) + " <dark_gray>» <aqua>" + decimalFormat.format(probabilityDouble * 100) + "%"));
        Bukkit.getPluginManager().callEvent(foundNextItemEvent);
    }

    private int countItemsInShulkerBox(ItemStack shulkerBox) {
        BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBox.getItemMeta();
        if(blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox box) {
            return (int) Arrays.stream(box.getInventory().getContents())
                    .filter(item -> item != null && !item.getType().isAir() && item.getType() != Material.BARRIER && item.getType() != Material.BUNDLE)
                    .count();
        }
        return 0;
    }

    public static boolean hasItemInInventory(Inventory inventory, Material targetMaterial) {
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
    public void onOpenAchievements(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if(!this.plugin.getGamemanager().isPreGame()) return;
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) return;
        if(e.getItem() == null) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.LIME_DYE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                new AchievementInventory(this.plugin, forceItemPlayer).open(player);
                return;
            }
        }
    }

    @EventHandler
    public void onAfterGame(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if(!this.plugin.getGamemanager().isEndGame()) return;
        if(e.getItem() == null) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.LIME_DYE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                new AchievementInventory(this.plugin, forceItemPlayer).open(player);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.COMPASS) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                new TeleporterInventory(this.plugin).open(player);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.GRASS_BLOCK) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if(player.getWorld().getName().equals("world")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<dark_red>✖<dark_gray>] <gray>You are already in the <green>overworld"));
                    return;
                }
                player.teleport(this.plugin.getSpawnLocation());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.NETHERRACK) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if(player.getWorld().getName().equals("world_nether")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<dark_red>✖<dark_gray>] <gray>You are already in the <red>nether"));
                    return;
                }
                player.teleport(new Location(Bukkit.getWorld("world_nether"), 0, 70, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.ENDER_EYE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if(player.getWorld().getName().equals("world_the_end")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>You are already in the <dark_purple>end"));
                    return;
                }
                World end = Bukkit.getWorld("world_the_end");
                Location location = new Location(end, 0, 0, 0);
                assert end != null;
                location.setY(end.getHighestBlockYAt(location) + 1);

                player.teleport(location);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }
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

        if (e.getItem().getType() == Material.KNOWLEDGE_BOOK) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                this.plugin.getAntimatterLocator().locateAntimatter(forceItemPlayer);
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
            stack.setAmount((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? stack.getAmount() - 1 : jokers));
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
    public void onChat_(AsyncChatEvent asyncChatEvent) {
        Player player = asyncChatEvent.getPlayer();
        asyncChatEvent.setCancelled(true);
        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>" + player.getName() + " <dark_gray>» <gray>" + PlainTextComponentSerializer.plainText().serialize(asyncChatEvent.originalMessage())));
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

        this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {

            if(achievement.checkRequirements(player, playerItemConsumeEvent)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
            }
        });
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
        ForceItemPlayer gamePlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        String plainDeathMessage = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(event.deathMessage()));
        String plainPlayerName = PlainTextComponentSerializer.plainText().serialize(player.name());

        event.deathMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<red>\uD83D\uDC80<dark_gray>] " + plainDeathMessage.replace(plainPlayerName, "<gold>" + player.getName() + "<gray>")));
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
            event.getDrops().removeIf(Gamemanager::isBackpack);
        }

        gamePlayer.removeItemDisplay();

        this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
            if(achievement.checkRequirements(player, event)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(gamePlayer), 0L);
            }
        });

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

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
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
    public void onGliding(EntityToggleGlideEvent entityToggleGlideEvent) {
        if(entityToggleGlideEvent.isGliding()) {
            if(!this.plugin.getSettings().isSettingEnabled(GameSetting.ELYTRA)) {
                entityToggleGlideEvent.setCancelled(true);
            }
        }
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

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Travelling to other dimensions is disabled!"));
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent playerAdvancementDoneEvent) {
        Advancement advancement = playerAdvancementDoneEvent.getAdvancement();
        if(advancement.key().namespace().equals("fib")) {
            String plainAdvancement = PlainTextComponentSerializer.plainText().serialize(advancement.displayName());
            String plainAdvancementDescription = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(advancement.getDisplay()).description());

            String advancementType = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "has completed the challenge" : "has made the advancement";
            String advancementTypeColor = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "<dark_purple>" : "<green>";

            playerAdvancementDoneEvent.message(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<yellow>⭐<dark_gray>] <gold>" + playerAdvancementDoneEvent.getPlayer().getName() + " <gray>" + advancementType + " <hover:show_text:'" + advancementTypeColor + plainAdvancement + "<newline>" + advancementTypeColor + plainAdvancementDescription + "'>" + advancementTypeColor + plainAdvancement + "</hover>"));
            if(advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE) {
                Bukkit.getOnlinePlayers().forEach(players -> {
                    if(players == playerAdvancementDoneEvent.getPlayer()) return;
                    players.playSound(players.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                });
            }
        } else {
            playerAdvancementDoneEvent.message(null);
        }

    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent playerChangedWorldEvent) {
        Player player = playerChangedWorldEvent.getPlayer();
        if(this.plugin.getGamemanager().isMidGame()) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            if(player.getWorld().getName().equals("world_the_end")) {
                Location spawnLocation = player.getLocation();
                spawnLocation.setY(player.getWorld().getHighestBlockYAt(spawnLocation) + 1);
                player.teleport(spawnLocation);
            }
            this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
                if(achievement.checkRequirements(player, playerChangedWorldEvent)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
                }
            });
        }
    }

    @EventHandler
    public void onTrade(PlayerTradeEvent playerTradeEvent) {
        Player player = playerTradeEvent.getPlayer();
        if(this.plugin.getGamemanager().isMidGame()) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
                if(achievement.checkRequirements(player, playerTradeEvent)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
                }
            });
        }
    }

    @EventHandler
    public void onOpenLootChest(PlayerInteractEvent playerInteractEvent) {
        Player player = playerInteractEvent.getPlayer();

        if(playerInteractEvent.getItem() == null) return;
        if(playerInteractEvent.getClickedBlock() == null) return;
        if(playerInteractEvent.getAction().isRightClick()) {
            if(playerInteractEvent.getClickedBlock().getType() == Material.CHEST) {
                if(this.plugin.getGamemanager().isMidGame()) {
                    ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                    this.plugin.getAchievementManager().achievementsList().forEach(achievement -> {
                        if(achievement.checkRequirements(player, playerInteractEvent)) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> achievement.grantTo(forceItemPlayer), 0L);
                        }
                    });
                }
            }
        }
    }
}
