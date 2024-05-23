package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeInventory extends InventoryBuilder {

    private final int[] PLAYER_SLOTS = { 9, 10, 11,
                                        18, 19, 20,
                                        27, 28, 29 };

    private final int[] OPPOSITE_SLOTS = { 15, 16, 17,
                                          24, 25, 26,
                                          33, 34, 35 };

    public TradeInventory(ForceItemBattle plugin, ForceItemPlayer player, ForceItemPlayer oppositePlayer) {
        super(9 * 5, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Trade <dark_gray>● <gray>Menu"));

        Map<ForceItemPlayer, List<ItemStack>> tradingItems = new HashMap<>();

        this.setItems(0, getInventory().getSize() - 1, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());

        this.setItem(1, new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture(player.player().getPlayerProfile().getTextures()).setDisplayName("<green>You").getItemStack());
        this.setItem(7, new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture(oppositePlayer.player().getPlayerProfile().getTextures()).setDisplayName("<red>" + oppositePlayer.player().getName()).getItemStack());

        this.setItems(this.PLAYER_SLOTS, new ItemBuilder(Material.AIR).getItemStack());
        this.setItems(this.OPPOSITE_SLOTS, new ItemBuilder(Material.AIR).getItemStack());

        this.setItem(21, new ItemBuilder(this.isItemInsideSlots(this.PLAYER_SLOTS, this.getInventory()) ? Material.ORANGE_DYE : Material.RED_DYE).setDisplayName("<dark_gray>● <gray>Status <dark_gray>» <red>Not ready").getItemStack());
        this.setItem(23, new ItemBuilder(this.isItemInsideSlots(this.OPPOSITE_SLOTS, this.getInventory()) ? Material.ORANGE_DYE : Material.RED_DYE).setDisplayName("<dark_gray>● <gray>Status <dark_gray>» <red>Not ready").getItemStack());

        this.addClickHandler(inventoryClickEvent -> {
            inventoryClickEvent.setCancelled(true);
            if(inventoryClickEvent.getCurrentItem() == null) return;

            if(inventoryClickEvent.getClickedInventory() == inventoryClickEvent.getView().getBottomInventory()) {
                for(int slot : this.PLAYER_SLOTS) {
                    if(this.getInventory().getItem(slot) == null) {
                        this.setItem(slot, inventoryClickEvent.getCurrentItem());

                        List<ItemStack> itemStacks = new ArrayList<>();
                        if(tradingItems.get(player) != null) itemStacks = tradingItems.get(player);

                        itemStacks.add(inventoryClickEvent.getCurrentItem());
                        tradingItems.put(player, itemStacks);

                        break;
                    }

                }

                for(int slot : this.OPPOSITE_SLOTS) {
                    if(oppositePlayer.player().getOpenInventory().getItem(slot) == null) {
                        oppositePlayer.player().getOpenInventory().setItem(slot, inventoryClickEvent.getCurrentItem());

                        List<ItemStack> itemStacks = new ArrayList<>();
                        if(tradingItems.get(oppositePlayer) != null) itemStacks = tradingItems.get(oppositePlayer);

                        itemStacks.add(inventoryClickEvent.getCurrentItem());
                        tradingItems.put(oppositePlayer, itemStacks);

                        break;
                    }
                }
            }
        });
    }

    private boolean isItemInsideSlots(int[] targetSlots, Inventory inventory) {
        for(int slots : targetSlots) {
            if(inventory.getItem(slots) != null) {
                return true;
            }
            break;
        }

        return false;
    }
}
