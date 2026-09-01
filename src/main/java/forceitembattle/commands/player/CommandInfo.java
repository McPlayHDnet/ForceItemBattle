package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.DescriptionItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CommandInfo extends CustomCommand implements CustomTabCompleter {

    private static final List<String> MATERIALS = Arrays.stream(Material.values())
            .map(CustomMaterials::idOf)
            .sorted()
            .toList();

    public CommandInfo(ForceItemBattle plugin) {
        super(plugin, "info");
        setUsage("[item]");
        setDescription("Get information about an item");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (args.length == 1) {
            Material material = this.matchMaterial(args[0]);
            if (material == null) {
                player.sendMessage(Text.of("<red>Invalid item name"));
                return;
            }
            item = new ItemStack(material);

        } else if (this.plugin.getRoundPhase().roundRunning()) {
            ForceItemPlayer forceItemPlayer =
                    this.plugin.getRoster().participant(player.getUniqueId()).orElse(null);
            if (forceItemPlayer == null) {
                player.sendMessage(Text.of("<red>You are not playing, type /info [item] to get information about an item"));
                return;
            }
            // With no argument during a round, /info describes the force item being hunted -- not
            // whatever happens to be in hand.
            item = new ItemStack(forceItemPlayer.activeMaterial());
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(Text.of("<red>You need to hold an item in your hand!"));
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
        CustomMaterials customMaterial = CustomMaterials.byId(input);
        if (customMaterial != null) {
            return customMaterial.getMaterial();
        }
        return Material.matchMaterial(input.toLowerCase());
    }

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
