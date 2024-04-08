package forceitembattle.settings.preset;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.InvSettings;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class InvSettingsPresets extends InventoryBuilder {

    public static HashMap<UUID, GamePreset> namingPhase = new HashMap<>();

    public InvSettingsPresets(ForceItemBattle forceItemBattle, GamePreset gamePreset, GameSettings gameSettings) {
        super(9*5, forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Settings <dark_gray>● <gray>Presets"));

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(36, 44, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            List<String> lore = new ArrayList<>();

            /* Name-Preset */
            this.setItem(19, new ItemBuilder(Material.NAME_TAG)
                    .setDisplayName("<dark_gray>● <green>Preset Name <dark_gray>» " + (gamePreset.presetName().isEmpty() ? "<red>Not set" : "<dark_aqua>" + gamePreset.presetName()))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                namingPhase.put(getPlayer().getUniqueId(), gamePreset);
                getPlayer().closeInventory();
                getPlayer().sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>Send your desired preset-name in chat"));
            });

            /* Timer-Preset */
            this.setItem(20, new ItemBuilder(Material.CLOCK)
                    .setDisplayName("<dark_gray>● <green>Time <dark_gray>» <dark_aqua>" + gamePreset.countdown())
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setCountdown(gamePreset.countdown() + (event.isRightClick() ? -5 : 5));
            });


            /* Settings-Preset */
            lore.add("");
            for(GameSetting defaultGameSettings : GameSetting.values()) {
                lore.add("  <dark_gray>● <gray>" + defaultGameSettings.displayName() + " <dark_gray>» " + (gamePreset.gameSettings().contains(defaultGameSettings) ? "<dark_green>✔" : "<dark_red>✘"));
            }
            lore.add("");
            this.setItem(22, new ItemBuilder(Material.STRUCTURE_VOID)
                    .setDisplayName("<dark_gray>● <green>Settings")
                    .setLore(lore)
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                new InvSettings(forceItemBattle, gamePreset).open(this.getPlayer());
            });
            lore.clear();


            /* Joker-Preset */
            this.setItem(24, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("<dark_gray>● <green>Joker <dark_gray>» <dark_aqua>" + gamePreset.jokers())
                    .getItemStack(), event -> {

                if(gamePreset.jokers() == 64 || gamePreset.jokers() == 0) {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    this.getPlayer().sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You reached the end of possible jokers."));
                    return;

                }

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setJokers(gamePreset.jokers() + (event.isRightClick() ? -1 : 1));

            });


            /* BackpackSize-Preset */
            this.setItem(25, new ItemBuilder(Material.BUNDLE)
                    .setDisplayName("<dark_gray>● <green>Backpack Slots <dark_gray>» <dark_aqua>" + gamePreset.backpackSize())
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                getPlayer().sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>Not changeable yet... (idk why tho)"));

            });


            /* Save-Preset */
            this.setItem(44, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setDisplayName("<dark_gray>● <green>Save & create preset")
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                gameSettings.addGamePreset(gamePreset);
                new InvPresetMenu(forceItemBattle, gameSettings).open(getPlayer());
            });
        });
    }
}
