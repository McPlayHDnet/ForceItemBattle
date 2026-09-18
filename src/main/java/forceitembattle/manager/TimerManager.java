package forceitembattle.manager;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.FileLogger;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import forceitembattle.util.TimeFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Drives the round clock once a second and renders it.
 *
 * <p>Everything here is presentation or plumbing. The rule — how much is left, and which seconds are
 * worth announcing — is {@link RoundClock}, which knows nothing about Bukkit.
 */
public class TimerManager implements Manager {

    private static final Title.Times TIMES =
            Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));

    /**
     * The paused-state title and action bar. Constant, and {@link #sendActionBar()} runs once a
     * second for every online player, so building and re-parsing them per player was pure waste.
     * Titles and Components are immutable, so one instance is safe to show to everybody.
     */
    private static final Title PAUSED_TITLE = Title.title(
            Component.empty(),
            Text.of("<red>Game is paused!"),
            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500)));

    private static final Component PAUSED_ACTION_BAR = Text.of("<gray>Timer <red><b>paused</red>");

    private final JavaPlugin plugin;
    private final Roster roster;
    private final RoundPhase roundPhase;
    private final GameSettings settings;
    private final Gamemanager gamemanager;
    private final ItemDifficultiesManager items;
    private final RandomEventManager randomEvents;
    private final TabListManager tabList;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();
    /** Driven here, owned by the plugin, so nothing has to reach through this class for the time. */
    private final RoundClock clock;
    private BukkitTask timerTask;

    public TimerManager(JavaPlugin plugin, RoundClock clock, Roster roster, RoundPhase roundPhase,
                        GameSettings settings, Gamemanager gamemanager, ItemDifficultiesManager items,
                        RandomEventManager randomEvents, TabListManager tabList) {
        this.plugin = plugin;
        this.roster = roster;
        this.roundPhase = roundPhase;
        this.settings = settings;
        this.gamemanager = gamemanager;
        this.items = items;
        this.randomEvents = randomEvents;
        this.tabList = tabList;
        this.clock = clock;
        if (this.plugin.getConfig().contains("timer.time")) {
            this.clock.setSecondsLeft(this.plugin.getConfig().getInt("timer.time"));
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

        this.plugin.reloadConfig();
        if (this.plugin.getConfig().getBoolean("isReset")) {
            this.plugin.getConfig().set("timer.time", 0);
        } else {
            this.save();
        }
        this.plugin.saveConfig();
    }

    public void setTimeLeft(int timeLeft) {
        this.clock.setSecondsLeft(timeLeft);
    }

    public void save() {
        this.plugin.getConfig().set("timer.time", this.clock.secondsLeft());
    }

    public void sendActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!this.roundPhase.roundRunning()) {
                if (this.roundPhase.isPausedGame()) {
                    player.showTitle(PAUSED_TITLE);
                }
                player.sendActionBar(PAUSED_ACTION_BAR);
                continue;
            }

            ForceItemPlayer forceItemPlayer = this.roster
                    .participant(player.getUniqueId())
                    .orElse(null);

            if (forceItemPlayer == null) {
                player.sendActionBar(Text.of("<gradient:#fcef64:#fcc44b:#f44c7d><b>"
                        + TimeFormat.humanised(this.clock.secondsLeft()) + "</b> <dark_gray>| <gold>SPEC"));
                continue;
            }

            this.sendPlayingHud(player, forceItemPlayer);
        }
    }

    private void sendPlayingHud(Player player, ForceItemPlayer forceItemPlayer) {
        Material material = forceItemPlayer.activeMaterial();

        String timeText = "<gradient:#fcef64:#fcc44b:#ff9e59><b>"
                + TimeFormat.humanised(this.clock.secondsLeft()) + "</b></gradient>";
        String scoreText = "";
        if (this.settings.isSettingEnabled(GameSetting.SCORE)) {
            // isInTeam(), not the TEAM setting: a spectator-turned-player holds no team and would
            // disagree with it.
            scoreText = "<dark_gray>| <green>" + (forceItemPlayer.isInTeam() ? "Team score: " : "Your score: ")
                    + "<white>" + forceItemPlayer.activeScore();
        }

        player.sendActionBar(Text.of(timeText + " " + scoreText));

        String bossBarTitle = this.itemLabel(material);
        if (this.settings.isSettingEnabled(GameSetting.CHAIN)) {
            bossBarTitle += " <gray><b>➡</b> " + this.itemLabel(forceItemPlayer.activeNextMaterial());
        }

        BossBar bar = this.bossBar.computeIfAbsent(player.getUniqueId(),
                uuid -> BossBar.bossBar(Component.empty(), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6));
        bar.name(Text.of(bossBarTitle));
        player.showBossBar(bar);
    }

    private String itemLabel(Material material) {
        return "<gradient:#6eee87:#5fc52e><b>" + CustomMaterials.nameOf(material)
                + " <reset><shadow:black:0.4>"
                + this.items.getUnicodeFromMaterial(false, material)
                + "</shadow>";
    }

    private void run() {
        this.timerTask = Scheduler.runTimerSync(new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBar();
                if (!TimerManager.this.roundPhase.roundRunning()) {
                    TimerManager.this.tabList.clearFooter();
                    return;
                }

                OptionalInt milestone = clock.tick();

                TimerManager.this.randomEvents.tick(clock.secondsLeft());
                announceUnlockedPools();

                // After the pool poll, so the footer countdown flips to "active" on the same tick the
                // unlock message is sent.
                TimerManager.this.tabList.update();

                milestone.ifPresent(TimerManager.this::announceMilestone);

                if (clock.expired()) {
                    announceRoundOver();
                    TimerManager.this.tabList.clearFooter();
                    TimerManager.this.gamemanager.finishGame();
                    FileLogger.log("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }, 20, 20);
    }

    private void announceUnlockedPools() {
        for (ItemDifficultiesManager.State unlockedPool :
                this.items.pollNewlyUnlockedStates()) {
            Bukkit.getOnlinePlayers().forEach(players -> {
                players.sendMessage(Text.of("<shadow:black:0><sprite:items:item/clock_00> <reset><gray>New item pool unlocked <dark_gray>» <"
                        + unlockedPool.getColor() + ">" + unlockedPool.getDisplayName()));
                players.playSound(players.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 0.8f);
            });
        }
    }

    /** The final five seconds put the bare number in the headline; above that, warning text. */
    private void announceMilestone(int secondsLeft) {
        Title title = RoundClock.isFinalCountdown(secondsLeft)
                ? Title.title(Text.of("<red>" + secondsLeft), Component.empty(), TIMES)
                : Title.title(Component.empty(), Text.of("<red>" + TimeFormat.countdownPhrase(secondsLeft)), TIMES);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        });
    }

    private void announceRoundOver() {
        Title gameDoneTitle = Title.title(Component.empty(),
                Text.of("<white>» <gold>Force Item Battle is over! <white>«"), TIMES);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
            player.showTitle(gameDoneTitle);
        });
    }
}
