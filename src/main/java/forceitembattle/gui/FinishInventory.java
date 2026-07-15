package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class FinishInventory extends InventoryBuilder {

    private static final int NEXT_PAGE_SLOT = 35;
    private static final int PREVIOUS_PAGE_SLOT = 27;
    private static final int FIRST_ITEM_SLOT = 10;

    private final Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();

    public FinishInventory(ForceItemBattle forceItemBattle, @Nullable ForceItemPlayer targetPlayer, @Nullable Team targetTeam, Integer place, boolean firstTime) {
        super(9 * 6, Text.of("<dark_gray>» <gold>Items <dark_gray>● <gray>" + (firstTime ? "????????" : (targetTeam == null ? targetPlayer.player().getName() : "Team " + targetTeam.getTeamDisplay()))));

        /* TOP-BORDER */
        this.setItems(0, 8, GuiItems.border());

        /* FILL */
        this.setItems(9, 53, GuiItems.filler());

        /* Found-Items */
        AtomicInteger currentPage = new AtomicInteger(0);

        if (firstTime) {
            boolean isEvent = forceItemBattle.getSettings().isSettingEnabled(GameSetting.EVENT);

            new BukkitRunnable() {

                final Map<Integer, ItemStack> slots = new HashMap<>();
                int startSlot = FIRST_ITEM_SLOT;
                int placedItems = -1;
                int pagesAmount = 0;

                @Override
                public void run() {
                    placedItems++;

                    if (startSlot == 53) {
                        //check if is even needed to create a new page
                        if ((targetTeam != null && targetTeam.getFoundItems().size() > 35) || targetPlayer.foundItems().size() > 35) {
                            pages.put(pagesAmount, new HashMap<>(slots));
                            pagesAmount++;
                            startSlot = FIRST_ITEM_SLOT;
                            slots.clear();

                            setItems(9, 53, GuiItems.filler());
                        }
                    }

                    List<ForceItem> items = (targetTeam != null ? targetTeam.getFoundItems() : targetPlayer.foundItems());
                    if (items.isEmpty()) {
                        setItem(startSlot, GuiItems.noItemsFound());
                        placedItems = -1;
                    } else {
                        ForceItem forceItem = items.get(placedItems);

                        String displayName = WordUtils.capitalizeFully(forceItem.material().name().replace("_", " "))
                                + " <dark_gray>» <gold>" + forceItem.timeNeeded();

                        List<String> lore = new ArrayList<>();

                        if (forceItem.usedSkip()) {
                            lore.add("");
                            lore.add("<dark_gray>[<red>Joker<dark_gray>]");
                        }
                        if (forceItem.back2Back().isActive()) {
                            lore.add("");
                            lore.add("<dark_gray>[<dark_aqua>B2B <dark_gray>» <aqua>" + forceItem.back2Back().getRarity() + "<dark_gray>]");
                        }

                        ItemStack itemStack = new ItemBuilder(forceItem.material())
                                .setDisplayName(displayName)
                                .setLore(lore)
                                .setGlowing(forceItem.usedSkip())
                                .getItemStack();
                        setItem(startSlot, itemStack);
                        slots.put(startSlot, itemStack);
                    }

                    Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.333F, 1));

                    if (startSlot == 16 || startSlot == 25 || startSlot == 34 || startSlot == 43) startSlot += 3;
                    else startSlot++;

                    if (placedItems >= (targetTeam != null ? (targetTeam.getFoundItems().size() - 1) : (targetPlayer.foundItems().size() - 1))) {

                        String teamDisplay = targetTeam == null ? targetPlayer.player().getName() :
                                targetTeam.getName() != null ? targetTeam.getName() :
                                        targetTeam.getPlayers()
                                                .stream()
                                                .map(name -> name.player().getName())
                                                .collect(Collectors.joining(", "));

                        String placeColor = forceItemBattle.getGamemanager().placeColor(place);

                        String chatMessage = placeColor + place + "<white>. " + teamDisplay + " <dark_gray>┃ <gold>" + (placedItems + 1) + " Items found " +
                                "<dark_gray>» <click:run_command:/result " + (targetTeam == null ? targetPlayer.player().getUniqueId() : "#" + targetTeam.getTeamId()) + "><dark_gray>[<aqua>Inventory<dark_gray>]";

                        pages.put(pagesAmount, new HashMap<>(slots));

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                clearCloseHandlers();

                                // just for safety in case there is a re-open in the very last tick... don't think this will ever happen but yeah why not
                                Bukkit.getScheduler().runTaskLater(forceItemBattle, () -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> {
                                        if (p.getOpenInventory().getTopInventory() == getInventory())
                                            p.closeInventory();
                                    });
                                }, 1L);

                                for (Player players : Bukkit.getOnlinePlayers()) {
                                    Component mainTitle = Text.of(placeColor + place + "<white>. " + teamDisplay);
                                    Component subTitle = Text.of("<gold>" + (placedItems + 1) + " Items found");

                                    Title.Times times = Title.Times.times(Duration.ofMillis(750), Duration.ofMillis(1750), Duration.ofMillis(750));
                                    Title title = Title.title(mainTitle, subTitle, times);

                                    players.showTitle(title);
                                }

                                getPlayer().sendMessage(Text.of(chatMessage));
                            }
                        }.runTaskLater(forceItemBattle, 100L);

                        forceItemBattle.getGamemanager().saveResultPages(targetPlayer, targetTeam, pages);
                        cancel();
                    }

                }
            }.runTaskTimer(forceItemBattle, 0L, isEvent ? 8L : 10L);
        } else {
            //Open Inventory beginning from the first page
            this.addUpdateHandler(() -> {
                Map<Integer, Map<Integer, ItemStack>> savedPages =
                        forceItemBattle.getGamemanager().getResultPages(targetPlayer, targetTeam);

                renderPage(savedPages, currentPage);
            });
        }

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
        if (firstTime) {
            this.addCloseHandler(event -> {
                Player player = (Player) event.getPlayer();

                Bukkit.getScheduler().runTaskLater(forceItemBattle, () -> {
                    if (!player.isOnline()) return;

                    player.openInventory(getInventory());
                }, 1L);
            });
        }
    }

    private void renderPage(@Nullable Map<Integer, Map<Integer, ItemStack>> savedPages, AtomicInteger currentPage) {
        if (savedPages == null || savedPages.isEmpty()) {
            this.setItem(FIRST_ITEM_SLOT, GuiItems.noItemsFound());
            return;
        }

        this.setItems(9, 53, GuiItems.filler());
        this.placeItems(savedPages.get(currentPage.get()));

        if (savedPages.size() <= 1) {
            return;
        }

        if (currentPage.get() != savedPages.size() - 1) {
            this.setItem(NEXT_PAGE_SLOT, GuiItems.nextPage(), event -> turnPage(currentPage, 1));
        }
        if (currentPage.get() != 0) {
            this.setItem(PREVIOUS_PAGE_SLOT, GuiItems.previousPage(), event -> turnPage(currentPage, -1));
        }
    }

    private void turnPage(AtomicInteger currentPage, int delta) {
        currentPage.addAndGet(delta);

        getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
    }

    private void placeItems(Map<Integer, ItemStack> itemStacksPerPage) {
        itemStacksPerPage.forEach((this::setItem));
    }
}
