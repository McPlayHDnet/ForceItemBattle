package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

import java.time.Duration;
public class CommandPause extends CustomCommand {

    public CommandPause() {
        super("pause");
        setDescription("Pause the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The timer is already paused."));
            return;
        }
        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>The game has been paused!"));
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.plugin.getGamemanager().setCurrentGameState(GameState.PAUSED_GAME);
    }
}
