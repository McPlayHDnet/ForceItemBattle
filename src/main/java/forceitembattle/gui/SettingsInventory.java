package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.QuickieMode;
import forceitembattle.settings.GamePreset;
import forceitembattle.util.Text;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;

public class SettingsInventory extends InventoryBuilder {

    private final ForceItemBattle plugin;
    private final GamePreset gamePreset;
    private int currentPage;

    public SettingsInventory(ForceItemBattle plugin, GamePreset gamePreset) {
        super(9 * 4, Text.of("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Menu"));

        this.plugin = plugin;
        this.gamePreset = gamePreset;
        this.currentPage = 0;


        this.addUpdateHandler(this::updateInventory);
    }

    private int totalPages() {
        return (int) Math.ceil(GameSetting.values().length / 7.0);
    }

    private void updateInventory() {
        this.setItems(0, getInventory().getSize() - 1, GuiItems.filler());

        int itemsPerPage = 7;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage - 1, GameSetting.values().length - 1);

        boolean hasPrevious = this.currentPage > 0;
        boolean hasNext = this.currentPage < this.totalPages() - 1;

        this.setItem(27, GuiItems.pageBack(hasPrevious),
                inventoryClickEvent -> {
                    if (hasPrevious) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                        this.currentPage--;
                    } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);

                }
        );

        this.setItem(35, GuiItems.pageForward(hasNext),
                inventoryClickEvent -> {
                    if (hasNext) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                        this.currentPage++;
                    } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                }
        );

        if (gamePreset != null) {
            this.setItem(8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_gray>» <green>Save settings").getItemStack(), inventoryClickEvent -> {
                this.getPlayer().playSound(this.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                new SettingsPresetsInventory(plugin, gamePreset, plugin.getSettings()).open(this.getPlayer());
            });

        } else {
            if (this.getPlayer().isOp()) {
                this.setItem(8, new ItemBuilder(Material.STRUCTURE_VOID).setDisplayName("<dark_gray>» <yellow>Manage presets").getItemStack(), inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    new PresetMenuInventory(plugin, plugin.getSettings()).open(this.getPlayer());
                });
            }

        }

        for (int i = startIndex; i <= endIndex; i++) {
            int slotIndex = i - startIndex + 10;
            GameSetting gameSetting = GameSetting.values()[i];
            String settingDisplayName = "<dark_gray>» <dark_aqua>" + gameSetting.displayName();
            this.setItem(slotIndex, new ItemBuilder(gameSetting.defaultMaterial()).setDisplayName(settingDisplayName).setLore(gameSetting.descriptionLore()).getItemStack(), inventoryClickEvent -> {

                if (gameSetting == GameSetting.TEAM) {
                    if (plugin.getGamemanager().forceItemPlayerMap().size() < 4) {
                        this.getPlayer().sendMessage(Text.of("<red>There are not enough players online"));
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        return;
                    }
                }

                if (gameSetting == GameSetting.QUICKIE) {
                    QuickieMode current = plugin.getSettings().getQuickieMode();
                    plugin.getSettings().setQuickieMode(inventoryClickEvent.isRightClick() ? current.previous() : current.next());
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    return;
                }

                if (gameSetting.defaultValue() instanceof Integer) {
                    return;
                }

                this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                if (gamePreset != null) {
                    if (gamePreset.getGameSettings().contains(gameSetting))
                        gamePreset.getGameSettings().remove(gameSetting);
                    else gamePreset.getGameSettings().add(gameSetting);
                } else {
                    plugin.getSettings().setSettingEnabled(gameSetting, !plugin.getSettings().isSettingEnabled(gameSetting));
                    if (gameSetting == GameSetting.TEAM) {
                        if (plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                            Bukkit.broadcast(Text.of("<red>Teams are now enabled. <dark_gray>» <white>/teams"));
                        } else {
                            Bukkit.broadcast(Text.of("<red>Teams are now disabled."));
                        }
                    }
                }

            });

            ItemBuilder itemBuilder = null;
            String enabledPrefix = "<dark_gray>➟";
            if (gameSetting == GameSetting.QUICKIE) {
                QuickieMode quickieMode = plugin.getSettings().getQuickieMode();
                if (quickieMode.isEnabled()) {
                    itemBuilder = new ItemBuilder(Material.LIME_DYE).setDisplayName(enabledPrefix + " <green>" + quickieMode.displayName() + " <dark_green>✔");
                } else {
                    itemBuilder = new ItemBuilder(Material.RED_DYE).setDisplayName(enabledPrefix + " <red>Disabled <dark_red>✘");
                }
            } else if (gamePreset != null) {
                if (gamePreset.getGameSettings().contains(gameSetting)) {
                    itemBuilder = new ItemBuilder(Material.LIME_DYE).setDisplayName(enabledPrefix + " <green>Enabled <dark_green>✔");
                } else if (gameSetting.defaultValue() instanceof Integer) {
                    int amount = 0;
                    if (gameSetting == GameSetting.BACKPACKSIZE) {
                        amount = gamePreset.getBackpackRows();
                        itemBuilder = new ItemBuilder(Material.STONE_BUTTON).setAmount(amount).setDisplayName(enabledPrefix + " <yellow>" + amount + " <gray>" + (amount == 1 ? "row" : "rows"));
                    } else if (gameSetting == GameSetting.TRADING_COOLDOWN) {
                        amount = gamePreset.getTradingCooldown();
                        itemBuilder = new ItemBuilder(Material.STONE_BUTTON).setAmount(amount).setDisplayName(enabledPrefix + " <yellow>" + amount + " <gray>" + (amount == 1 ? "minute" : "minutes"));
                    }

                } else {
                    itemBuilder = new ItemBuilder(Material.RED_DYE).setDisplayName(enabledPrefix + " <red>Disabled <dark_red>✘");
                }
            } else {
                if (plugin.getSettings().isSettingEnabled(gameSetting)) {
                    itemBuilder = new ItemBuilder(Material.LIME_DYE).setDisplayName(enabledPrefix + " <green>Enabled <dark_green>✔");
                } else if (gameSetting.defaultValue() instanceof Integer) {
                    int amount = plugin.getSettings().getSettingValue(gameSetting);
                    itemBuilder = new ItemBuilder(Material.STONE_BUTTON).setAmount(amount).setDisplayName(enabledPrefix + " <yellow>" + amount + " <gray>" + (amount == 1 ? "row" : "rows"));
                } else {
                    itemBuilder = new ItemBuilder(Material.RED_DYE).setDisplayName(enabledPrefix + " <red>Disabled <dark_red>✘");
                }
            }

            Objects.requireNonNull(itemBuilder);
            this.setItem(slotIndex + 9, itemBuilder.getItemStack(), inventoryClickEvent -> {
                if (inventoryClickEvent.getCurrentItem() == null) return;

                if (gameSetting == GameSetting.QUICKIE) {
                    QuickieMode current = plugin.getSettings().getQuickieMode();
                    plugin.getSettings().setQuickieMode(inventoryClickEvent.isRightClick() ? current.previous() : current.next());
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    return;
                }

                if (inventoryClickEvent.getCurrentItem().getType() == Material.LIME_DYE || inventoryClickEvent.getCurrentItem().getType() == Material.RED_DYE) {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    if (gamePreset != null) {
                        if (gamePreset.getGameSettings().contains(gameSetting))
                            gamePreset.getGameSettings().remove(gameSetting);
                        else gamePreset.getGameSettings().add(gameSetting);
                    } else {
                        plugin.getSettings().setSettingEnabled(gameSetting, !plugin.getSettings().isSettingEnabled(gameSetting));
                        if (gameSetting == GameSetting.TEAM) {
                            if (plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                                Bukkit.broadcast(Text.of("<red>Teams are now enabled. <dark_gray>» <white>/teams"));
                            } else {
                                Bukkit.broadcast(Text.of("<red>Teams are now disabled."));
                            }
                        }
                    }

                } else if (inventoryClickEvent.getCurrentItem().getType() == Material.STONE_BUTTON) {
                    if (!plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK) || !plugin.getSettings().isSettingEnabled(GameSetting.TRADING)) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        return;
                    }
                    if (gameSetting == GameSetting.BACKPACKSIZE) {
                        int backpackSize = inventoryClickEvent.getCurrentItem().getAmount();

                        if (inventoryClickEvent.isLeftClick() && backpackSize < 6) {
                            backpackSize += 1;
                        } else if (inventoryClickEvent.isRightClick() && backpackSize > 1) {
                            backpackSize -= 1;
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                            return;
                        }
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        if (gamePreset != null) {
                            gamePreset.setBackpackRows(backpackSize);
                        } else {
                            plugin.getSettings().setSettingValue(gameSetting, backpackSize);
                        }

                    } else if (gameSetting == GameSetting.TRADING_COOLDOWN) {
                        int tradingCooldown = inventoryClickEvent.getCurrentItem().getAmount();

                        if (inventoryClickEvent.isLeftClick() && tradingCooldown < 5) {
                            tradingCooldown += 1;
                        } else if (inventoryClickEvent.isRightClick() && tradingCooldown > 1) {
                            tradingCooldown -= 1;
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                            return;
                        }
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        if (gamePreset != null) {
                            gamePreset.setTradingCooldown(tradingCooldown);
                        } else {
                            plugin.getSettings().setSettingValue(gameSetting, tradingCooldown);
                        }
                    }


                }

            });
        }
    }


}
