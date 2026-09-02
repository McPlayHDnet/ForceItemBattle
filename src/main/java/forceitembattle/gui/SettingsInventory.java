package forceitembattle.gui;

import forceitembattle.model.Roster;
import forceitembattle.settings.GamePreset;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.QuickieMode;
import forceitembattle.util.Text;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;

public final class SettingsInventory extends InventoryBuilder {

    private final Roster roster;
    private final GameSettings settings;
    private final GamePreset gamePreset;
    private int currentPage;

    public SettingsInventory(Roster roster, GameSettings settings, GamePreset gamePreset) {
        super(9 * 4, Text.of("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Menu"));

        this.roster = roster;
        this.settings = settings;
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
                new SettingsPresetsInventory(roster, settings, gamePreset).open(this.getPlayer());
            });

        } else {
            if (this.getPlayer().isOp()) {
                this.setItem(8, new ItemBuilder(Material.STRUCTURE_VOID).setDisplayName("<dark_gray>» <yellow>Manage presets").getItemStack(), inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    new PresetMenuInventory(roster, settings).open(this.getPlayer());
                });
            }

        }

        for (int i = startIndex; i <= endIndex; i++) {
            int slotIndex = i - startIndex + 10;
            GameSetting gameSetting = GameSetting.values()[i];
            String settingDisplayName = "<dark_gray>» <dark_aqua>" + gameSetting.displayName();
            this.setItem(slotIndex, new ItemBuilder(gameSetting.defaultMaterial()).setDisplayName(settingDisplayName).setLore(gameSetting.descriptionLore()).getItemStack(), inventoryClickEvent -> {

                if (gameSetting == GameSetting.TEAM) {
                    if (this.roster.players().size() < 4) {
                        this.getPlayer().sendMessage(Text.of("<red>There are not enough players online"));
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        return;
                    }
                }

                if (gameSetting == GameSetting.QUICKIE) {
                    QuickieMode current = settings.getQuickieMode();
                    settings.setQuickieMode(inventoryClickEvent.isRightClick() ? current.previous() : current.next());
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    return;
                }

                if (gameSetting.defaultValue() instanceof Integer) {
                    return;
                }

                this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                toggleSetting(gamePreset, gameSetting);

            });

            ItemBuilder itemBuilder = null;
            String enabledPrefix = "<dark_gray>➟";
            if (gameSetting == GameSetting.QUICKIE) {
                QuickieMode quickieMode = settings.getQuickieMode();
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
                    }

                } else {
                    itemBuilder = new ItemBuilder(Material.RED_DYE).setDisplayName(enabledPrefix + " <red>Disabled <dark_red>✘");
                }
            } else {
                if (settings.isSettingEnabled(gameSetting)) {
                    itemBuilder = new ItemBuilder(Material.LIME_DYE).setDisplayName(enabledPrefix + " <green>Enabled <dark_green>✔");
                } else if (gameSetting.defaultValue() instanceof Integer) {
                    int amount = settings.getSettingValue(gameSetting);
                    itemBuilder = new ItemBuilder(Material.STONE_BUTTON).setAmount(amount).setDisplayName(enabledPrefix + " <yellow>" + amount + " <gray>" + (amount == 1 ? "row" : "rows"));
                } else {
                    itemBuilder = new ItemBuilder(Material.RED_DYE).setDisplayName(enabledPrefix + " <red>Disabled <dark_red>✘");
                }
            }

            Objects.requireNonNull(itemBuilder);
            this.setItem(slotIndex + 9, itemBuilder.getItemStack(), inventoryClickEvent -> {
                if (inventoryClickEvent.getCurrentItem() == null) return;

                if (gameSetting == GameSetting.QUICKIE) {
                    QuickieMode current = settings.getQuickieMode();
                    settings.setQuickieMode(inventoryClickEvent.isRightClick() ? current.previous() : current.next());
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    return;
                }

                if (inventoryClickEvent.getCurrentItem().getType() == Material.LIME_DYE || inventoryClickEvent.getCurrentItem().getType() == Material.RED_DYE) {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    toggleSetting(gamePreset, gameSetting);

                } else if (inventoryClickEvent.getCurrentItem().getType() == Material.STONE_BUTTON) {
                    // BACKPACKSIZE is the only stone-button setting.
                    if (!settings.isSettingEnabled(GameSetting.BACKPACK)) {
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
                            settings.setSettingValue(gameSetting, backpackSize);
                        }
                    }
                }

            });
        }
    }

    /**
     * Flips a boolean setting, on the preset being edited when there is one and on the live settings
     * otherwise. Both the name row and the status row below it toggle, so both call this.
     */
    private void toggleSetting(GamePreset gamePreset, GameSetting gameSetting) {
        if (gamePreset != null) {
            if (gamePreset.getGameSettings().contains(gameSetting)) {
                gamePreset.getGameSettings().remove(gameSetting);
            } else {
                gamePreset.getGameSettings().add(gameSetting);
            }
            return;
        }

        settings.setSettingEnabled(gameSetting, !settings.isSettingEnabled(gameSetting));
        if (gameSetting == GameSetting.TEAM) {
            Bukkit.broadcast(settings.isSettingEnabled(GameSetting.TEAM)
                    ? Text.of("<red>Teams are now enabled. <dark_gray>» <white>/teams")
                    : Text.of("<red>Teams are now disabled."));
        }
    }
}
