package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ItemsInventory extends InventoryBuilder {


    public ItemsInventory(ForceItemBattle forceItemBattle, Player player) {
        super(9*6, forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Items <gray>(" + forceItemBattle.getItemDifficultiesManager().getAllItems().size() + ") <dark_gray>● <gray>Settings"));

        HashMap<Integer, HashMap<Integer, ItemStack>> pages = new HashMap<>();

        final int[] currentPage = {0};
        AtomicReference<Material> currentFilter = new AtomicReference<>(Material.LIME_DYE);

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<gold>").addItemFlags(ItemFlag.values()).getItemStack());


        this.setItem(2, new ItemBuilder(Material.LIME_DYE).setDisplayName("<green>All items").setGlowing(currentFilter.get() == Material.LIME_DYE).addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentFilter.get() == Material.LIME_DYE) return;
            currentFilter.set(Material.LIME_DYE);
            currentPage[0] = 0;

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
            this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);

        });

        this.setItem(3, new ItemBuilder(Material.ORANGE_DYE).setDisplayName("<gold>Included Items").setGlowing(currentFilter.get() == Material.ORANGE_DYE).addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentFilter.get() == Material.ORANGE_DYE) return;
            currentFilter.set(Material.ORANGE_DYE);
            currentPage[0] = 0;

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
            this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);

        });

        this.setItem(4, new ItemBuilder(Material.RED_DYE).setDisplayName("<red>All non-craftable").setGlowing(currentFilter.get() == Material.RED_DYE).addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentFilter.get() == Material.RED_DYE) return;
            currentFilter.set(Material.RED_DYE);
            currentPage[0] = 0;

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
            this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);

        });

        this.setItem(5, new ItemBuilder(Material.LIGHT_BLUE_DYE).setDisplayName("<aqua>Included Items with description").setGlowing(currentFilter.get() == Material.LIGHT_BLUE_DYE).addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentFilter.get() == Material.LIGHT_BLUE_DYE) return;
            currentFilter.set(Material.LIGHT_BLUE_DYE);
            currentPage[0] = 0;

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
            this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);

        });

        this.setItem(6, new ItemBuilder(Material.GRAY_DYE).setDisplayName("<gray>Excluded Items").setGlowing(currentFilter.get() == Material.GRAY_DYE).addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentFilter.get() == Material.GRAY_DYE) return;
            currentFilter.set(Material.GRAY_DYE);
            currentPage[0] = 0;

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
            this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);

        });


        this.setItem(0, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("<dark_red>« <red>Previous Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentPage[0] != 0) {
                currentPage[0]--;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);

                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });
        this.setItem(8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_green>» <green>Next Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
            if(currentPage[0] < (pages.size() - 1)) {
                currentPage[0]++;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);

                this.setItems(9, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });


        this.setFilteredItems(forceItemBattle, currentFilter.get(), currentPage[0], pages);


        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    private void setFilteredItems(ForceItemBattle forceItemBattle, Material currentFilter, int currentPage, HashMap<Integer, HashMap<Integer, ItemStack>> pages) {
        int startSlot = 9;
        int initialPages = 0;
        int items = 0;

        HashMap<Integer, ItemStack> itemStackHashMap = new HashMap<>();
        pages.clear();
        for (Material materials : Material.values()) {
            if(materials.isItem() && !materials.isAir()) {
                if(currentFilter == Material.LIME_DYE) {
                    if (!pages.containsKey(initialPages)) {
                        itemStackHashMap = new HashMap<>();
                        pages.put(initialPages, itemStackHashMap);
                    }

                    itemStackHashMap.put(startSlot, new ItemBuilder(materials).setGlowing((forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials))).setLoreLegacy(forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(materials)).addItemFlags(ItemFlag.values()).getItemStack());

                    startSlot++;

                    if(startSlot == this.getInventory().getSize()) {
                        startSlot = 9;
                        initialPages++;
                    }

                    items++;

                } else if(currentFilter == Material.ORANGE_DYE) {
                    if (forceItemBattle.getItemDifficultiesManager().itemInList(materials)) {
                        if (!pages.containsKey(initialPages)) {
                            itemStackHashMap = new HashMap<>();
                            pages.put(initialPages, itemStackHashMap);
                        }

                        itemStackHashMap.put(startSlot, new ItemBuilder(materials).setGlowing((forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials))).setLoreLegacy(forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(materials)).addItemFlags(ItemFlag.values()).getItemStack());

                        startSlot++;

                        if (startSlot == this.getInventory().getSize()) {
                            startSlot = 9;
                            initialPages++;
                        }

                        items++;
                    }

                } else if(currentFilter == Material.RED_DYE) {
                    if(Bukkit.getRecipesFor(new ItemStack(materials)).isEmpty()) {
                        if (!pages.containsKey(initialPages)) {
                            itemStackHashMap = new HashMap<>();
                            pages.put(initialPages, itemStackHashMap);
                        }

                        itemStackHashMap.put(startSlot, new ItemBuilder(materials).setGlowing(forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials)).setLoreLegacy(forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(materials)).addItemFlags(ItemFlag.values()).getItemStack());

                        startSlot++;

                        if(startSlot == this.getInventory().getSize()) {
                            startSlot = 9;
                            initialPages++;
                        }

                        items++;
                    }

                } else if(currentFilter == Material.LIGHT_BLUE_DYE) {
                    if(forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials) && forceItemBattle.getItemDifficultiesManager().itemHasDescription(materials)) {
                        if (!pages.containsKey(initialPages)) {
                            itemStackHashMap = new HashMap<>();
                            pages.put(initialPages, itemStackHashMap);
                        }

                        itemStackHashMap.put(startSlot, new ItemBuilder(materials).setGlowing(forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials)).setLoreLegacy(forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(materials)).addItemFlags(ItemFlag.values()).getItemStack());

                        startSlot++;

                        if(startSlot == this.getInventory().getSize()) {
                            startSlot = 9;
                            initialPages++;
                        }

                        items++;
                    }

                } else if(currentFilter == Material.GRAY_DYE) {
                    if(!forceItemBattle.getItemDifficultiesManager().itemInAllLists(materials)) {
                        if (!pages.containsKey(initialPages)) {
                            itemStackHashMap = new HashMap<>();
                            pages.put(initialPages, itemStackHashMap);
                        }

                        itemStackHashMap.put(startSlot, new ItemBuilder(materials).addItemFlags(ItemFlag.values()).getItemStack());

                        startSlot++;

                        if(startSlot == this.getInventory().getSize()) {
                            startSlot = 9;
                            initialPages++;
                        }

                        items++;
                    }
                }
            }

        }

        if (pages.isEmpty()) {
            pages.put(0, new HashMap<>());
        }

        if (pages.containsKey(currentPage)) {
            pages.get(currentPage).forEach(this::setItem);
        } else {
            getPlayer().sendMessage("No items to display on this page.");
        }
    }
}
