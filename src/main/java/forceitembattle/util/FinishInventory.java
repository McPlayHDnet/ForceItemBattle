package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FinishInventory extends InventoryBuilder {

    private final Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();

    public FinishInventory(ForceItemBattle forceItemBattle, ForceItemPlayer targetPlayer, Integer place, boolean firstTime) {
        super(9*6, forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Items <dark_gray>● <gray>XXXXXXXXXX"));

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        /* FILL */
        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());

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
                            setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());
                            startSlot = 10;
                        }
                    }


                    List<ForceItem> items = targetPlayer.foundItems();
                    if (items.isEmpty()) {
                        setItem(startSlot, new ItemBuilder(Material.BARRIER).setDisplayName("<red>No Items found").getItemStack());
                        placedItems = -1;
                    } else {
                        ForceItem forceItem = items.get(placedItems);
                        ItemStack itemStack = new ItemBuilder(forceItem.material()).setDisplayName(WordUtils.capitalize(forceItem.material().name().replace("_", " ").toLowerCase()) + (forceItem.usedSkip() ? " <red><b>SKIPPED</b>" : "") + " <dark_gray>» <gold>" + forceItem.timeNeeded()).setGlowing(forceItem.usedSkip()).getItemStack();
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

                                String placeColor = "<white>";
                                if(place == 3) placeColor = "<red>";
                                else if(place == 2) placeColor = "<gray>";
                                else if(place == 1) placeColor = "<gold>";

                                for(Player players : Bukkit.getOnlinePlayers()) {
                                    if(players.getOpenInventory().getTopInventory() == getInventory()) {
                                        players.closeInventory();
                                    }

                                    Component mainTitle = forceItemBattle.getGamemanager().getMiniMessage().deserialize(placeColor + place + "<white>. " + targetPlayer.player().getName());
                                    Component subTitle = forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gold>" + (placedItems + 1) + " Items found");

                                    Title.Times times = Title.Times.times(Duration.ofMillis(750), Duration.ofMillis(1750), Duration.ofMillis(750));
                                    Title title = Title.title(mainTitle, subTitle, times);

                                    players.showTitle(title);

                                }

                                getPlayer().sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize(placeColor + place + "<white>. " + targetPlayer.player().getName() + " <dark_gray>┃ <gold>" + (placedItems + 1) + " Items found <dark_gray>» <click:run_command:/result " + targetPlayer.player().getUniqueId() + "><dark_gray>[<aqua>Inventory<dark_gray>]"));

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
