package forceitembattle.util;

import java.util.Objects;
import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvPresetMenu;
import forceitembattle.settings.preset.InvSettingsPresets;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

public class InvSettings extends InventoryBuilder {

    private final ForceItemBattle plugin;
    private final GamePreset gamePreset;
    private int currentPage;

    public InvSettings(ForceItemBattle plugin, GamePreset gamePreset) {
        super(9 * 4, Text.of("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Menu"));

        this.plugin = plugin;
        this.gamePreset = gamePreset;
        this.currentPage = 0;


        this.addUpdateHandler(this::updateInventory);
    }

    private int totalPages() {
        return (int) Math.ceil(GameSetting.values().length / 7.0);
    }

    private String getPageButton(int slot, int currentPage) {
        String headValue = "";
        if (slot == 27) {
            if (currentPage == 0) {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=";
            } else {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
            }
        } else if (slot == 35) {
            if (currentPage == this.totalPages() - 1) {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=";
            } else {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
            }
        }
        return headValue;
    }

    private void updateInventory() {
        this.setItems(0, getInventory().getSize() - 1, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("<gray>").addItemFlags(ItemFlag.values()).getItemStack());

        int itemsPerPage = 7;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage - 1, GameSetting.values().length - 1);


        this.setItem(27, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture(this.getPageButton(27, this.currentPage))
                        .setDisplayName("<dark_red>« <red>Previous page")
                        .getItemStack(),
                inventoryClickEvent -> {
                    if (this.currentPage > 0) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                        this.currentPage--;
                    } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);

                }
        );

        this.setItem(35, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture(this.getPageButton(35, this.currentPage))
                        .setDisplayName("<dark_green>» <green>Next page")
                        .getItemStack(),
                inventoryClickEvent -> {
                    if (this.currentPage < this.totalPages() - 1) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                        this.currentPage++;
                    } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                }
        );

        if (gamePreset != null) {
            this.setItem(8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_gray>» <green>Save settings").getItemStack(), inventoryClickEvent -> {
                this.getPlayer().playSound(this.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                new InvSettingsPresets(plugin, gamePreset, plugin.getSettings()).open(this.getPlayer());
            });

        } else {
            if (this.getPlayer().isOp()) {
                this.setItem(8, new ItemBuilder(Material.STRUCTURE_VOID).setDisplayName("<dark_gray>» <yellow>Manage presets").getItemStack(), inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    new InvPresetMenu(plugin, plugin.getSettings()).open(this.getPlayer());
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
            if (gamePreset != null) {
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