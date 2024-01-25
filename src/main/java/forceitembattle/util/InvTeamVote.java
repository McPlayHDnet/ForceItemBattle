package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class InvTeamVote implements Listener {
    private final Inventory inv;

    public InvTeamVote() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 9, "CHOOSE YOUR TEAM");

        // Put the items into the inventory
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        inv.clear();

        inv.setItem(0, createGuiItem(Material.WHITE_WOOL, ChatColor.WHITE + "Team1", ForceItemBattle.getGamemanager().getPlayersInTeam("team1")));
        inv.setItem(1, createGuiItem(Material.GRAY_WOOL, ChatColor.DARK_GRAY + "Team2", ForceItemBattle.getGamemanager().getPlayersInTeam("team2")));
        inv.setItem(2, createGuiItem(Material.RED_WOOL, ChatColor.RED + "Team3", ForceItemBattle.getGamemanager().getPlayersInTeam("team3")));
        inv.setItem(3, createGuiItem(Material.YELLOW_WOOL, ChatColor.YELLOW + "Team4", ForceItemBattle.getGamemanager().getPlayersInTeam("team4")));
        inv.setItem(4, createGuiItem(Material.LIME_WOOL, ChatColor.GREEN + "Team5", ForceItemBattle.getGamemanager().getPlayersInTeam("team5")));
        inv.setItem(5, createGuiItem(Material.CYAN_WOOL, ChatColor.AQUA + "Team6", ForceItemBattle.getGamemanager().getPlayersInTeam("team6")));
        inv.setItem(6, createGuiItem(Material.BLUE_WOOL, ChatColor.DARK_BLUE + "Team7", ForceItemBattle.getGamemanager().getPlayersInTeam("team7")));
        inv.setItem(7, createGuiItem(Material.MAGENTA_WOOL, ChatColor.DARK_PURPLE + "Team8", ForceItemBattle.getGamemanager().getPlayersInTeam("team8")));
        inv.setItem(8, createGuiItem(Material.PINK_WOOL, ChatColor.LIGHT_PURPLE + "Team9", ForceItemBattle.getGamemanager().getPlayersInTeam("team9")));
    }

    // Nice little method to create a gui item with a custom name, and description
    protected ItemStack createGuiItem(final Material material, final String name, final List<String> list) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        // Set the name of the item
        meta.setDisplayName(name);

        // Set the lore of the item
        meta.setLore(list);

        item.setItemMeta(meta);

        return item;
    }

    // You can open the inventory with this
    public void openInventory(final HumanEntity ent) {
        initializeItems();
        ent.openInventory(inv);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().equals(Material.AIR)) return;

        final Player p = (Player) e.getWhoClicked();

        // Using slots click is a best option for your inventory click's
        //p.sendMessage("You clicked at slot " + e.getRawSlot());
        if (e.getRawSlot() > inv.getSize()) return;

        /////////////////////////////////////////////////////////////////////////
        String team = "team" + (e.getRawSlot()+1);
        if (e.isLeftClick() && !ForceItemBattle.getGamemanager().isTeam(p, team)) {
            ForceItemBattle.getGamemanager().addPlayerToTeam(p, team);
            p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        }
        else if (e.isRightClick() && ForceItemBattle.getGamemanager().isTeam(p, team)) {
            ForceItemBattle.getGamemanager().removePlayerFromTeam(p);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
        /////////////////////////////////////////////////////////////////////////
        ((Player) e.getWhoClicked()).setScoreboard(ForceItemBattle.getGamemanager().getBoard());
        initializeItems();
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        HumanEntity p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() != Material.PAPER) return;
        openInventory(p);
    }

    public Inventory getInv() {
        return inv;
    }
}
