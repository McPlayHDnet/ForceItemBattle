package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InvSettings extends InventoryBuilder {

    public InvSettings() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        super(9*6, "§8» §3Settings §8● §7Menu");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        /* Food-Setting */
        this.setItem(20, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());

        /* KeepInv-Setting */
        this.setItem(22, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory") ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack());

        /* Backpack-Setting */
        this.setItem(24, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack") ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack());

        /* PvP-Setting */
        this.setItem(30, new ItemBuilder(Material.IRON_SWORD).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.pvp") ? "§aPvP §2✔" : "§cPvP §4✘")).getItemStack());

        /* Teams-Setting */
        this.setItem(32, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame") ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());


        this.addClickHandler(inventoryClickEvent -> {
            Player player = (Player)inventoryClickEvent.getWhoClicked();
            inventoryClickEvent.setCancelled(true);

            if(inventoryClickEvent.getCurrentItem() == null) return;

            if(inventoryClickEvent.isLeftClick()) {
                /* Food-Setting */
                if(inventoryClickEvent.getSlot() == 20) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.food", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.food"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(20, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());

                /* KeepInv-Setting */
                } else if(inventoryClickEvent.getSlot() == 22) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.keepinventory", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory"));
                    ForceItemBattle.getInstance().saveConfig();

                    Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.KEEP_INVENTORY, ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory")));

                    this.setItem(22, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory") ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack());

                /* Backpack-Setting */
                } else if(inventoryClickEvent.getSlot() == 24) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.backpack", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(24, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack") ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack());

                /* PvP-Setting */
                } else if(inventoryClickEvent.getSlot() == 30) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.pvp", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.pvp"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(30, new ItemBuilder(Material.IRON_SWORD).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.pvp") ? "§aPvP §2✔" : "§cPvP §4✘")).getItemStack());

                /* Teams-Setting */
                } else if(inventoryClickEvent.getSlot() == 32) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.isTeamGame", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(32, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame") ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());

                }
            }
        });
    }
}
