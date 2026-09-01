package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.ScoreOwner;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * One owner's turn in the ceremony, dealt out item by item.
 *
 * <p>Was half of {@code FinishInventory}, behind a {@code firstTime} boolean that switched between
 * two disjoint bodies: this one, which <em>builds</em> the pages, and {@link ResultScreen}, which
 * renders pages already built. Nothing was shared between the two branches except the border.
 *
 * <h2>Two callbacks, not one</h2>
 *
 * <p>{@code onPagesBuilt} fires when the last item lands and the ticker cancels. {@code
 * onRevealComplete} fires five seconds later, after the title. They are deliberately separate:
 * collapsing them would move the archive write to the later moment, and anyone clicking the
 * {@code [Inventory]} link during the title would open an empty screen.
 *
 * <p>The pages leave through a callback rather than being written here, so this class never touches
 * shared state — {@code FinishInventory} used to reach into {@code Gamemanager} and store them
 * itself.
 */
public final class ResultReveal extends InventoryBuilder {

    private static final int FIRST_ITEM_SLOT = 10;
    private static final int LAST_ITEM_SLOT = 53;
    private static final int PAGE_CAPACITY = 35;
    private static final long REVEAL_TO_TITLE_TICKS = 100L;

    private final Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();

    /**
     * @param onPagesBuilt     the finished pages, handed over the moment the reveal stops dealing
     * @param onRevealComplete run once the title and chat line went out, or null when this is not
     *                         the winner's turn
     */
    public ResultReveal(ForceItemBattle plugin,
                        ResultCeremony.Reveal reveal,
                        Consumer<Map<Integer, Map<Integer, ItemStack>>> onPagesBuilt,
                        @Nullable Runnable onRevealComplete) {
        super(9 * 6, Text.of("<dark_gray>» <gold>Items <dark_gray>● <gray>????????"));

        ScoreOwner owner = reveal.owner();
        int place = reveal.place();
        String displayName = ResultDisplay.nameOf(owner);
        boolean attributesCollectors = ResultDisplay.attributesCollectors(owner);

        this.setItems(0, 8, GuiItems.border());
        this.setItems(9, LAST_ITEM_SLOT, GuiItems.filler());

        boolean isEvent = plugin.getSettings().isSettingEnabled(GameSetting.EVENT);

        new BukkitRunnable() {

            final Map<Integer, ItemStack> slots = new HashMap<>();
            int startSlot = FIRST_ITEM_SLOT;
            int placedItems = -1;
            int pagesAmount = 0;

            @Override
            public void run() {
                placedItems++;

                List<ForceItem> items = owner.foundItems();

                if (startSlot == LAST_ITEM_SLOT && items.size() > PAGE_CAPACITY) {
                    pages.put(pagesAmount, new HashMap<>(slots));
                    pagesAmount++;
                    startSlot = FIRST_ITEM_SLOT;
                    slots.clear();

                    setItems(9, LAST_ITEM_SLOT, GuiItems.filler());
                }

                if (items.isEmpty()) {
                    setItem(startSlot, GuiItems.noItemsFound());
                    placedItems = -1;
                } else {
                    ItemStack itemStack = render(items.get(placedItems));
                    setItem(startSlot, itemStack);
                    slots.put(startSlot, itemStack);
                }

                Bukkit.getOnlinePlayers().forEach(viewer ->
                        viewer.playSound(viewer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.333F, 1));

                // The grid is six wide inside a nine-wide inventory, so three slots are skipped at
                // the end of every row.
                if (startSlot == 16 || startSlot == 25 || startSlot == 34 || startSlot == 43) {
                    startSlot += 3;
                } else {
                    startSlot++;
                }

                if (placedItems < items.size() - 1) {
                    return;
                }

                String placeColor = Text.placeColor(place);
                String chatMessage = placeColor + place + "<white>. " + displayName
                        + " <dark_gray>┃ <gold>" + (placedItems + 1) + " Items found "
                        + "<dark_gray>» <click:run_command:/result "
                        + ResultDisplay.resultArgumentFor(owner)
                        + "><dark_gray>[<aqua>Inventory<dark_gray>]";

                pages.put(pagesAmount, new HashMap<>(slots));

                announceLater(placeColor, displayName, placedItems + 1, chatMessage);

                onPagesBuilt.accept(pages);
                cancel();
            }

            private ItemStack render(ForceItem forceItem) {
                List<String> lore = new ArrayList<>();

                if (attributesCollectors) {
                    String collector = ResultDisplay.collectorName(owner, forceItem.collectedBy());
                    if (collector != null) {
                        lore.add("<dark_gray>» <gray>" + (forceItem.usedSkip() ? "Skipped" : "Found")
                                + " by <yellow>" + collector);
                    }
                }

                if (forceItem.usedSkip()) {
                    lore.add("");
                    lore.add("<dark_gray>[<red>Joker<dark_gray>]");
                }
                if (forceItem.back2Back().isActive()) {
                    lore.add("");
                    lore.add("<dark_gray>[<dark_aqua>B2B <dark_gray>» <aqua>"
                            + forceItem.back2Back().getRarity() + "<dark_gray>]");
                }

                return new ItemBuilder(CustomMaterials.itemStackOf(forceItem.material()))
                        .setDisplayName(CustomMaterials.nameOf(forceItem.material())
                                + " <dark_gray>» <gold>" + forceItem.timeNeeded())
                        .setLore(lore)
                        .setGlowing(forceItem.usedSkip())
                        .getItemStack();
            }

            private void announceLater(String placeColor, String name, int itemCount, String chatMessage) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        clearCloseHandlers();

                        // Guards against a re-open landing on the very last tick.
                        Scheduler.runLaterSync(() -> Bukkit.getOnlinePlayers().forEach(viewer -> {
                            if (viewer.getOpenInventory().getTopInventory() == getInventory()) {
                                viewer.closeInventory();
                            }
                        }), 1L);

                        Title.Times times = Title.Times.times(
                                Duration.ofMillis(750), Duration.ofMillis(1750), Duration.ofMillis(750));
                        Component mainTitle = Text.of(placeColor + place + "<white>. " + name);
                        Component subTitle = Text.of("<gold>" + itemCount + " Items found");

                        for (Player viewer : Bukkit.getOnlinePlayers()) {
                            viewer.showTitle(Title.title(mainTitle, subTitle, times));
                        }

                        getPlayer().sendMessage(Text.of(chatMessage));

                        if (onRevealComplete != null) {
                            onRevealComplete.run();
                        }
                    }
                }.runTaskLater(plugin, REVEAL_TO_TITLE_TICKS);
            }
        }.runTaskTimer(plugin, 0L, isEvent ? 8L : 10L);

        this.addClickHandler(event -> event.setCancelled(true));

        // Nobody closes their way out of the ceremony; it reopens until the reveal finishes.
        this.addCloseHandler(event -> {
            Player viewer = (Player) event.getPlayer();

            Scheduler.runLaterSync(() -> {
                if (viewer.isOnline()) {
                    viewer.openInventory(getInventory());
                }
            }, 1L);
        });
    }
}
