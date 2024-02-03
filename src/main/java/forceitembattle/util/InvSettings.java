package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

public class InvSettings extends InventoryBuilder {
    
    public InvSettings(ForceItemBattle forceItemBattle) {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        super(9*6, "§8» §3Settings §8● §7Menu");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        /* Food-Setting */
        this.setItem(19, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (forceItemBattle.getSettings().isFoodEnabled() ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());

        /* KeepInv-Setting */
        this.setItem(21, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (forceItemBattle.getSettings().isKeepInventoryEnabled() ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack());

        /* Backpack-Setting */
        this.setItem(23, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (forceItemBattle.getSettings().isBackpackEnabled() ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack());

        /* PvP-Setting */
        this.setItem(25, new ItemBuilder(Material.IRON_SWORD).setDisplayName("§8» " + (forceItemBattle.getSettings().isPvpEnabled() ? "§aPvP §2✔" : "§cPvP §4✘")).getItemStack());

        /* Teams-Setting */
        this.setItem(29, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (forceItemBattle.getSettings().isTeamGame() ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());

        /* Nether-Setting */
        this.setItem(33, new ItemBuilder(Material.NETHERRACK).setDisplayName("§8» " + (forceItemBattle.getSettings().isNetherEnabled() ? "§aNether §2✔" : "§cNether §4✘")).getItemStack());


        this.addClickHandler(inventoryClickEvent -> {
            Player player = (Player)inventoryClickEvent.getWhoClicked();
            inventoryClickEvent.setCancelled(true);

            if(inventoryClickEvent.getCurrentItem() == null) return;

            if(inventoryClickEvent.isLeftClick()) {
                /* Food-Setting */
                if(inventoryClickEvent.getSlot() == 19) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    forceItemBattle.getSettings().setFoodEnabled(!forceItemBattle.getSettings().isFoodEnabled());

                    this.setItem(19, new ItemBuilder(Material.COOKED_BEEF).setDisplayName("§8» " + (forceItemBattle.getSettings().isFoodEnabled() ? "§aFood §2✔" : "§cFood §4✘")).getItemStack());

                /* KeepInv-Setting */
                } else if(inventoryClickEvent.getSlot() == 21) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    forceItemBattle.getSettings().setKeepInventoryEnabled(!forceItemBattle.getSettings().isKeepInventoryEnabled());

                    Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.KEEP_INVENTORY, forceItemBattle.getSettings().isKeepInventoryEnabled()));

                    this.setItem(21, new ItemBuilder(Material.TOTEM_OF_UNDYING).setDisplayName("§8» " + (forceItemBattle.getSettings().isKeepInventoryEnabled() ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘")).getItemStack());

                /* Backpack-Setting */
                } else if(inventoryClickEvent.getSlot() == 23) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    forceItemBattle.getSettings().setBackpackEnabled(!forceItemBattle.getSettings().isBackpackEnabled());

                    this.setItem(23, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» " + (forceItemBattle.getSettings().isBackpackEnabled() ? "§aBackpack §2✔" : "§cBackpack §4✘")).getItemStack());

                /* PvP-Setting */
                } else if(inventoryClickEvent.getSlot() == 25) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    forceItemBattle.getSettings().setPvpEnabled(!forceItemBattle.getSettings().isPvpEnabled());

                    this.setItem(25, new ItemBuilder(Material.IRON_SWORD).setDisplayName("§8» " + (forceItemBattle.getSettings().isPvpEnabled() ? "§aPvP §2✔" : "§cPvP §4✘")).getItemStack());

                /* Teams-Setting */
                } else if(inventoryClickEvent.getSlot() == 29) {
                    player.playSound(player, Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage("§cWork in progress...");
                    /* TODO: teams
                    forceItemBattle.getConfig().set("settings.isTeamGame", !forceItemBattle.getSettings().isTeamGame());
                    forceItemBattle.saveConfig();

                    this.setItem(29, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (forceItemBattle.getSettings().isTeamGame() ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());
                    */

                /* Nether-Setting */
                } else if(inventoryClickEvent.getSlot() == 33) {
                    player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);

                    forceItemBattle.getItemDifficultiesManager().toggleNetherItems();

                    this.setItem(33, new ItemBuilder(Material.NETHERRACK).setDisplayName("§8» " + (forceItemBattle.getSettings().isNetherEnabled() ? "§aNether §2✔" : "§cNether §4✘")).getItemStack());

                }
            }
        });
    }
}
