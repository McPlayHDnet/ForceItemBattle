package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.FileLogger;
import forceitembattle.util.Text;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.scheduler.BukkitTask;

public class TimerManager implements Manager {

    private final ForceItemBattle forceItemBattle;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();
    /**
     * Time left until the game end (seconds).
     */
    @Setter
    @Getter
    private int timeLeft;
    private BukkitTask timerTask;

    public TimerManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.timeLeft = this.forceItemBattle.getConfig().getInt("timer.time");
        } else {
            this.timeLeft = 0;
        }
    }

    @Override
    public void enable() {
        run();
    }

    @Override
    public void disable() {
        if (this.timerTask != null) {
            this.timerTask.cancel();
        }

        this.forceItemBattle.reloadConfig();
        if (this.forceItemBattle.getConfig().getBoolean("isReset")) {
            this.forceItemBattle.getConfig().set("timer.time", 0);
        } else {
            this.save();
        }
        this.forceItemBattle.saveConfig();
    }

    public String formatSeconds(int inputSeconds) {
        int seconds = inputSeconds % 60;
        int minutes = (inputSeconds / 60) % 60;
        int hours = inputSeconds / 60 / 60;

        String time = "";
        if (hours != 0) time += hours + "h ";
        if (minutes != 0) time += minutes + "m ";
        if (seconds != 0) time += seconds + "s";

        return time;
    }

    public void sendActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!this.forceItemBattle.getGamemanager().isMidGame()) {
                if (this.forceItemBattle.getGamemanager().isPausedGame()) {
                    Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500));
                    Title timeLeftTitle = Title.title(Component.empty(), Text.of("<red>Game is paused!"), times);
                    player.showTitle(timeLeftTitle);

                }
                player.sendActionBar(Text.of("<gray>Timer <red><b>paused</red>"));
                continue;
            }

            if (this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());

                if (!forceItemPlayer.isSpectator()) {
                    Material material = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial();

                    boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);
                    boolean scoreShown = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.SCORE);
                    String timeText = "<gradient:#fcef64:#fcc44b:#ff9e59><b>" + this.formatSeconds(this.getTimeLeft()) + "</b></gradient>";
                    String scoreText = "";
                    if (scoreShown) {
                        scoreText = teamMode
                                ? "<dark_gray>| <green>Team score: <white>" + forceItemPlayer.currentTeam().getCurrentScore()
                                : "<dark_gray>| <green>Your score: <white>" + forceItemPlayer.currentScore();
                    }

                    player.sendActionBar(
                            Text.of(
                                    timeText + " " + scoreText
                            )
                    );

                    String bossBarTitle = "<gradient:#6eee87:#5fc52e><b>" + this.forceItemBattle.getGamemanager().getMaterialName(material) +
                            " <reset><shadow:black:0.4>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, material) + "</shadow>";
                    String chainBossTitle = null;

                    if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.CHAIN)) {
                        Material nextMaterial = forceItemPlayer.getNextMaterial();
                        chainBossTitle = "<gradient:#6eee87:#5fc52e><b>" + this.forceItemBattle.getGamemanager().getMaterialName(nextMaterial) + " <reset><shadow:black:0.4>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, nextMaterial) + "</shadow>";
                    }

                    String finalBossBar = bossBarTitle + (chainBossTitle != null ? " <gray><b>➡</b> " + chainBossTitle : "");

                    try {
                        BossBar bar = this.bossBar.get(player.getUniqueId());
                        bar.name(Text.of(finalBossBar));
                        player.showBossBar(bar);
                    } catch (NullPointerException e) {
                        BossBar bar = BossBar.bossBar(Text.of(finalBossBar), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
                        player.showBossBar(bar);
                        this.bossBar.put(player.getUniqueId(), bar);
                    }
                } else {
                    player.sendActionBar(Text.of("<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTimeLeft()) + "</b> <dark_gray>| <gold>SPEC"));
                }

            } else {
                player.sendActionBar(Text.of("<gradient:#fcef64:#fcc44b:#f44c7d><b>" + this.formatSeconds(this.getTimeLeft()) + "</b> <dark_gray>| <gold>SPEC"));

            }
        }
    }

    public void save() {
        this.forceItemBattle.getConfig().set("timer.time", timeLeft);
    }

    private void run() {
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {

                sendActionBar();
                if (!forceItemBattle.getGamemanager().isMidGame()) {
                    forceItemBattle.getTabListManager().clearFooter();
                    return;
                }
                setTimeLeft(getTimeLeft() - 1);

                // Notify everyone in chat when a new item pool unlocks (once per pool).
                for (ItemDifficultiesManager.State unlockedPool :
                        forceItemBattle.getItemDifficultiesManager().pollNewlyUnlockedStates()) {
                    Bukkit.getOnlinePlayers().forEach(players -> {
                        players.sendMessage(Text.of("<shadow:black:0><sprite:items:item/clock_00> <gray>New item pool unlocked <dark_gray>» <" + unlockedPool.getColor() + ">" + unlockedPool.getDisplayName()));
                        players.playSound(players.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 0.8f);
                    });
                }

                // Refresh the tab footer after the pool poll so its pool countdown
                // flips to "active" on the same tick the unlock message is sent.
                forceItemBattle.getTabListManager().update();

                switch (getTimeLeft()) {
                    case 300: {
                        Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                        Title timeLeftTitle = Title.title(Component.empty(), Text.of("<red>5 minutes left"), times);
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
                        Title timeLeftTitle = Title.title(Component.empty(), Text.of("<red>1 minute left"), times);
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
                        Title timeLeftTitle = Title.title(Component.empty(), Text.of("<red>" + getTimeLeft() + " seconds left"), times);
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
                        Title timeLeftTitle = Title.title(Text.of("<red>" + getTimeLeft()), Component.empty(), times);
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
                if (getTimeLeft() <= 0) {
                    Title.Times times = Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));
                    Title gameDoneTitle = Title.title(Component.empty(), Text.of("<white>» <gold>Force Item Battle is over! <white>«"), times);
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
                        player.showTitle(gameDoneTitle);
                    });
                    forceItemBattle.getTabListManager().clearFooter();
                    forceItemBattle.getGamemanager().finishGame();
                    FileLogger.log("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }

}
