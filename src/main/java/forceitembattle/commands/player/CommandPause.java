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
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1000), Duration.ofMillis(500));
        Title pauseTitle = Title.title(Component.empty(), plugin.getGamemanager().getMiniMessage().deserialize("<red>The game has been pasued!"), times);
        Bukkit.getOnlinePlayers().forEach(players -> players.showTitle(pauseTitle));
        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>The game has been paused!"));
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.plugin.getGamemanager().setCurrentGameState(GameState.PAUSED_GAME);
    }
}
