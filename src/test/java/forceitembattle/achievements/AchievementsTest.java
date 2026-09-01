package forceitembattle.achievements;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.progress.AchievementProgressTracker;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.LootGenerateEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * The {@link Achievements} table itself — 80 constants and the cast that drives them.
 *
 * <h2>The cast</h2>
 *
 * <p>{@code AchievementManager} does this for every ROUND achievement, once per matching event:
 *
 * <pre>
 *   AchievementHandler&lt;AchievementProgressTracker&gt; typed =
 *           (AchievementHandler&lt;AchievementProgressTracker&gt;) handler;   // unchecked
 *   typed.check(event, tracker, player, world);
 * </pre>
 *
 * <p>The tracker came from that same handler's {@code createProgress()}, so the cast is sound as
 * long as every handler agrees with itself about its progress type. Nothing checked that for any of
 * the 53 ROUND constants. A handler whose {@code createProgress()} returns the wrong tracker throws
 * {@code ClassCastException} in a live round, for one achievement, at whatever moment its trigger
 * first fires — and only for the players it was evaluated against.
 *
 * <p>So the central test here drives every ROUND achievement through its own handler with its own
 * progress and a real event for its trigger, doing exactly what the manager does.
 */
class AchievementsTest {

    private ServerMock server;
    private WorldMock world;
    private ForceItemPlayer participant;
    private FakeAchievementWorld achievementWorld;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        PlayerMock player = this.server.addPlayer("Understudy1");
        this.participant = new ForceItemPlayer(player, Material.DIRT, 3, 0);
        this.participant.scoreOwner().startRound(Material.DIRT, Material.STONE, 0L);
        this.achievementWorld = new FakeAchievementWorld().clock(5400, 5400);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- the table ----------------------------------------------------------------------------

    @Test
    void everyAchievementHasATitleAndADescription() {
        for (Achievements achievement : Achievements.values()) {
            assertNotNull(achievement.getTitle(), achievement + " needs a title");
            assertFalse(achievement.getTitle().isBlank(), achievement + " has a blank title");
            assertNotNull(achievement.getDescription(), achievement + " needs a description");
            assertFalse(achievement.getDescription().isBlank(), achievement + " has a blank description");
        }
    }

    /** Titles are what a player reads in the menu; two the same is a bug you only notice in game. */
    @Test
    void titlesAreUnique() {
        Map<String, Achievements> byTitle = new HashMap<>();
        List<String> clashes = new ArrayList<>();

        for (Achievements achievement : Achievements.values()) {
            Achievements previous = byTitle.put(achievement.getTitle(), achievement);
            if (previous != null) {
                clashes.add(previous + " and " + achievement + " both titled '" + achievement.getTitle() + "'");
            }
        }

        assertTrue(clashes.isEmpty(), String.join("; ", clashes));
    }

    /**
     * The construction-time invariant, restated. It cannot fail at runtime — a malformed constant
     * stops the enum class-initialising and takes the whole plugin with it — so this is here to say
     * what the rule is, and to fail with a readable message rather than
     * {@code ExceptionInInitializerError} if it ever does.
     */
    @Test
    void everyAchievementCarriesExactlyItsOwnRule() {
        for (Achievements achievement : Achievements.values()) {
            int carriers = (achievement.getHandler() != null ? 1 : 0)
                    + (achievement.getGlobalRule() != null ? 1 : 0)
                    + (achievement.getCompletionistRule() != null ? 1 : 0)
                    + (achievement.getCollectionRule() != null ? 1 : 0);

            assertEquals(1, carriers, achievement + " must carry exactly one rule or handler");

            switch (achievement.getScope()) {
                case ROUND -> assertNotNull(achievement.getHandler(), achievement + " is ROUND");
                case GLOBAL -> assertNotNull(achievement.getGlobalRule(), achievement + " is GLOBAL");
                case META -> assertNotNull(achievement.getCompletionistRule(), achievement + " is META");
                case COLLECTION -> assertNotNull(achievement.getCollectionRule(), achievement + " is COLLECTION");
            }
        }
    }

    /** Only ROUND achievements are handler-driven; the trigger map is built from exactly those. */
    @Test
    void onlyRoundAchievementsHaveAHandler() {
        for (Achievements achievement : Achievements.values()) {
            if (achievement.getScope() == AchievementScope.ROUND) {
                assertNotNull(achievement.getHandler().getTrigger(),
                        achievement + " has no trigger, so nothing would ever evaluate it");
            } else {
                assertNull(achievement.getHandler(), achievement + " is not ROUND but has a handler");
            }
        }
    }

    @Test
    void isGlobalAgreesWithTheScope() {
        for (Achievements achievement : Achievements.values()) {
            assertEquals(achievement.getScope() == AchievementScope.GLOBAL, achievement.isGlobal(),
                    achievement + " disagrees with its own scope");
        }
    }

    // --- the cast -----------------------------------------------------------------------------

