package forceitembattle.listener;

import forceitembattle.util.Text;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandShout;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.settings.preset.InvSettingsPresets;
import forceitembattle.util.Team;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class ChatListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        // Preset naming phase: capture the message as the preset name instead of chatting.
        if (InvSettingsPresets.namingPhase != null
                && InvSettingsPresets.namingPhase.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                GamePreset gamePreset = InvSettingsPresets.namingPhase.get(player.getUniqueId());
                gamePreset.setPresetName(message);
                new InvSettingsPresets(this.plugin, gamePreset, this.plugin.getSettings()).open(player);
                InvSettingsPresets.namingPhase.remove(player.getUniqueId());
            });
            return;
        }

        Team currentTeam = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId()).currentTeam();

        // Shout: always global.
        if (CommandShout.isShouting(player)) {
            Bukkit.broadcast(Text.of(
                    "<gold>" + player.getName() + " <dark_gray>» <white>" + message
            ));
            return;
        }

        // No team chat active -> global.
        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)
                || !this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM_CHAT)
                || currentTeam == null) {

            Bukkit.broadcast(Text.of(
                    "<gold>" + player.getName() + " <dark_gray>» <white>" + message
            ));
            return;
        }

        // Team chat.
        String teamMessage = "<green>Team</green> <gray>| <gold>" + player.getName() +
                " <dark_gray>» <white>" + message;

        currentTeam.getPlayers().forEach(fibPlayer -> {
            Player p = fibPlayer.player();
            if (p != null && p.isOnline()) {
                p.sendMessage(Text.of(teamMessage));
            }
        });
    }
}