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
        super(9*5, "§8» §3Settings §8● §7Menu");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(36, 44, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        /* Food-Setting */
        this.setItem(19, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§aFood §2✔" : "§cFood §4✘")).getItemStack(), inventoryClickEvent -> {
            Player player = (Player) inventoryClickEvent.getWhoClicked();

            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);

            ForceItemBattle.getInstance().getConfig().set("settings.food", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.food"));
            ForceItemBattle.getInstance().saveConfig();

            this.getInventory().getItem(19).setAmount(this.getInventory().getItem(19).getAmount() + 1);

            this.setItem(19, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());
        });

        /* KeepInv-Setting */
        this.setItem(21, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory") ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack(), inventoryClickEvent -> {
            Player player = (Player) inventoryClickEvent.getWhoClicked();



            });

        /* Backpack-Setting */
        this.setItem(23, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack") ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack(), inventoryClickEvent -> {
            Player player = (Player) inventoryClickEvent.getWhoClicked();

            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);


        });

        /* Teams-Setting */
        this.setItem(25, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame") ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack(), inventoryClickEvent -> {
            Player player = (Player) inventoryClickEvent.getWhoClicked();

            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);

        });


        this.addClickHandler(inventoryClickEvent -> {
            Player player = (Player)inventoryClickEvent.getWhoClicked();
            inventoryClickEvent.setCancelled(true);

            if(inventoryClickEvent.getCurrentItem() == null) return;

            if(inventoryClickEvent.isLeftClick()) {
                if(inventoryClickEvent.getSlot() == 19) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.food", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.food"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(19, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());

                } else if(inventoryClickEvent.getSlot() == 21) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.keepinventory", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory"));
                    ForceItemBattle.getInstance().saveConfig();

                    Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.KEEP_INVENTORY, ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory")));

                    this.setItem(21, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory") ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack());

                } else if(inventoryClickEvent.getSlot() == 23) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.backpack", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(23, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack") ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack());

                } else if(inventoryClickEvent.getSlot() == 25) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    ForceItemBattle.getInstance().getConfig().set("settings.isTeamGame", !ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame"));
                    ForceItemBattle.getInstance().saveConfig();

                    this.setItem(25, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame") ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());

                }
            }
        });
    }
}
