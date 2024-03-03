package forceitembattle.settings.preset;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.InvSettings;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvSettingsPresets extends InventoryBuilder {

    @Getter
    private static final Map<UUID, GamePreset> namingPhase = new HashMap<>();

    public InvSettingsPresets(ForceItemBattle forceItemBattle, GamePreset gamePreset, GameSettings gameSettings) {
        super(9 * 5, "§8» §3Settings §8● §7Presets");

        /* BORDER */
        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(36, 44, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("§f").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {

            List<String> lore = new ArrayList<>();

            /* Name-Preset */
            this.setItem(19, new ItemBuilder(Material.NAME_TAG)
                    .setDisplayName("§8● §aPreset Name §8» " + (gamePreset.getPresetName().isEmpty() ? "§cNot set" : "§3" + gamePreset.getPresetName()))
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                namingPhase.put(getPlayer().getUniqueId(), gamePreset);
                getPlayer().closeInventory();
                getPlayer().sendMessage("§3Send your desired preset-name in chat");
            });

            /* Timer-Preset */
            this.setItem(20, new ItemBuilder(Material.CLOCK)
                    .setDisplayName("§8● §aTime §8» §3" + gamePreset.getCountdown())
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setCountdown(gamePreset.getCountdown() + (event.isRightClick() ? -5 : 5));
            });


            /* Settings-Preset */
            lore.add("");
            for (GameSetting defaultGameSettings : GameSetting.values()) {
                lore.add("  §8● §7" + defaultGameSettings.displayName() + " §8» " + (gamePreset.getGameSettings().contains(defaultGameSettings) ? "§2✔" : "§4✘"));
            }
            lore.add("");
            this.setItem(22, new ItemBuilder(Material.STRUCTURE_VOID)
                    .setDisplayName("§8● §aSettings")
                    .setLore(lore)
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                new InvSettings(forceItemBattle, gamePreset).open(this.getPlayer());
            });
            lore.clear();


            /* Joker-Preset */
            this.setItem(24, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§8● §aJoker §8» §3" + gamePreset.getJokers())
                    .getItemStack(), event -> {

                if (gamePreset.getJokers() == 64 || gamePreset.getJokers() == 0) {
                    this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    this.getPlayer().sendMessage("§cYou reached the end of possible jokers.");
                    return;

                }

                getPlayer().playSound(getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                gamePreset.setJokers(gamePreset.getJokers() + (event.isRightClick() ? -1 : 1));

            });


            /* BackpackSize-Preset */
            this.setItem(25, new ItemBuilder(Material.BUNDLE)
                    .setDisplayName("§8● §aBackpack Slots §8» §3" + gamePreset.getBackpackSize())
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                getPlayer().sendMessage("§cNot changeable yet... (idk why tho)");

            });


            /* Save-Preset */
            this.setItem(44, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setDisplayName("§8● §aSave & create preset")
                    .getItemStack(), event -> {

                getPlayer().playSound(getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                gameSettings.addGamePreset(gamePreset);
                new InvPresetMenu(forceItemBattle, gameSettings).open(getPlayer());
            });
        });
    }
}
