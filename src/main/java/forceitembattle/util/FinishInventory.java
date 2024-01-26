package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FinishInventory extends InventoryBuilder {


    public FinishInventory(Player targetPlayer, @Nullable Map<UUID, Integer> place, boolean firstTime) {
        super(9*6, "§8» §6Items §8● §7" + targetPlayer.getName());

        HashMap<Integer, ItemStack[]> pages = new HashMap<>();

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§6").addItemFlags(ItemFlag.values()).getItemStack());

        /* FILL */
        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());

        /* Found-Items */
        final int[] startSlot = {10};
        final int[] placedItems = {-1};
        final int[] pagesAmount = {1};

        final int[] currentPage = {0};

        if(firstTime) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    placedItems[0]++;

                    if(startSlot[0] == 53) {
                        //check if is even needed to create a new page
                        if(ForceItemBattle.getGamemanager().getItemList(targetPlayer).size() > 35) {
                            pagesAmount[0]++;
                            pages.put(pagesAmount[0], getInventory().getContents());
                            setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());
                            startSlot[0] = 10;
                            //setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                            //setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                        }
                    }


                    ForceItem forceItem = ForceItemBattle.getGamemanager().getItemList(targetPlayer).get(placedItems[0]);
                    setItem(startSlot[0], new ItemBuilder(forceItem.material()).setDisplayName(WordUtils.capitalize(forceItem.material().name().replace("_", " ").toLowerCase()) + " §8» §6" + forceItem.timeNeeded()).getItemStack());

                    Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1));

                    if(startSlot[0] == 16 || startSlot[0] == 25 || startSlot[0] == 34 || startSlot[0] == 43) startSlot[0] += 3;
                    else startSlot[0]++;

                    if(placedItems[0] == ForceItemBattle.getGamemanager().getItemList(targetPlayer).size() - 1) {

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                Bukkit.getOnlinePlayers().forEach(players -> {
                                    if(players.getOpenInventory().getTopInventory() == getInventory()) {
                                        players.closeInventory();
                                    }

                                    players.sendTitle(ForceItemBattle.getGamemanager().sortByValue(ForceItemBattle.getGamemanager().getScore(), true).size() + ". " + targetPlayer.getName(), "§6" + (placedItems[0] + 1) + " Items found", 15, 35, 15);

                                });

                                TextComponent placementText = new TextComponent(ForceItemBattle.getGamemanager().sortByValue(ForceItemBattle.getGamemanager().getScore(), true).size() + ". " + targetPlayer.getName() + " ");
                                TextComponent textComponent = new TextComponent("§8[§bInventory§8]");
                                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/result" + placementText.getText().substring(placementText.getText().lastIndexOf(". ") + 1)));

                                getPlayer().spigot().sendMessage(placementText, textComponent);

                                ForceItemBattle.getGamemanager().getScore().remove(targetPlayer.getUniqueId());
                                //place.remove(targetPlayer.getUniqueId());
                                ForceItemBattle.getGamemanager().savedInventory.put(targetPlayer.getUniqueId(), pages);
                            }
                        }.runTaskLater(ForceItemBattle.getInstance(), 60L);


                        cancel();
                    }

                }
            }.runTaskTimer(ForceItemBattle.getInstance(), 0L, 10L);
        } else {
            //Open Inventory beginning from the first page
            System.out.println(ForceItemBattle.getGamemanager().savedInventory.toString());
            this.getInventory().setContents(ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).get(currentPage[0]));
            if(currentPage[0] != 0) {
                setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                    currentPage[0]--;

                    this.getInventory().setContents(ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).get(currentPage[0]));

                    if(currentPage[0] != ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).size()) {
                        setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                    } else if(currentPage[0] != 0) {
                        setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                });
            }
            if(currentPage[0] != ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).size()) {

                setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                    currentPage[0]++;

                    this.getInventory().setContents(ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).get(currentPage[0]));

                    if(currentPage[0] != ForceItemBattle.getGamemanager().savedInventory.get(targetPlayer.getUniqueId()).size()) {
                        setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                    } else if(currentPage[0] != 0) {
                        setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                });
            }
        }

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    public int getMaxPages(int itemsFound) {
         int i = 0;

         if(itemsFound > 35 && itemsFound <= 70) i = 1;
         else if(itemsFound > 70 && itemsFound <= 105) i = 2;
         else if(itemsFound > 105 && itemsFound <= 140) i = 3;
         else if(itemsFound > 140 && itemsFound <= 175) i = 4;
         else if(itemsFound > 175 && itemsFound <= 210) i = 5;

         return i;
    }
}
