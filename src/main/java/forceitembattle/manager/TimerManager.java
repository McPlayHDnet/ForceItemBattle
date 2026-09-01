package forceitembattle.manager;

import forceitembattle.model.Roster;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.RoundClock;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.FileLogger;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Drives the round clock once a second and renders it.
 *
 * <p>Everything here is presentation or plumbing: boss bars, action bars, titles, sounds, the
 * per-second fan-out to the other managers, and persisting the remaining time across a restart.
 * The rule — how much is left, and which seconds are worth announcing — is {@link RoundClock},
 * which knows nothing about Bukkit and can be run a thousand seconds forward in a test.
 */
public class TimerManager implements Manager {

    private static final Title.Times TIMES =
            Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(1000), Duration.ofMillis(1000));

    private final ForceItemBattle forceItemBattle;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();
    /**
     * Driven here, owned by the plugin. This manager still ticks it once a second and renders what
     * it reports; it no longer holds the only reference, which is what let
     * {@code ItemDifficultiesManager} stop reaching through this class for the time.
     */
    private final RoundClock clock;
    private BukkitTask timerTask;

    public TimerManager(ForceItemBattle forceItemBattle, RoundClock clock) {
        this.forceItemBattle = forceItemBattle;
        this.clock = clock;
        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.clock.setSecondsLeft(this.forceItemBattle.getConfig().getInt("timer.time"));
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

    public int getTimeLeft() {
        return this.clock.secondsLeft();
    }

    public void setTimeLeft(int timeLeft) {
        this.clock.setSecondsLeft(timeLeft);
    }

    public void save() {
        this.forceItemBattle.getConfig().set("timer.time", this.clock.secondsLeft());
    }

    public void sendActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!this.forceItemBattle.getRoundPhase().roundRunning()) {
                if (this.forceItemBattle.getRoundPhase().isPausedGame()) {
                    Title timeLeftTitle = Title.title(Component.empty(), Text.of("<red>Game is paused!"),
                            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500)));
                    player.showTitle(timeLeftTitle);

                }
                player.sendActionBar(Text.of("<gray>Timer <red><b>paused</red>"));
                continue;
            }

            ForceItemPlayer forceItemPlayer = this.forceItemBattle.getRoster()
                    .participant(player.getUniqueId())
                    .orElse(null);

            if (forceItemPlayer == null) {
                player.sendActionBar(Text.of("<gradient:#fcef64:#fcc44b:#f44c7d><b>"
                        + TimeFormat.humanised(this.getTimeLeft()) + "</b> <dark_gray>| <gold>SPEC"));
                continue;
            }

            this.sendPlayingHud(player, forceItemPlayer);
        }
    }

    /** The action bar and boss bar of someone actually hunting an item. */
    private void sendPlayingHud(Player player, ForceItemPlayer forceItemPlayer) {
        Material material = forceItemPlayer.activeMaterial();

        String timeText = "<gradient:#fcef64:#fcc44b:#ff9e59><b>"
                + TimeFormat.humanised(this.getTimeLeft()) + "</b></gradient>";
        String scoreText = "";
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.SCORE)) {
            // Only the label differs by mode; the number is the active score either way. Whose
            // score it is is a question about this player's score owner, not about the TEAM
            // setting — a spectator-turned-player with no team would disagree with the setting.
            scoreText = "<dark_gray>| <green>" + (forceItemPlayer.isInTeam() ? "Team score: " : "Your score: ")
                    + "<white>" + forceItemPlayer.activeScore();
        }

        player.sendActionBar(Text.of(timeText + " " + scoreText));

        String bossBarTitle = this.itemLabel(material);
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.CHAIN)) {
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
                + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, material)
                + "</shadow>";
    }

    private void run() {
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBar();
                if (!forceItemBattle.getRoundPhase().roundRunning()) {
                    forceItemBattle.getTabListManager().clearFooter();
                    return;
                }

                OptionalInt milestone = clock.tick();

                forceItemBattle.getRandomEventManager().tick(clock.secondsLeft());
                announceUnlockedPools();

                // Refresh the tab footer after the pool poll so its pool countdown
                // flips to "active" on the same tick the unlock message is sent.
                forceItemBattle.getTabListManager().update();

                milestone.ifPresent(TimerManager.this::announceMilestone);

                if (clock.expired()) {
                    announceRoundOver();
                    forceItemBattle.getTabListManager().clearFooter();
                    forceItemBattle.getGamemanager().finishGame();
                    FileLogger.log("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }

    private void announceUnlockedPools() {
        for (ItemDifficultiesManager.State unlockedPool :
                this.forceItemBattle.getItemDifficultiesManager().pollNewlyUnlockedStates()) {
            Bukkit.getOnlinePlayers().forEach(players -> {
                players.sendMessage(Text.of("<shadow:black:0><sprite:items:item/clock_00> <reset><gray>New item pool unlocked <dark_gray>» <"
                        + unlockedPool.getColor() + ">" + unlockedPool.getDisplayName()));
                players.playSound(players.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 0.8f);
            });
        }
    }

    /**
     * Renders one countdown milestone. The final five seconds put the bare number in the headline,
     * because by then the number is the whole message; everything above that is a line of warning
     * text under an empty headline.
     */
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
