package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.util.CustomMaterial;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static forceitembattle.util.RecipeInventory.CUSTOM_MATERIALS;
import static forceitembattle.util.RecipeInventory.ID_TO_MATERIAL;

public class CommandInfo extends CustomCommand implements CustomTabCompleter {

    public CommandInfo() {
        super("info");
        setUsage("[item]");
        setDescription("Get information about an item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (args.length == 1) {
            Material material = this.matchMaterial(args[0]);
            if (material == null) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Invalid item name"));
                return;
            }
            item = new ItemStack(material);

        } else if (this.plugin.getGamemanager().isMidGame()) {
            if (this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                if(forceItemPlayer.isSpectator()) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You are not playing, type /info [item] to get information about an item"));
                    return;
                }
                item = new ItemStack(forceItemPlayer.getCurrentMaterial());
            } else {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You are not playing, type /info [item] to get information about an item"));
            }
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You need to hold an item in your hand!"));
            return;
        }

        DescriptionItem descriptionItem;
        if (this.plugin.getItemDifficultiesManager().itemHasDescription(item.getType())) {
            descriptionItem = this.plugin.getItemDifficultiesManager().getDescriptionItems().get(item.getType());
            if (descriptionItem.lines() != null) {
                this.plugin.getItemDifficultiesManager().getDescriptionItemLines(descriptionItem.material()).forEach(player::sendMessage);
            } else {
                throw new NullPointerException("The item description is either null or empty");
            }
        }

        this.plugin.getRecipeManager().createRecipeViewer(player, item);
    }

    private Material matchMaterial(String input) {
        Material material = ID_TO_MATERIAL.get(input.toLowerCase());

        if (material == null) {
            material = Material.matchMaterial(input.toLowerCase());
        }

        return material;
    }

    private static final List<String> MATERIALS = Arrays.stream(Material.values())
            .map(material -> {
                CustomMaterial customMaterial = CUSTOM_MATERIALS.get(material);
                if (customMaterial != null) {
                    return customMaterial.id();
                } else {
                    return material.name().toLowerCase();
                }
            })
            .sorted()
            .toList();

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String key = args[0].toLowerCase();
        int index = Collections.binarySearch(MATERIALS, key);
        if (index < 0) {
            index = -index - 1;
        }

        List<String> result = new ArrayList<>();
        while (index < MATERIALS.size()) {
            String s = MATERIALS.get(index);
            if (s.startsWith(key)) {
                result.add(s);
            } else {
                break;
            }
            index++;
        }
        return result;
    }
}
