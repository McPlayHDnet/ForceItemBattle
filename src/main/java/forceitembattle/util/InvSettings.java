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
        super(9*6, "§8» §3Settings §8● §7Menu");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            for(GameSetting gameSettings : GameSetting.values()) {
                String settingDisplayName = "§8» ";
                if(gamePreset != null) {
                    settingDisplayName += (gamePreset.getGameSettings().contains(gameSettings) ? "§a" + gameSettings.displayName() + " §2✔" : "§c" + gameSettings.displayName() + " §4✘");

                    this.setItem(53, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§8» §aSave settings").getItemStack(), inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                        new InvSettingsPresets(plugin, gamePreset, plugin.getSettings()).open(this.getPlayer());
                    });

                } else {
                    settingDisplayName += (plugin.getSettings().isSettingEnabled(gameSettings) ? "§a" + gameSettings.displayName() + " §2✔" : "§c" + gameSettings.displayName() + " §4✘");

                    this.setItem(8, new ItemBuilder(Material.STRUCTURE_VOID).setDisplayName("§8» §eManage presets").getItemStack(), inventoryClickEvent -> {
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
                        if(gamePreset.getGameSettings().contains(gameSettings)) gamePreset.getGameSettings().remove(gameSettings);
                        else gamePreset.getGameSettings().add(gameSettings);
                    } else {
                        plugin.getSettings().setSettingEnabled(gameSettings, !plugin.getSettings().isSettingEnabled(gameSettings));
                    }

                });
            }

        });
    }
}
