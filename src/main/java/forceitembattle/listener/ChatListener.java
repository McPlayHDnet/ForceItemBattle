package forceitembattle.listener;

import forceitembattle.model.Roster;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import forceitembattle.commands.player.CommandShout;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GamePreset;
import forceitembattle.gui.SettingsPresetsInventory;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    /**
     * Still needed: this listener opens a GUI, and the GUI layer has not been swept. Its
     * collaborators are named below; this is the one thing left that is genuinely a plugin.
     */
    private final ForceItemBattle plugin;
    private final Roster roster;
    private final Gamemanager gamemanager;
    private final GameSettings settings;
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        // Preset naming phase: capture the message as the preset name instead of chatting.
        if (SettingsPresetsInventory.namingPhase != null
                && SettingsPresetsInventory.namingPhase.containsKey(player.getUniqueId())) {
            Scheduler.runSync(() -> {
                GamePreset gamePreset = SettingsPresetsInventory.namingPhase.get(player.getUniqueId());
                gamePreset.setPresetName(message);
                new SettingsPresetsInventory(this.plugin, gamePreset, this.settings).open(player);
                SettingsPresetsInventory.namingPhase.remove(player.getUniqueId());
            });
            return;
        }

        ForceItemPlayer fibPlayer = this.roster.get(player.getUniqueId());

        if (CommandShout.isShouting(player)) {
            Bukkit.broadcast(Text.of(
                    "<gold>" + player.getName() + " <dark_gray>» <white>" + message
            ));
            return;
        }

        // Null means no roster entry -- someone who joined mid-round. They are on no team, so global
        // is the right answer for them too.
        if (fibPlayer == null
                || !fibPlayer.isInTeam()
                || !this.settings.isSettingEnabled(GameSetting.TEAM_CHAT)) {

            Bukkit.broadcast(Text.of(
                    "<gold>" + player.getName() + " <dark_gray>» <white>" + message
            ));
            return;
        }

        String teamMessage = "<green>Team</green> <gray>| <gold>" + player.getName() +
                " <dark_gray>» <white>" + message;

        // squad() is the people this player's item and score belong to, which in a team game is
        // exactly the set team chat addresses.
        fibPlayer.squad().forEach(member -> {
            Player p = member.player();
            if (p != null && p.isOnline()) {
                p.sendMessage(Text.of(teamMessage));
            }
        });
    }
}
