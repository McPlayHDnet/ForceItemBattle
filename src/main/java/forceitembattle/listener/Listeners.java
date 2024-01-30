package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItem;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Listeners implements Listener {

    private Map<UUID, ItemStack> remainingJokers = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) {
            if (!ForceItemBattle.getGamemanager().isPlayerInMaps(event.getPlayer())) {
                event.getPlayer().getInventory().clear();
                event.getPlayer().setLevel(0);
                event.getPlayer().setExp(0);
                event.getPlayer().setGameMode(GameMode.SPECTATOR);
            } else {
                ForceItemBattle.getTimer().getBossBar().get(event.getPlayer().getUniqueId()).addPlayer(event.getPlayer());
            }
        } else {

            event.getPlayer().getInventory().clear();
            event.getPlayer().setLevel(0);
            event.getPlayer().setExp(0);
            event.getPlayer().setGameMode(GameMode.ADVENTURE);

            event.getPlayer().setWalkSpeed(0);
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 150, false, false));

            if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame"))
                /* Team TODO */
                //event.getPlayer().getInventory().setItem(0, ForceItemBattle.getInvSettings().createGuiItem(Material.PAPER, ChatColor.GREEN + "Teams", "right click to choose your team"));
            if (event.getPlayer().isOp()) {
                //event.getPlayer().getInventory().setItem(7, ForceItemBattle.getInvSettings().createGuiItem(Material.COMMAND_BLOCK_MINECART, ChatColor.YELLOW + "Settings", "right click to edit"));
                //event.getPlayer().getInventory().setItem(8, ForceItemBattle.getInvSettings().createGuiItem(Material.LIME_DYE, ChatColor.GREEN + "Start", "right click to start"));
            }
        }
        event.getPlayer().setScoreboard(ForceItemBattle.getGamemanager().getBoard());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
    }

    /* Found-/Skip Item */
    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if(entityPickupItemEvent.getEntity() instanceof Player player) {
            if(ForceItemBattle.getTimer().isRunning()) {
                ItemStack pickedItem = entityPickupItemEvent.getItem().getItemStack();
                Material currentMaterial = ForceItemBattle.getGamemanager().getCurrentMaterial(player);

                if(pickedItem.getType() == currentMaterial) {
                    FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                    foundItemEvent.setFoundItem(pickedItem);
                    foundItemEvent.skipped(false);

                    Bukkit.getPluginManager().callEvent(foundItemEvent);
                }
            }
        }
    }

    @EventHandler
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if(ForceItemBattle.getTimer().isRunning()) {
            ItemStack clickedItem = inventoryClickEvent.getCurrentItem();
            Material currentItem = ForceItemBattle.getGamemanager().getCurrentMaterial(player);

            if(clickedItem == null) return;

            if(clickedItem.getType() == currentItem) {
                FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                foundItemEvent.setFoundItem(clickedItem);
                foundItemEvent.skipped(false);

                Bukkit.getPluginManager().callEvent(foundItemEvent);
            }
        }
    }


    /* Custom Found-Item Event */
    @EventHandler
    public void onFoundItem(FoundItemEvent foundItemEvent) {
        Player player = foundItemEvent.getPlayer();
        ItemStack itemStack = foundItemEvent.getFoundItem();

        ForceItemBattle.getGamemanager().getScore().put(player.getUniqueId(), ForceItemBattle.getGamemanager().getScore().get(player.getUniqueId()) + 1);
        ArrayList<ForceItem> mat = ForceItemBattle.getGamemanager().getItemList(player);
        mat.add(new ForceItem(itemStack.getType(), ForceItemBattle.getTimer().formatSeconds(ForceItemBattle.getTimer().getTime()), foundItemEvent.isSkipped()));
        ForceItemBattle.getGamemanager().getItemList().put(player.getUniqueId(), mat);
        ForceItemBattle.getGamemanager().getCurrentMaterial().put(player.getUniqueId(), ForceItemBattle.getGamemanager().generateMaterial());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

        ArmorStand armorStand = (ArmorStand) player.getPassengers().get(0);
        armorStand.getEquipment().setHelmet(new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player)));

        Bukkit.broadcastMessage("§a" + player.getName() + " §7" + (foundItemEvent.isSkipped() ? "skipped" : "found") + " §6" + WordUtils.capitalize(itemStack.getType().name().toLowerCase().replace("_", " ")));
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) { // triggered if a joker is used
        if (!ForceItemBattle.getTimer().isRunning()) return;
        if (!ForceItemBattle.getGamemanager().isPlayerInMaps(e.getPlayer())) return;
        if(e.getItem() == null) return;
        /*
        if (ForceItemBattle.getGamemanager().hasDelay(e.getPlayer())) {
            e.getPlayer().sendMessage(ChatColor.RED + "Please wait a second.");
            return;
        }
        */


        if(e.getItem().getType() == Material.BARRIER) {
            if(e.getAction() == Action.RIGHT_CLICK_AIR) {
                int jokers = ForceItemBattle.getGamemanager().getJokers().get(e.getPlayer().getUniqueId());
                if (jokers > 0) {

                    jokers--;

                    ItemStack stack = e.getPlayer().getInventory().getItem(e.getPlayer().getInventory().first(Material.BARRIER));
                    if (stack.getAmount() > 1) {
                        stack.setAmount(jokers);
                    } else {
                        stack.setType(Material.AIR);
                    }
                    Material mat;
                    if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                        /////////////////////////////////////// TEAMS ///////////////////////////////////////
                        mat = ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(e.getPlayer());
                    } else {
                        mat = ForceItemBattle.getGamemanager().getCurrentMaterial(e.getPlayer());
                    }
                    e.getPlayer().getInventory().setItem(e.getPlayer().getInventory().first(Material.BARRIER), stack);
                    e.getPlayer().getInventory().addItem(new ItemStack(mat));
                    if (!e.getPlayer().getInventory().contains(mat)) {
                        e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), new ItemStack(mat));
                    }
                    ForceItemBattle.getTimer().sendActionBar();
                    //ForceItemBattle.getGamemanager().setDelay(e.getPlayer(), 2);

                    ArmorStand armorStand = (ArmorStand) e.getPlayer().getPassengers().get(0);
                    armorStand.getEquipment().setHelmet(new ItemStack(mat));

                    FoundItemEvent foundItemEvent = new FoundItemEvent(e.getPlayer());
                    foundItemEvent.setFoundItem(new ItemStack(mat));
                    foundItemEvent.skipped(true);

                    Bukkit.getPluginManager().callEvent(foundItemEvent);

                    ForceItemBattle.getGamemanager().getJokers().put(e.getPlayer().getUniqueId(), jokers);
                } else {
                    e.getPlayer().sendMessage("§cNo more skips left.");
                }
            } else if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.getPlayer().sendMessage("§cClick it in the air");
            }

        } else if(e.getItem().getType() == Material.BUNDLE) {
            ForceItemBattle.getBackpack().openPlayerBackpack(e.getPlayer());
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

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            movedItem = event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR ?
                    event.getWhoClicked().getInventory().getItem(event.getHotbarButton())
                    : event.getCurrentItem();
        }

        if (movedItem != null) {
            if (movedItem.getType() == Material.BARRIER || movedItem.getType() == Material.BUNDLE) {
                event.setCancelled(true);
                return;
            }
        }


        if (event.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder) {
            inventoryBuilder.handleClick(event);
            return;
        }

    }

    @EventHandler
    public void onOffHand(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if(playerSwapHandItemsEvent.getMainHandItem() == null || playerSwapHandItemsEvent.getOffHandItem() == null) return;
        if(playerSwapHandItemsEvent.getMainHandItem().getType() == Material.BARRIER ||
                playerSwapHandItemsEvent.getMainHandItem().getType() == Material.BUNDLE ||
                playerSwapHandItemsEvent.getOffHandItem().getType() == Material.BARRIER ||
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
                Bukkit.getScheduler().runTask(ForceItemBattle.getInstance(), () -> inventoryBuilder.open((Player) inventoryCloseEvent.getPlayer()));
                return;

            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!ForceItemBattle.getTimer().isRunning()) {
            event.setCancelled(true);
        }

        if (isPvpEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        // Disable Fire damage if pvp disabled and there's another player nearby
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            for (Entity nearby : event.getEntity().getNearbyEntities(5, 5, 5)) {
                if (!(nearby instanceof Player)) {
                    continue;
                }

                boolean isSameAsDamaged = nearby.getName().equalsIgnoreCase(event.getEntity().getName());
                if (!isSameAsDamaged) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isPvpEnabled() {
        return ForceItemBattle.getInstance().getConfig().getBoolean("settings.pvp");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvpDisabled(EntityDamageByEntityEvent event) {
        if (isPvpEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!(getEntityOrigin(event.getDamager()) instanceof Player)) {
            return;
        }

        event.setCancelled(true);
    }

    private Object getEntityOrigin(Entity entity) {
        if (entity instanceof Projectile) {
            return ((Projectile) entity).getShooter();
        }

        if (entity instanceof TNTPrimed) {
            return ((TNTPrimed) entity).getSource();
        }

        return entity;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        Player player = playerDeathEvent.getEntity();
        player.getPassengers().forEach(Entity::remove);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        Player player = playerRespawnEvent.getPlayer();
        ItemStack jokers = new ItemBuilder(Material.BARRIER).setAmount(ForceItemBattle.getGamemanager().getJokers().get(player.getUniqueId())).setDisplayName("§5Skip").getItemStack();
        player.getInventory().setItem(4, jokers);
        player.getInventory().setItem(8, new ItemStack(Material.BUNDLE));

        ArmorStand itemDisplay = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, 2, 0), EntityType.ARMOR_STAND);
        itemDisplay.getEquipment().setHelmet(new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player)));
        itemDisplay.setInvisible(true);
        itemDisplay.setInvulnerable(true);
        itemDisplay.setGravity(false);
        player.addPassenger(itemDisplay);
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractAtEntityEvent playerInteractAtEntityEvent) {
        if(playerInteractAtEntityEvent.getRightClicked() instanceof ArmorStand armorStand) {
            if(armorStand.isInvisible()) playerInteractAtEntityEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent playerDropItemEvent) {
        if(playerDropItemEvent.getItemDrop().getItemStack().getType() == Material.BARRIER || playerDropItemEvent.getItemDrop().getItemStack().getType() == Material.BUNDLE) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if(!ForceItemBattle.getTimer().isRunning()) return;
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food")) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.getBlock().getType() == Material.BARRIER) event.setCancelled(true);
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (ForceItemBattle.getTimer().isRunning()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (ForceItemBattle.getTimer().isRunning()) return;
        if (event.getTarget() == null) return;
        if (event.getTarget().getType() != EntityType.PLAYER) return;
        event.setTarget(null);
        event.setCancelled(true);
    }
}
