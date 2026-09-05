package forceitembattle.gui;

import forceitembattle.model.Roster;
import forceitembattle.settings.GamePreset;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

public final class SettingsPresetsInventory extends InventoryBuilder {

    public static HashMap<UUID, GamePreset> namingPhase = new HashMap<>();

    public SettingsPresetsInventory(Roster roster, GameSettings gameSettings, GamePreset gamePreset) {
        super(9 * 5, Text.of("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Presets"));

        this.setItems(0, 8, GuiItems.border());
        this.setItems(36, 44, GuiItems.border());

        this.addUpdateHandler(() -> {

            List<String> lore = new ArrayList<>();

            this.setItem(19, new ItemBuilder(Material.NAME_TAG)
                    .setDisplayName("<dark_gray>● <green>Preset Name <dark_gray>» " + (gamePreset.getPresetName().isEmpty() ? "<red>Not set" : "<dark_aqua>" + gamePreset.getPresetName()))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                namingPhase.put(getPlayer().getUniqueId(), gamePreset);
                getPlayer().closeInventory();
                getPlayer().sendMessage(Text.of("<dark_aqua>Send your desired preset-name in chat"));
            });

            this.setItem(21, new ItemBuilder(Material.CLOCK)
                    .setDisplayName("<dark_gray>● <green>Time <dark_gray>» <dark_aqua>" + gamePreset.getCountdown())
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setCountdown(gamePreset.getCountdown() + (event.isRightClick() ? -5 : 5));
            });


            lore.add("");
            for (GameSetting defaultGameSettings : GameSetting.values()) {
                lore.add("  <dark_gray>● <gray>" + defaultGameSettings.displayName() + " <dark_gray>» " + (gamePreset.getGameSettings().contains(defaultGameSettings) ? "<dark_green>✔" : "<dark_red>✘"));
            }
            lore.add("");
            this.setItem(23, new ItemBuilder(Material.STRUCTURE_VOID)
                    .setDisplayName("<dark_gray>● <green>Settings")
                    .setLore(lore)
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                new SettingsInventory(roster, gameSettings, gamePreset).open(this.getPlayer());
            });
            lore.clear();


            this.setItem(25, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("<dark_gray>● <green>Joker <dark_gray>» <dark_aqua>" + gamePreset.getJokers())
                    .getItemStack(), event -> {

                if (gamePreset.getJokers() == 64 || gamePreset.getJokers() == 0) {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    this.getPlayer().sendMessage(Text.of("<red>You reached the end of possible jokers."));
                    return;

                }

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setJokers(gamePreset.getJokers() + (event.isRightClick() ? -1 : 1));

            });

            this.setItem(44, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setDisplayName("<dark_gray>● <green>Save & create preset")
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                gameSettings.addGamePreset(gamePreset);
                new PresetMenuInventory(roster, gameSettings).open(getPlayer());
            });
        });
    }
}
