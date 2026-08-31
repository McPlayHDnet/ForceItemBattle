package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CommandFixSkips extends CustomCommand {
    public CommandFixSkips(ForceItemBattle plugin) {
        super(plugin, "fixskips");
        setDescription("Fix skips");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>You can only use this during the game."));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        boolean silent = args.length > 0 && args[0].equalsIgnoreCase("-silent");

        ForceItemPlayer forceItemPlayer = this.plugin.getRoster().get(player.getUniqueId());
        if (forceItemPlayer == null) {
            if (!silent) {
                player.sendMessage(Text.of("<red>You are not playing."));
            }
            return;
        }

        int remainingJokers = forceItemPlayer.activeJokers();
        if (remainingJokers == 0) {
            if (!silent) {
                player.sendMessage(Text.of("<red>You don't have any jokers left."));
            }
            return;
        }

        ItemStack jokers = Gamemanager.getJokers(remainingJokers);
        Inventory backpack = this.plugin.getBackpackManager().getBackpackForPlayer(player);

        backpack.remove(Gamemanager.getJokerMaterial());

        // Everyone the joker pool belongs to: the team in a team game, just this player otherwise.
        // The branch this replaces asked the TEAM setting and then dereferenced currentTeam(), so
        // a player with no team in a round configured for teams NPE'd here -- and onRespawn runs
        // "/fixskips -silent", which made it a crash on respawn rather than on a command.
        for (ForceItemPlayer member : forceItemPlayer.squad()) {
            member.player().getInventory().remove(Gamemanager.getJokerMaterial());
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), jokers);
        } else {
            player.getInventory().addItem(jokers);
        }
        if (!silent) {
            player.sendMessage(Text.of("<yellow>Removed all duplicate jokers and gave you <white>" + jokers.getAmount() + "<yellow> jokers."));
        }
    }
}
