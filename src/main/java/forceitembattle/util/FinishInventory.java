package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FinishInventory extends InventoryBuilder {

    private final Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();

    public FinishInventory(ForceItemBattle forceItemBattle, ForceItemPlayer targetPlayer, Integer place, boolean firstTime) {
        super(9*6, "§8» §6Items §8● §7XXXXXXXXXX");

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§6").addItemFlags(ItemFlag.values()).getItemStack());

        /* FILL */
        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());

        /* Found-Items */

        AtomicInteger currentPage = new AtomicInteger();

        if(firstTime) {
            new BukkitRunnable() {

                int startSlot = 10;
                int placedItems = -1;
                int pagesAmount = 0;

                final Map<Integer, ItemStack> slots = new HashMap<>();

                @Override
                public void run() {
                    placedItems++;

                    if(startSlot == 53) {
                        //check if is even needed to create a new page
                        if(targetPlayer.foundItems().size() > 35) {
                            pagesAmount++;
                            setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§8").addItemFlags(ItemFlag.values()).getItemStack());
                            startSlot = 10;
                        }
                    }


                    List<ForceItem> items = targetPlayer.foundItems();
                    if (items.isEmpty()) {
                        setItem(startSlot, new ItemBuilder(Material.BARRIER).setDisplayName("§cNo Items found").getItemStack());
                        placedItems = -1;
                    } else {
                        ForceItem forceItem = items.get(placedItems);
                        ItemStack itemStack = new ItemBuilder(forceItem.material()).setDisplayName(WordUtils.capitalize(forceItem.material().name().replace("_", " ").toLowerCase()) + (forceItem.usedSkip() ? " §c§lSKIPPED" : "") + " §8» §6" + forceItem.timeNeeded()).setGlowing(forceItem.usedSkip()).getItemStack();
                        setItem(startSlot, itemStack);
                        slots.put(startSlot, itemStack);
                        pages.put(pagesAmount, slots);
                    }

                    Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1));

                    if(startSlot == 16 || startSlot == 25 || startSlot == 34 || startSlot == 43) startSlot += 3;
                    else startSlot++;

                    if(placedItems >= targetPlayer.foundItems().size() - 1) {

                        if(pages.isEmpty()) pages.put(0, slots);

                        new BukkitRunnable() {

                            @Override
                            public void run() {

                                TextComponent placementText = new TextComponent(place + ". " + targetPlayer.player().getName() + " §8┃ §6" + (placedItems + 1) + " Items found §8» ");
                                TextComponent textComponent = new TextComponent("§8[§bInventory§8]");
                                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/result " + targetPlayer.player().getUniqueId()));

                                Bukkit.getOnlinePlayers().forEach(players -> {
                                    if(players.getOpenInventory().getTopInventory() == getInventory()) {
                                        players.closeInventory();
                                    }

                                    players.sendTitle(place + ". " + targetPlayer.player().getName(), "§6" + (placedItems + 1) + " Items found", 15, 35, 15);
                                });


                                getPlayer().spigot().sendMessage(placementText, textComponent);

                                forceItemBattle.getGamemanager().savedInventory.put(targetPlayer.player().getUniqueId(), pages);
                            }
                        }.runTaskLater(forceItemBattle, 100L);


                        cancel();
                    }

                }
            }.runTaskTimer(forceItemBattle, 0L, 10L);
        } else {
            //Open Inventory beginning from the first page

            Map<Integer, ItemStack> itemStacks = forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).get(currentPage.get());

            if(forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).isEmpty()) {
                this.setItem(10, new ItemBuilder(Material.BARRIER).setDisplayName("§cNo Items found").getItemStack());
            } else {
                this.placeItems(itemStacks);
            }

            if(forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).size() > 1) {

                setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                    currentPage.getAndIncrement();

                    this.placeItems(itemStacks);

                    if(currentPage.get() != forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).size()) {
                        setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                    if(currentPage.get() != 0) {
                        setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                });

                setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                    currentPage.getAndDecrement();

                    this.placeItems(itemStacks);

                    if(currentPage.get() != forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).size()) {
                        setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§aNext Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                    if(currentPage.get() != 0) {
                        setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cPrevious Page").addItemFlags(ItemFlag.values()).getItemStack());
                    }
                });
            }
        }

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    private void placeItems(Map<Integer, ItemStack> itemStacksPerPage) {
        itemStacksPerPage.forEach((this::setItem));
    }
}