    /**
     * Every ROUND achievement, driven exactly as {@code AchievementManager} drives it.
     *
     * <p>If a handler's {@code createProgress()} disagrees with what its {@code check()} expects,
     * this is where it surfaces — here, in one run, rather than in a live round for one achievement
     * at whatever moment its trigger first fires.
     */
    @Test
    void everyRoundHandlerAcceptsItsOwnProgressTracker() {
        List<String> failures = new ArrayList<>();

        for (Achievements achievement : Achievements.values()) {
            if (achievement.getScope() != AchievementScope.ROUND) {
                continue;
            }

            AchievementHandler<?> handler = achievement.getHandler();
            Event event = eventFor(handler.getTrigger());

            try {
                @SuppressWarnings("unchecked")
                AchievementHandler<AchievementProgressTracker> typed =
                        (AchievementHandler<AchievementProgressTracker>) handler;
                typed.check(event, handler.createProgress(), this.participant, this.achievementWorld);
            } catch (ClassCastException e) {
                failures.add(achievement + " (" + handler.getClass().getSimpleName()
                        + ") rejects its own progress: " + e.getMessage());
            } catch (RuntimeException e) {
                failures.add(achievement + " (" + handler.getClass().getSimpleName()
                        + ") threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    /** Driving the same handler repeatedly must not throw either — progress accumulates. */
    @Test
    void everyRoundHandlerSurvivesRepeatedEvaluation() {
        assertDoesNotThrow(() -> {
            for (Achievements achievement : Achievements.values()) {
                if (achievement.getScope() != AchievementScope.ROUND) {
                    continue;
                }
                AchievementHandler<?> handler = achievement.getHandler();
                @SuppressWarnings("unchecked")
                AchievementHandler<AchievementProgressTracker> typed =
                        (AchievementHandler<AchievementProgressTracker>) handler;
                AchievementProgressTracker progress = handler.createProgress();
                Event event = eventFor(handler.getTrigger());

                for (int i = 0; i < 3; i++) {
                    typed.check(event, progress, this.participant, this.achievementWorld);
                }
            }
        });
    }

    /** A representative event per trigger — the same kinds the listeners actually deliver. */
    private Event eventFor(Trigger trigger) {
        PlayerMock player = (PlayerMock) this.participant.player();
        return switch (trigger) {
            case OBTAIN_ITEM, OBTAIN_ITEM_IN_TIME, BACK_TO_BACK ->
                    Finds.found(this.participant, Material.DIRT);
            case SKIP_ITEM -> Finds.skipped(this.participant, Material.DIRT);
            case WHEEL_OF_FORTUNE -> new WheelOfFortuneWinEvent(player, Material.DIRT);
            case ANTIMATTER_TELEPORTER -> new AntimatterTeleporterUseEvent(player, true);
            case DYING -> mock(PlayerDeathEvent.class);
            case TRADING -> purchaseEvent();
            case EATING -> consumeEvent();
            case LOOT -> lootEvent();
            case MOB_DEATH -> mobDeathEvent();
            case BEEHIVE_HARVEST -> mock(PlayerInteractEvent.class);
            case VISIT -> new PlayerMoveEvent(player,
                    new Location(this.world, 0, 64, 0), new Location(this.world, 1, 64, 1));
            // Reads the player's inventory and ignores the event entirely.
            case INVENTORY_FULL -> Finds.found(this.participant, Material.DIRT);
        };
    }

    /** The handler asks the event who traded; a real one always carries a player. */
    private Event purchaseEvent() {
        io.papermc.paper.event.player.PlayerPurchaseEvent event =
                mock(io.papermc.paper.event.player.PlayerPurchaseEvent.class);
        when(event.getPlayer()).thenReturn(this.participant.player());
        return event;
    }

    private Event consumeEvent() {
        PlayerItemConsumeEvent event = mock(PlayerItemConsumeEvent.class);
        when(event.getItem()).thenReturn(new ItemStack(Material.BREAD));
        return event;
    }

    private Event lootEvent() {
        LootGenerateEvent event = mock(LootGenerateEvent.class);
        when(event.getLoot()).thenReturn(List.of(new ItemStack(Material.DIAMOND)));
        return event;
    }

    private Event mobDeathEvent() {
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
        when(event.getDrops()).thenReturn(new ArrayList<>());
        return event;
    }

    /**
     * Every title and description has to survive the unlock announcement, which embeds both inside a
     * single-quoted {@code <hover:show_text:'...'>} argument. An apostrophe there closes the argument
     * early and MiniMessage then dumps the entire raw markup into chat as literal text — the whole
     * line, not just the hover. "That's a Rock, Jim" and "It's so empty" shipped doing exactly that.
     *
     * <p>This runs over the table rather than those two constants so the next title with an
     * apostrophe — or a backslash, or a {@code <} — fails here instead of in chat.
     */
    @Test
    void everyAchievementAnnouncementRendersWithoutLeakingMarkup() {
        for (Achievements achievement : Achievements.values()) {
            String announcement = "<gray>has made the achievement <hover:show_text:'<dark_aqua>"
                    + Text.tagArgument(achievement.getTitle()) + "<newline><gray>"
                    + Text.tagArgument(achievement.getDescription()) + "'><dark_aqua>["
                    + achievement.getTitle() + "]</hover>";

            String rendered = PlainTextComponentSerializer.plainText().serialize(Text.of(announcement));

            assertFalse(rendered.contains("<hover:"),
                    achievement.name() + " leaked its markup into chat: " + rendered);
            assertEquals("has made the achievement [" + achievement.getTitle() + "]", rendered,
                    achievement.name() + " did not render as plain text");
        }
    }
}
