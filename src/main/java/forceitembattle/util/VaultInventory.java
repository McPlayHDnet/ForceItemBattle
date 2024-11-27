package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static forceitembattle.util.RecipeInventory.CUSTOM_MATERIALS;

public class VaultInventory extends InventoryBuilder {

    private final ForceItemBattle plugin;

    public VaultInventory(ForceItemBattle plugin) {
        super(9 * 5, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_green>Vault"));

        this.plugin = plugin;

        this.setItems(0, 8, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(this.getInventory().getSize() - 9, this.getInventory().getSize() - 1, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        this.setItems(9, 17, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(27, 35, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());

        this.setItem(13, new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ3MmM5ZDYyOGJiMzIyMWVmMzZiNGNiZDBiOWYxNWVkZDU4ZTU4NjgxODUxNGQ3ZTgyM2Q1NWM0OGMifX19")
                .setDisplayName("<gray>")
                .addItemFlags(ItemFlag.values())
                .getItemStack());

        this.addUpdateHandler(() -> {
            List<Material> itemList = plugin.getItemDifficultiesManager().getAvailableItems();
            Collections.shuffle(itemList);

            new BukkitRunnable() {
                int ticks = 0;
                int currentIndex = 0;
                final int totalDuration = 315;
                double accumulatedTime = 0;

                @Override
                public void run() {
                    if (ticks >= totalDuration) {
                        this.cancel();

                        Material wonMaterial = Objects.requireNonNull(getInventory().getItem(22)).getType();
                        Component subTitle = plugin.getGamemanager().getMiniMessage().deserialize("<gold>" + plugin.getGamemanager().getMaterialName(wonMaterial));

                        Title.Times times = Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(2000), Duration.ofMillis(600));
                        Title title = Title.title(Component.empty(), subTitle, times);

                        ItemStack itemStack = new ItemBuilder(wonMaterial).setDisplayName(CUSTOM_MATERIALS.get(wonMaterial) != null ? CUSTOM_MATERIALS.get(wonMaterial).displayName() : null).getItemStack();

                        getPlayer().showTitle(title);
                        getPlayer().playSound(getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        getPlayer().getInventory().addItem(itemStack);
                        if (!getPlayer().getInventory().contains(itemStack)) {
                            getPlayer().getWorld().dropItemNaturally(getPlayer().getLocation(), itemStack);
                        }
                        getPlayer().closeInventory();

                        return;
                    }

                    double progress = (double) ticks / totalDuration;
                    double easedProgress = easeOutCubic(progress);
                    double currentSpeed = Math.max(0.04, 1 - easedProgress);

                    accumulatedTime += currentSpeed;
                    if (accumulatedTime >= 1) {
                        for (int i = 18; i < 27; i++) {
                            setItem(i, null);
                        }

                        for (int i = 0; i < 9; i++) {
                            int itemIndex = (currentIndex + i) % itemList.size();
                            setItem(18 + i, new ItemBuilder(itemList.get(itemIndex)).setDisplayName(CUSTOM_MATERIALS.get(itemList.get(itemIndex)) != null ? CUSTOM_MATERIALS.get(itemList.get(itemIndex)).displayName() : null).getItemStack());
                        }

                        currentIndex = (currentIndex + 1) % itemList.size();
                        accumulatedTime -= 1;

                        getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    }

                    ticks++;
                }

                private double easeOutCubic(double x) {
                    return 1 - Math.pow(1 - x, 3);
                }
            }.runTaskTimer(this.plugin, 0L, 1L);
        });

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }
}
