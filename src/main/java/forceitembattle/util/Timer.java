package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Timer {

    private final ForceItemBattle forceItemBattle;
    /**
     * Time left until the game end (seconds).
     */
    @Setter
    @Getter
    private int timeLeft;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();

    public Timer(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.timeLeft = this.forceItemBattle.getConfig().getInt("timer.time");
        } else {
            this.timeLeft = 0;
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
                if(this.forceItemBattle.getGamemanager().isPausedGame()) {
                    Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500));
                    Title timeLeftTitle = Title.title(Component.empty(), forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>Game is paused!"), times);
                    player.showTitle(timeLeftTitle);

                }
                player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gray>Timer <red><b>paused</red>"));
                continue;
            }

            if (this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());

                if(!forceItemPlayer.isSpectator()) {
                    Material material = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial();

                    player.playerListName(forceItemBattle.getGamemanager().getMiniMessage().deserialize(
                            (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? "<yellow>[" + forceItemPlayer.currentTeam().getTeamDisplay() + "] " : "") + "<white>" +
                                    player.getName() + " <gray>[<gold>" + this.forceItemBattle.getGamemanager().getMaterialName(material) + " <reset><color:#4e5c24>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(true, material) + "<gray>]"));

                    player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(
                            "<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTimeLeft()) + "</b> <dark_gray>| " +
                                    (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? "<green>Team score: <white>" + forceItemPlayer.currentTeam().getCurrentScore() : "<green>Your score: <white>" + forceItemPlayer.currentScore())));

                    String bossBarTitle = "<gradient:#6eee87:#5fc52e><b>" + this.forceItemBattle.getGamemanager().getMaterialName(material) + " <reset><color:#4e5c24>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, material);
                    String chainBossTitle = null;

                    if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.CHAIN)) {
                        Material nextMaterial = forceItemPlayer.getNextMaterial();
                        chainBossTitle = "<gradient:#6eee87:#5fc52e><b>" + this.forceItemBattle.getGamemanager().getMaterialName(nextMaterial) + " <reset><color:#4e5c24>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, nextMaterial);
                    }

                    String finalBossBar = bossBarTitle + (chainBossTitle != null ? " <gray><b>➡</b> " + chainBossTitle : "");

                    try {
                        BossBar bar = this.bossBar.get(player.getUniqueId());
                        bar.name(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(finalBossBar));
                        player.showBossBar(bar);
                    } catch (NullPointerException e) {
                        BossBar bar = BossBar.bossBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(finalBossBar), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
                        player.showBossBar(bar);
                        this.bossBar.put(player.getUniqueId(), bar);
                    }
                } else {
                    player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTimeLeft()) + "</b> <dark_gray>| <gold>SPEC"));
                }

            } else {
                player.sendActionBar(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTimeLeft()) + "</b> <dark_gray>| <gold>SPEC"));

            }
        }
    }

    public void save() {
        this.forceItemBattle.getConfig().set("timer.time", timeLeft);
    }

    private void run() {
        new BukkitRunnable() {
            @Override
            public void run() {

                sendActionBar();
                if (!forceItemBattle.getGamemanager().isMidGame()) {
                    return;
                }
                setTimeLeft(getTimeLeft() - 1);

                switch (getTimeLeft()) {
                    case 300: {
                        Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                        Title timeLeftTitle = Title.title(Component.empty(), forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>5 minutes left"), times);
                        Bukkit.getOnlinePlayers().forEach(
                                players -> {
                                    players.showTitle(timeLeftTitle);
                                    players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                                }
                        );
                        break;
                    }
                    case 60: {
                        Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                        Title timeLeftTitle = Title.title(Component.empty(), forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>1 minute left"), times);
                        Bukkit.getOnlinePlayers().forEach(
                                players -> {
                                    players.showTitle(timeLeftTitle);
                                    players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                                }
                        );
                        break;
                    }
                    case 30, 10: {
                        Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                        Title timeLeftTitle = Title.title(Component.empty(), forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>" + getTimeLeft() + " seconds left"), times);
                        Bukkit.getOnlinePlayers().forEach(
                                players -> {
                                    players.showTitle(timeLeftTitle);
                                    players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                                }
                        );
                        break;
                    }
                    case 5, 4, 3, 2, 1: {
                        Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                        Title timeLeftTitle = Title.title(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>" + getTimeLeft()), Component.empty(), times);
                        Bukkit.getOnlinePlayers().forEach(
                                players -> {
                                    players.showTitle(timeLeftTitle);
                                    players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                                }
                        );
                        break;
                    }
                    default:
                        break;
                }
                if (getTimeLeft()<=0) {
                    Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                    Title gameDoneTitle = Title.title(Component.empty(), forceItemBattle.getGamemanager().getMiniMessage().deserialize("<white>» <gold>Force Item Battle is over! <white>«"), times);
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
                        player.showTitle(gameDoneTitle);
                    });
                    forceItemBattle.getGamemanager().finishGame();
                    forceItemBattle.logToFile("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }

}
