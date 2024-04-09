package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Timer {

    private final ForceItemBattle forceItemBattle;
    @Setter
    @Getter
    private int time;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();

    public Timer(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.time = this.forceItemBattle.getConfig().getInt("timer.time");
        } else {
            this.time = 0;
        }

        run();
    }

    public String formatSeconds(int inputSeconds) {
        int seconds = inputSeconds % 60;
        int minutes = (inputSeconds / 60) % 60;
        int hours = inputSeconds / 60 / 60;

        String time = "";
        if(hours != 0) time += hours + "h ";
        if(minutes != 0) time += minutes + "m ";
        if(seconds != 0) time += seconds + "s";

        return time;
    }

    public void sendActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!this.forceItemBattle.getGamemanager().isMidGame()) {
                player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>Timer paused</red>"));
                if(player.getLocation().getWorld() == null) continue;
                player.getLocation().getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 1);
                continue;
            }

            if (this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());

                player.playerListName(forceItemBattle.getGamemanager().getMiniMessage().deserialize(
                        (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? "<yellow>[#" + forceItemPlayer.currentTeam().getTeamId() + "] " : "") + "<white>" +
                        player.getName() + " <gray>[<gold>" + this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer) + " <reset><color:#4e5c24>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(true, forceItemPlayer.currentMaterial()) + "<gray>]"));

                player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(
                        "<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTime()) + "</b> <dark_gray>| " +
                                (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? "<green>Team score: <white>" + forceItemPlayer.currentTeam().getCurrentScore() : "<green>Your score: <white>" + forceItemPlayer.currentScore())));

                String material = this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer);

                String bossBarTitle = "<gradient:#6eee87:#5fc52e><b>" + material + " <reset><color:#4e5c24>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false,
                        (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial())
                );

                try {
                    BossBar bar = this.bossBar.get(player.getUniqueId());
                    bar.name(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(bossBarTitle));
                    player.showBossBar(bar);
                } catch (NullPointerException e) {
                    BossBar bar = BossBar.bossBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(bossBarTitle), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
                    player.showBossBar(bar);
                    this.bossBar.put(player.getUniqueId(), bar);
                }

            } else {
                player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTime()) + "</b> <dark_gray>| <gold>SPEC"));

            }
        }
    }

    public void save() {
        this.forceItemBattle.getConfig().set("timer.time", time);
    }

    private void run() {
        new BukkitRunnable() {
            @Override
            public void run() {

                sendActionBar();
                if (!forceItemBattle.getGamemanager().isMidGame()) {
                    return;
                }
                setTime(getTime() - 1);

                switch (getTime()) {
                    case 300: {
                        Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red><< 5 minutes left >>"));
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 60: {
                        Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red><< 1 minute left >>"));
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 30, 10: {
                        Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red><< 30 seconds left >>"));
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 5, 4, 3, 2: {
                        Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red><< " + getTime() + " seconds left >>"));
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 1: {
                        Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red><< 1 second left >>"));
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    default:
                        break;
                }
                if (getTime()<=0) {
                    Bukkit.broadcast(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gold><< Force Item Battle is over >>"));
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1));
                    forceItemBattle.getGamemanager().finishGame();
                    forceItemBattle.logToFile("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }

}
