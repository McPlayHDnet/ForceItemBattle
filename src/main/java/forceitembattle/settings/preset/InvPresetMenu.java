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
        super(9*5, "§8» §3Settings §8● §7Presets");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(36, 44, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            this.setItem(4, new ItemBuilder(Material.STRUCTURE_VOID)
                    .setDisplayName("§8● §aCreate a new preset")
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                GamePreset tempGamePreset = new GamePreset();
                new InvSettingsPresets(forceItemBattle, tempGamePreset, gameSettings).open(getPlayer());
            });

            List<String> lore = new ArrayList<>();
            gameSettings.gamePresetMap().forEach((presetName, preset) -> {
                lore.add("");
                lore.add("  §8● §7Duration §8» §a" + preset.getCountdown() + " minutes");
                lore.add("  §8● §7Joker §8» §a" + preset.getJokers());
                lore.add("  §8● §7Backpack size §8» §a" + preset.getBackpackSize() + " slots");
                lore.add("");
                for(GameSetting gameSetting : GameSetting.values()) {
                    lore.add("  §8● §7" + gameSetting.displayName() + " §8» " + (gameSettings.isSettingEnabledInPreset(preset, gameSetting) ? "§2✔" : "§4✘"));
                }
                lore.add("");
                this.addItem(new ItemBuilder(Material.PAPER).setDisplayName("§8● §3" + preset.getPresetName()).setLore(lore).getItemStack());
                lore.clear();
            });

        });
    }
}
