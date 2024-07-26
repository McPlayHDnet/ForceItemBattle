package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FinishInventory extends InventoryBuilder {

    private final Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();

    public FinishInventory(ForceItemBattle forceItemBattle, @Nullable ForceItemPlayer targetPlayer, @Nullable Team targetTeam, Integer place, boolean firstTime) {
        super(9*6, forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Items <dark_gray>● <gray>" + (firstTime ? "????????" : (targetTeam == null ? targetPlayer.player().getName() : "Team #" + targetTeam.getTeamId()))));

        /* TOP-BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        /* FILL */
        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());

        /* Found-Items */
        AtomicInteger currentPage = new AtomicInteger(0);

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
                        if((targetTeam != null && targetTeam.getFoundItems().size() > 35) || targetPlayer.foundItems().size() > 35) {
                            pages.put(pagesAmount, new HashMap<>(slots));
                            pagesAmount++;
                            startSlot = 10;
                            slots.clear();

                            setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());
                        }
                    }

                    List<ForceItem> items = (targetTeam != null ? targetTeam.getFoundItems() : targetPlayer.foundItems());
                    if (items.isEmpty()) {
                        setItem(startSlot, new ItemBuilder(Material.BARRIER).setDisplayName("<red>No Items found").getItemStack());
                        placedItems = -1;
                    } else {
                        ForceItem forceItem = items.get(placedItems);
                        ItemStack itemStack = new ItemBuilder(forceItem.material()).setDisplayName(WordUtils.capitalize(forceItem.material().name().replace("_", " ").toLowerCase()) + (forceItem.usedSkip() ? " <red><b>SKIPPED</b>" : "") + (forceItem.isBackToBack() ? " <aqua><b>B2B</b>" : "") + " <dark_gray>» <gold>" + forceItem.timeNeeded()).setGlowing(forceItem.usedSkip()).getItemStack();
                        setItem(startSlot, itemStack);
                        slots.put(startSlot, itemStack);
                    }

                    Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1));

                    if(startSlot == 16 || startSlot == 25 || startSlot == 34 || startSlot == 43) startSlot += 3;
                    else startSlot++;

                    if(placedItems >= (targetTeam != null ? (targetTeam.getFoundItems().size() - 1) : (targetPlayer.foundItems().size() - 1))) {

                        pages.put(pagesAmount, new HashMap<>(slots));

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                String placeColor = forceItemBattle.getGamemanager().placeColor(place);

                                for(Player players : Bukkit.getOnlinePlayers()) {
                                    if(players.getOpenInventory().getTopInventory() == getInventory()) {
                                        players.closeInventory();
                                    }

                                    Component mainTitle = forceItemBattle.getGamemanager().getMiniMessage().deserialize(placeColor + place + "<white>. " + (targetTeam == null ? targetPlayer.player().getName() : targetTeam.getPlayers().stream().map(name -> name.player().getName()).collect(Collectors.joining(", "))));
                                    Component subTitle = forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gold>" + (placedItems + 1) + " Items found");

                                    Title.Times times = Title.Times.times(Duration.ofMillis(750), Duration.ofMillis(1750), Duration.ofMillis(750));
                                    Title title = Title.title(mainTitle, subTitle, times);

                                    players.showTitle(title);
                                }
                                getPlayer().sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize(placeColor + place + "<white>. " + (targetTeam == null ? targetPlayer.player().getName() : targetTeam.getPlayers().stream().map(name -> name.player().getName()).collect(Collectors.joining(", "))) + " <dark_gray>┃ <gold>" + (placedItems + 1) + " Items found <dark_gray>» <click:run_command:/result " + (targetTeam == null ? targetPlayer.player().getUniqueId() : "#" + targetTeam.getTeamId()) + "><dark_gray>[<aqua>Inventory<dark_gray>]"));
                            }
                        }.runTaskLater(forceItemBattle, 100L);

                        if(targetTeam == null) forceItemBattle.getGamemanager().savedInventory.put(targetPlayer.player().getUniqueId(), pages);
                        else forceItemBattle.getGamemanager().savedInventoryTeam.put(targetTeam, pages);
                        cancel();
                    }

                }
            }.runTaskTimer(forceItemBattle, 0L, 10L);
        } else {
            //Open Inventory beginning from the first page
            this.addUpdateHandler(() -> {
                if(targetTeam == null) {
                    if(forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).isEmpty()) {
                        this.setItem(10, new ItemBuilder(Material.BARRIER).setDisplayName("<red>No Items found").getItemStack());
                    } else {
                        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());
                        this.placeItems(forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).get(currentPage.get()));
                    }

                    if(forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).size() > 1) {
                        if(currentPage.get() != forceItemBattle.getGamemanager().savedInventory.get(targetPlayer.player().getUniqueId()).size() - 1) {
                            setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_green>» <green>Next Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                                currentPage.getAndIncrement();

                                getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            });
                        }
                        if(currentPage.get() != 0) {
                            setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("<dark_red>« <red>Previous Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                                currentPage.getAndDecrement();

                                getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            });
                        }
                    }

                } else {

                    if(forceItemBattle.getGamemanager().savedInventoryTeam.get(targetTeam).isEmpty()) {
                        this.setItem(10, new ItemBuilder(Material.BARRIER).setDisplayName("<red>No Items found").getItemStack());
                    } else {
                        this.setItems(9, 53, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());
                        this.placeItems(forceItemBattle.getGamemanager().savedInventoryTeam.get(targetTeam).get(currentPage.get()));
                    }

                    if(forceItemBattle.getGamemanager().savedInventoryTeam.get(targetTeam).size() > 1) {
                        if(currentPage.get() != forceItemBattle.getGamemanager().savedInventoryTeam.get(targetTeam).size() - 1) {
                            setItem(35, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_green>» <green>Next Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                                currentPage.getAndIncrement();

                                getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            });
                        }
                        if(currentPage.get() != 0) {
                            setItem(27, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("<dark_red>« <red>Previous Page").addItemFlags(ItemFlag.values()).getItemStack(), inventoryClickEvent -> {
                                currentPage.getAndDecrement();

                                getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            });
                        }
                    }
                }


            });

        }

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    private void placeItems(Map<Integer, ItemStack> itemStacksPerPage) {
        itemStacksPerPage.forEach((this::setItem));
    }
}
