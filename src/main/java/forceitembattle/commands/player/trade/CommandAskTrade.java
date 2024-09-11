package forceitembattle.commands.player.trade;

import forceitembattle.commands.CustomCommand;
import forceitembattle.listener.Listeners;
import forceitembattle.manager.TradingManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;

public class CommandAskTrade extends CustomCommand {

    public CommandAskTrade() {
        super("asktrade");
        setDescription("Ask if someone has an item you/your team needs");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You don't have permission to use this command."));
            return;
        }
        if(!this.plugin.getGamemanager().isMidGame()) {

            return;
        }
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Material currentMaterial = forceItemPlayer.getCurrentMaterial();
        String materialName =  this.plugin.getGamemanager().getMaterialName(currentMaterial);

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(TradingManager.PREFIX + "<gray>You asked for <dark_aqua>" + materialName));

        Bukkit.getOnlinePlayers().forEach(players -> {
            if(players == player) return;
            players.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(TradingManager.PREFIX + "<yellow>" + player.getName() + " <gray>is looking for <dark_aqua>" + materialName + " <dark_gray>» <click:run_command:/trade " + player.getName() + "><dark_gray>[<aqua>Request a trade<dark_gray>]"));

            if(Listeners.hasItemInInventory(players.getInventory(), currentMaterial)) {
                Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(500));
                Title pauseTitle = Title.title(Component.empty(), plugin.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You have an item that <yellow>" + player.getName() + " <dark_aqua>wants"), times);

                players.showTitle(pauseTitle);

                //TODO: trading inventory
                //TODO: right know its fine just to notify a player when he has an item that someone else needs
            }
        });
    }
}
