package forceitembattle.fairplay;

import forceitembattle.commands.CustomCommand;
import forceitembattle.model.RoundPhase;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

@RequiredArgsConstructor
public class OpTransparencyListener implements Listener {

    // Only commands a non-op could not run are reported. Of those: /checkmods broadcasts itself, and the rest
    // cannot reveal or change the world. Private messages stay private even on a server that restricts them.
    private static final Set<String> UNREPORTED = Set.of("msg", "tell", "w", "teammsg", "tm", "help", "checkmods", "settings", "start");
    private static final int MAX_SHOWN_LENGTH = 200;

    private final RoundPhase roundPhase;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().isOp()) {
            report(event.getPlayer().getName(), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        report(senderName(event.getSender()), event.getCommand());
    }

    private void report(String who, String commandLine) {
        if (this.roundPhase.isEndGame()) {
            return;
        }

        String command = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        String label = command.split(" ", 2)[0].toLowerCase();
        Command resolved = Bukkit.getCommandMap().getCommand(label);
        if (resolved == null || UNREPORTED.contains(resolved.getName()) || !isOpOnly(resolved)) {
            return;
        }

        String shown = command.length() > MAX_SHOWN_LENGTH ? command.substring(0, MAX_SHOWN_LENGTH) + "…" : command;
        Bukkit.broadcast(Text.of(Prefix.FAIR_PLAY.toString())
                .append(Component.text(who, NamedTextColor.YELLOW))
                .append(Component.text(" used ", NamedTextColor.GRAY))
                .append(Component.text("/" + shown, NamedTextColor.WHITE)));
    }

    // Plugin commands gate through preconditions, not permission nodes; everything else through its node's default.
    private static boolean isOpOnly(Command command) {
        if (command instanceof PluginCommand pluginCommand && pluginCommand.getExecutor() instanceof CustomCommand custom) {
            return custom.isOpOnly();
        }
        String permission = command.getPermission();
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        for (String node : permission.split(";")) {
            Permission registered = Bukkit.getPluginManager().getPermission(node);
            PermissionDefault fallback = registered == null ? Permission.DEFAULT_PERMISSION : registered.getDefault();
            if (fallback.getValue(false)) {
                return false;
            }
        }
        return true;
    }

    private static String senderName(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender
                ? "Console"
                : sender.getName();
    }
}
