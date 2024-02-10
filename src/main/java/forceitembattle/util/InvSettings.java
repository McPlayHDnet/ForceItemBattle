package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

public class InvSettings extends InventoryBuilder {
    
    public InvSettings(ForceItemBattle plugin) {
        super(9*6, "§8» §3Settings §8● §7Menu");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {
            /* Food-Setting */
            this.setItem(19, new ItemBuilder(Material.COOKED_BEEF)
                    .setDisplayName("§8» " + (plugin.getSettings().isFoodEnabled() ? "§aFood §2✔" : "§cFood §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getSettings().setFoodEnabled(!plugin.getSettings().isFoodEnabled());
            });

            /* KeepInv-Setting */
            this.setItem(21, new ItemBuilder(Material.TOTEM_OF_UNDYING)
                    .setDisplayName("§8» " + (plugin.getSettings().isKeepInventoryEnabled() ? "§aKeep Inventory §2✔" : "§cKeep Inventory §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getSettings().setKeepInventoryEnabled(!plugin.getSettings().isKeepInventoryEnabled());
            });

            /* Backpack-Setting */
            this.setItem(23, new ItemBuilder(Material.BUNDLE)
                    .setDisplayName("§8» " + (plugin.getSettings().isBackpackEnabled() ? "§aBackpack §2✔" : "§cBackpack §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getSettings().setBackpackEnabled(!plugin.getSettings().isBackpackEnabled());
            });

            /* PvP-Setting */
            this.setItem(25, new ItemBuilder(Material.IRON_SWORD)
                    .setDisplayName("§8» " + (plugin.getSettings().isPvpEnabled() ? "§aPvP §2✔" : "§cPvP §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                plugin.getSettings().setPvpEnabled(!plugin.getSettings().isPvpEnabled());
            });

            /* Teams-Setting */
            this.setItem(29, new ItemBuilder(Material.RED_BED)
                    .setDisplayName("§8» " + (plugin.getSettings().isTeamGame() ? "§aTeams §2✔" : "§cTeams §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                getPlayer().sendMessage("§cWork in progress...");
                    /* TODO: teams
                    forceItemBattle.getConfig().set("settings.isTeamGame", !forceItemBattle.getSettings().isTeamGame());
                    forceItemBattle.saveConfig();

                    this.setItem(29, new ItemBuilder(Material.RED_BED).setDisplayName("§8» " + (forceItemBattle.getSettings().isTeamGame() ? "§aTeams §2✔" : "§cTeams §4✘")).getItemStack());
                    */
            });

            /* Faster growth & decay */
            this.setItem(31, new ItemBuilder(Material.CACTUS)
                    .setDisplayName("§8» " + (plugin.getSettings().isFasterRandomTick() ? "§aFaster plants growth & decay §2✔" : "§cFaster plants growth & decay §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getSettings().setFasterRandomTick(!plugin.getSettings().isFasterRandomTick());
            });

            /* Nether-Setting */
            this.setItem(33, new ItemBuilder(Material.NETHERRACK)
                    .setDisplayName("§8» " + (plugin.getSettings().isNetherEnabled() ? "§aNether §2✔" : "§cNether §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getItemDifficultiesManager().toggleNetherItems();
            });

            /* End-Setting */
            this.setItem(39, new ItemBuilder(Material.END_STONE)
                    .setDisplayName("§8» " + (plugin.getSettings().isEndEnabled() ? "§aEnd §2✔" : "§cEnd §4✘"))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getSettings().setEndEnabled(!plugin.getSettings().isEndEnabled());
            });
        });
    }
}
