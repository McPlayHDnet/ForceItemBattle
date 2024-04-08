package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.InvPresetMenu;
import forceitembattle.settings.preset.InvSettingsPresets;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

public class InvSettings extends InventoryBuilder {
    
    public InvSettings(ForceItemBattle plugin, GamePreset gamePreset) {
        super(9*6, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Menu"));

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            for(GameSetting gameSettings : GameSetting.values()) {
                String settingDisplayName = "<dark_gray>» ";
                if(gamePreset != null) {
                    settingDisplayName += (gamePreset.gameSettings().contains(gameSettings) ? "<green>" + gameSettings.displayName() + " <dark_green>✔" : "<red>" + gameSettings.displayName() + " <dark_red>✘");

                    this.setItem(53, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("<dark_gray>» <green>Save settings").getItemStack(), inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                        new InvSettingsPresets(plugin, gamePreset, plugin.getSettings()).open(this.getPlayer());
                    });

                } else {
                    settingDisplayName += (plugin.getSettings().isSettingEnabled(gameSettings) ? "<green>" + gameSettings.displayName() + " <dark_green>✔" : "<red>" + gameSettings.displayName() + " <dark_red>✘");

                    this.setItem(8, new ItemBuilder(Material.STRUCTURE_VOID).setDisplayName("<dark_gray>» <yellow>Manage presets").getItemStack(), inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        new InvPresetMenu(plugin, plugin.getSettings()).open(this.getPlayer());
                    });
                }
                this.setItem(gameSettings.defaultSlot(), new ItemBuilder(gameSettings.defaultMaterial()).setDisplayName(settingDisplayName).getItemStack(), inventoryClickEvent -> {

                    if(gameSettings == GameSetting.TEAM) {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        return;
                    }

                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    if(gamePreset != null) {
                        if(gamePreset.gameSettings().contains(gameSettings)) gamePreset.gameSettings().remove(gameSettings);
                        else gamePreset.gameSettings().add(gameSettings);
                    } else {
                        plugin.getSettings().setSettingEnabled(gameSettings, !plugin.getSettings().isSettingEnabled(gameSettings));
                    }

                });
            }

        });
    }
}
