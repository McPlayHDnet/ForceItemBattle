package forceitembattle.commands.admin;

import forceitembattle.PasteRSUploader;
import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Player;

public class CommandLogConsole extends CustomCommand {

    public CommandLogConsole() {
        super("log");

        setDescription("Create a pastebin with the current console log");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if(player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>Uploading logs, please wait..."));

            PasteRSUploader.uploadLog().thenAccept(player::sendMessage);
        }
    }
}
