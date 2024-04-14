package forceitembattle.settings.preset;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class InvPresetMenu extends InventoryBuilder {

    public InvPresetMenu(ForceItemBattle forceItemBattle, GameSettings gameSettings) {
        super(9*5, forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Presets"));

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(36, 44, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            this.setItem(4, new ItemBuilder(Material.STRUCTURE_VOID)
                    .setDisplayName("<dark_gray>● <green>Create a new preset")
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                GamePreset tempGamePreset = new GamePreset();
                new InvSettingsPresets(forceItemBattle, tempGamePreset, gameSettings).open(getPlayer());
            });

            List<String> lore = new ArrayList<>();
            gameSettings.gamePresetMap().forEach((presetName, preset) -> {
                lore.add("");
                lore.add("  <dark_gray>● <gray>Duration <dark_gray>» <green>" + preset.getCountdown() + " minutes");
                lore.add("  <dark_gray>● <gray>Joker <dark_gray>» <green>" + preset.getJokers());
                lore.add("  <dark_gray>● <gray>Backpack size <dark_gray>» <green>" + preset.getBackpackRows() * 9 + " slots");
                lore.add("");
                for(GameSetting gameSetting : GameSetting.values()) {
                    if(!(gameSetting.defaultValue() instanceof Integer)) {
                        lore.add("  <dark_gray>● <gray>" + gameSetting.displayName() + " <dark_gray>» " + (gameSettings.isSettingEnabledInPreset(preset, gameSetting) ? "<dark_green>✔" : "<dark_red>✘"));
                    }
                }
                lore.add("");
                this.addItem(new ItemBuilder(Material.PAPER).setDisplayName("<dark_gray>● <dark_aqua>" + preset.getPresetName()).setLore(lore).getItemStack());
                lore.clear();
            });

        });
    }
}
