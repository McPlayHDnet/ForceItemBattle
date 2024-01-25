package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class InvTeleport implements Listener {
    private final Inventory inv;

    public InvTeleport() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 54, "Teleporter");

        // Put the items into the inventory
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        inv.clear();
        List<Player> players = (List<Player>) Bukkit.getOnlinePlayers().stream().toList();

        for (int i=0; i<=players.size()-1; i++) {
            if (players.get(i).getGameMode() == GameMode.SURVIVAL) inv.setItem(i, createGuiItem(players.get(i)));
        }
    }

    // Nice little method to create a gui item with a custom name, and description
    protected ItemStack createGuiItem(Player player) {
        final ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(player.getName());
        skullMeta.setLore(Arrays.asList("click to teleport"));

        item.setItemMeta(skullMeta);
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

        //////////////////////////////////////////////////////////////
        if (Bukkit.getPlayer(e.getCurrentItem().getItemMeta().getDisplayName()) != null) {
            ((Player) e.getWhoClicked()).getPlayer().teleport(Bukkit.getPlayer(e.getCurrentItem().getItemMeta().getDisplayName()));
            e.getWhoClicked().closeInventory();
            ((Player) e.getWhoClicked()).playSound(e.getWhoClicked(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        } else initializeItems();
        //////////////////////////////////////////////////////////////
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
        if (ForceItemBattle.getTimer().isRunning()) return;
        if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS) openInventory(e.getPlayer());
    }

    public Inventory getInv() {
        return inv;
    }
}
