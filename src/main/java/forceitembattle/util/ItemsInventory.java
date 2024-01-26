package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemsInventory extends InventoryBuilder {


    public ItemsInventory(Player player) {
        super(9*6, "§8» §6Items §8● §7Settings");

        HashMap<Integer, HashMap<Integer, ItemStack>> pages = new HashMap<>();
        HashMap<Integer, ItemStack> itemStackHashMap = new HashMap<>();

        int startSlot = 9;
        int initialPages = 0;
        int items = 0;
        final int[] currentPage = {0};


        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§6").addItemFlags(ItemFlag.values()).getItemStack());

        this.setItem(0, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§4« §cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentPage[0] != 0) {
                currentPage[0]--;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                player.sendMessage("Current page: " + currentPage[0] + " with " + pages.get(currentPage[0]).size() + " items in it");

                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });
        this.setItem(8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§2» §aNext Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentPage[0] < (int)(Arrays.stream(Material.values()).filter(Material::isItem).count() / (this.getInventory().getSize() - 9))) {
                currentPage[0]++;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                player.sendMessage("Current page: " + currentPage[0] + " with " + pages.get(currentPage[0]).size() + " items in it");

                this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });


        for (Material materials : Material.values()) {
            if(materials.isItem() && !materials.isAir()) {
                if (!pages.containsKey(initialPages)) {
                    itemStackHashMap = new HashMap<>();
                    pages.put(initialPages, itemStackHashMap);
                }

                itemStackHashMap.put(startSlot, new ItemStack(materials));

                startSlot++;

                if(startSlot == this.getInventory().getSize()) {
                    startSlot = 9;
                    initialPages++;
                }

                items++;
            }

        }


        pages.get(currentPage[0]).forEach(this::setItem);


        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }
}
