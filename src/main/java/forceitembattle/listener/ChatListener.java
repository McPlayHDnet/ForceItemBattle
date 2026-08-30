package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
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

    private final ForceItemBattle plugin;

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
                new SettingsPresetsInventory(this.plugin, gamePreset, this.plugin.getSettings()).open(player);
                SettingsPresetsInventory.namingPhase.remove(player.getUniqueId());
            });
            return;
        }

        ForceItemPlayer fibPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (CommandShout.isShouting(player)) {
            Bukkit.broadcast(Text.of(
                    "<gold>" + player.getName() + " <dark_gray>» <white>" + message
            ));
            return;
        }

        // No team chat active -> global.
        // The TEAM setting was a third way of asking a question isInTeam() already answers, and
        // the null check beside it was there because the two could disagree.
        if (!fibPlayer.isInTeam()
                || !this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM_CHAT)) {

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
