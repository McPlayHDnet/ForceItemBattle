package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;

public class FinishInventory extends InventoryBuilder {


    public FinishInventory(Player targetPlayer, Integer place, boolean firstTime) {
        super(9*6, "§8» §6Items §8● §7XXXXXXXXXX");

        HashMap<Integer, ItemStack[]> pages = new HashMap<>();

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).setDisplayName("§6").addItemFlags(ItemFlag.values()).getItemStack());

        /* FILL */
        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());

        /* Found-Items */

        if(firstTime) {
            new BukkitRunnable() {

                int startSlot = 10;
                int placedItems = -1;
                int pagesAmount = 1;

                @Override
                public void run() {
                    placedItems++;

                    if(startSlot == 53) {
                        //check if is even needed to create a new page
                        if(ForceItemBattle.getGamemanager().getItemList(targetPlayer).size() > 35) {
                            pagesAmount++;
                            pages.put(pagesAmount, getInventory().getContents());
                            setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());
                            startSlot = 10;
                            //setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                            //setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                        }
                    }


                    List<ForceItem> items = ForceItemBattle.getGamemanager().getItemList(targetPlayer);
                    if (items.isEmpty()) {
                        setItem(startSlot, new ItemBuilder(Material.BARRIER).setDisplayName("§cNo Items found").getItemStack());
                    } else {
                        ForceItem forceItem = items.get(placedItems);
                        setItem(startSlot, new ItemBuilder(forceItem.material()).setDisplayName(WordUtils.capitalize(forceItem.material().name().replace("_", " ").toLowerCase()) + (forceItem.usedSkip() ? " §c§lSKIPPED" : "") + " §8» §6" + forceItem.timeNeeded()).setGlowing(forceItem.usedSkip()).getItemStack());
                    }

                    Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1));

                    if(startSlot == 16 || startSlot == 25 || startSlot == 34 || startSlot == 43) startSlot += 3;
                    else startSlot++;

                    if(placedItems >= ForceItemBattle.getGamemanager().getItemList(targetPlayer).size() - 1) {

                        new BukkitRunnable() {

                            @Override
                            public void run() {

                                TextComponent placementText = new TextComponent(place + ". " + targetPlayer.getName() + " ");
                                TextComponent textComponent = new TextComponent("§8[§bInventory§8]");
                                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/result" + placementText.getText().substring(placementText.getText().lastIndexOf(". ") + 1)));

                                Bukkit.getOnlinePlayers().forEach(players -> {
                                    if(players.getOpenInventory().getTopInventory() == getInventory()) {
                                        players.closeInventory();
                                    }

                                    players.sendTitle(place + ". " + targetPlayer.getName(), "§6" + (placedItems + 1) + " Items found", 15, 35, 15);
                                });


                                getPlayer().spigot().sendMessage(placementText, textComponent);

                                ForceItemBattle.getGamemanager().getScore().remove(targetPlayer.getUniqueId());
                                ForceItemBattle.getGamemanager().savedInventory.put(targetPlayer.getUniqueId(), pages);
                            }
                        }.runTaskLater(ForceItemBattle.getInstance(), 100L);


                        cancel();
                    }

                }
            }.runTaskTimer(ForceItemBattle.getInstance(), 0L, 10L);
        } else {
            //Open Inventory beginning from the first page
            /* TODO
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
            }*/
        }

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }
}
