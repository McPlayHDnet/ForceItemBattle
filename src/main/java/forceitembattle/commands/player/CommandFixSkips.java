package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CommandFixSkips extends CustomCommand {
    public CommandFixSkips() {
        super("fixskips");
        setDescription("Fix skips");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You can only use this during the game."));
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        boolean usingTeams = this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM);
        int remainingJokers = usingTeams ? forceItemPlayer.currentTeam().getRemainingJokers() : forceItemPlayer.remainingJokers();
        if (remainingJokers == 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You don't have any jokers left."));
            return;
        }

        ItemStack jokers = Gamemanager.getJokers(remainingJokers);
        Inventory backpack = this.plugin.getBackpack().getBackpackForPlayer(player);

        backpack.remove(Gamemanager.getJokerMaterial());
        if (usingTeams) {
            for (ForceItemPlayer teammate : forceItemPlayer.currentTeam().getPlayers()) {
                teammate.player().getInventory().remove(Gamemanager.getJokerMaterial());
            }

        } else {
            player.getInventory().remove(Gamemanager.getJokerMaterial());
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), jokers);
        } else {
            player.getInventory().addItem(jokers);
        }
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>Removed all duplicate jokers and gave you <white>" + jokers.getAmount() + "<yellow> jokers."));
    }
}
