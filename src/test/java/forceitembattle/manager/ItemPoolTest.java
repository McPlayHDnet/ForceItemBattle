package forceitembattle.manager;

import forceitembattle.model.RoundClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.plugin.java.JavaPlugin;
import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.QuickieMode;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The unlock schedule and the generation-pool cache, which must not go stale unnoticed. */
class ItemPoolTest {
    private ItemDifficultiesManager items;
    private final RoundClock roundClock = new RoundClock();
    private GameSettings settings;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        settings = mock(GameSettings.class);

        when(settings.getQuickieMode()).thenReturn(QuickieMode.DISABLED);
        when(settings.isSettingEnabled(GameSetting.HARD)).thenReturn(true);
        when(settings.isSettingEnabled(GameSetting.EXTREME)).thenReturn(true);
        when(settings.isSettingEnabled(GameSetting.END)).thenReturn(true);

        items = new ItemDifficultiesManager(plugin, roundClock, settings);
        items.enable();
    }

    /** Puts the clock at {@code elapsedMinutes} into a round of {@code durationMinutes}. */
    private void clock(int durationMinutes, int elapsedMinutes) {
        roundClock.startRound(durationMinutes * 60);
        roundClock.setSecondsLeft(durationMinutes * 60 - elapsedMinutes * 60);
    }

    // ==================== unlock schedule ====================

    @Test
    void longRoundsUseFixedFiveAndFifteenMinuteMarks() {
        items.configureUnlockSchedule(60);

        clock(60, 0);
        assertEquals(List.of(State.EARLY), items.getActiveStates());

        clock(60, 5);
        assertEquals(List.of(State.EARLY, State.MID), items.getActiveStates());

        clock(60, 15);
        assertEquals(List.of(State.EARLY, State.MID, State.LATE), items.getActiveStates());
    }

    /**
     * The fixed marks stay fixed as the round gets longer — that is the whole reason for the
     * threshold. On a 90-minute game the percentage schedule would hold LATE back for 26 minutes.
     */
    @Test
    void fixedMarksDoNotDriftWithRoundLength() {
        items.configureUnlockSchedule(90);

        clock(90, 14);
        assertFalse(items.getActiveStates().contains(State.LATE));

        clock(90, 15);
        assertTrue(items.getActiveStates().contains(State.LATE));
    }

    @Test
    void shortRoundsUseThePercentageSchedule() {
        items.configureUnlockSchedule(30);

        clock(30, 2);
        assertEquals(List.of(State.EARLY), items.getActiveStates());

        clock(30, 3); // 11.11% of 30 minutes
        assertEquals(List.of(State.EARLY, State.MID), items.getActiveStates());

        clock(30, 9); // 28.88% of 30 minutes
        assertEquals(List.of(State.EARLY, State.MID, State.LATE), items.getActiveStates());
    }

    @Test
    void quickieModeCapsWhichPoolsEverOpen() {
        items.configureUnlockSchedule(60);
        clock(60, 59);

        when(settings.getQuickieMode()).thenReturn(QuickieMode.EARLY);
        assertEquals(List.of(State.EARLY), items.getActiveStates());

        when(settings.getQuickieMode()).thenReturn(QuickieMode.EARLY_MID);
        assertEquals(List.of(State.EARLY, State.MID), items.getActiveStates());
    }

    @Test
    void nextStateAndCountdownAgreeWithTheSchedule() {
        items.configureUnlockSchedule(60);

        clock(60, 0);
        assertEquals(State.MID, items.getNextState());
        assertEquals(5 * 60, items.secondsUntilNextPool());

        clock(60, 15);
        // everything is open, so there is no next pool
        assertEquals(null, items.getNextState());
        assertEquals(-1, items.secondsUntilNextPool());
    }

    @Test
    void eachPoolIsAnnouncedExactlyOncePerGame() {
        items.configureUnlockSchedule(60);

        clock(60, 0);
        assertEquals(List.of(), items.pollNewlyUnlockedStates()); // EARLY is the baseline

        clock(60, 5);
        assertEquals(List.of(State.MID), items.pollNewlyUnlockedStates());
        assertEquals(List.of(), items.pollNewlyUnlockedStates()); // not again

        clock(60, 15);
        assertEquals(List.of(State.LATE), items.pollNewlyUnlockedStates());

        // a new game announces them again
        items.resetUnlockAnnouncements();
        assertEquals(List.of(State.MID, State.LATE), items.pollNewlyUnlockedStates());
    }

    // ==================== pool cache ====================

    @Test
    void repeatedReadsReuseTheCachedPool() {
        items.configureUnlockSchedule(60);
        clock(60, 0);

        assertSame(items.getAvailableItems(), items.getAvailableItems());
    }

    @Test
    void thePoolGrowsWhenAPoolUnlocks() {
        items.configureUnlockSchedule(60);

        clock(60, 0);
        int early = items.getAvailableItems().size();

        clock(60, 15);
        List<Material> everything = items.getAvailableItems();

        assertTrue(everything.size() > early,
                "expected the LATE unlock to widen the pool, was " + early + " -> " + everything.size());
    }

    @Test
    void changingASettingRebuildsThePool() {
        items.configureUnlockSchedule(60);
        clock(60, 15);

        List<Material> withEnd = items.getAvailableItems();
        when(settings.isSettingEnabled(GameSetting.END)).thenReturn(false);
        List<Material> withoutEnd = items.getAvailableItems();

        assertNotSame(withEnd, withoutEnd);
        assertTrue(withoutEnd.size() < withEnd.size());
        // BLACK_SHULKER_BOX is registered with ItemTag.END
        assertTrue(withEnd.contains(Material.BLACK_SHULKER_BOX));
        assertFalse(withoutEnd.contains(Material.BLACK_SHULKER_BOX));
    }

    @Test
    void disablingHardRemovesNetherItems() {
        items.configureUnlockSchedule(60);
        clock(60, 15);

        // ANCIENT_DEBRIS is registered with ItemTag.NETHER
        assertTrue(items.getAvailableItems().contains(Material.ANCIENT_DEBRIS));

        when(settings.isSettingEnabled(GameSetting.HARD)).thenReturn(false);

        assertFalse(items.getAvailableItems().contains(Material.ANCIENT_DEBRIS),
                "NETHER-tagged item survived HARD=false");
    }

    /** With HARD on but EXTREME off, only the EXTREME-tagged items drop out. */
    @Test
    void disablingExtremeKeepsNetherItems() {
        items.configureUnlockSchedule(60);
        clock(60, 15);

        when(settings.isSettingEnabled(GameSetting.EXTREME)).thenReturn(false);
        List<Material> pool = items.getAvailableItems();

        assertTrue(pool.contains(Material.ANCIENT_DEBRIS));
        assertTrue(items.getExtremeItems().stream().noneMatch(pool::contains),
                "an EXTREME-tagged item survived EXTREME=false");
    }

    @Test
    void itemInListAgreesWithTheReturnedPool() {
        items.configureUnlockSchedule(60);
        clock(60, 15);

        List<Material> pool = items.getAvailableItems();
        assertTrue(items.itemInList(pool.get(0)));
        assertFalse(items.itemInList(Material.AIR)); // never registered
    }

    /** The cache hands back its own list; an in-place edit would corrupt every other reader. */
    @Test
    void theReturnedPoolIsUnmodifiable() {
        items.configureUnlockSchedule(60);
        clock(60, 0);

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> items.getAvailableItems().add(Material.STONE));
    }

    @Test
    void generatedMaterialsComeFromTheCurrentPool() {
        items.configureUnlockSchedule(60);
        clock(60, 0);

        List<Material> pool = items.getAvailableItems();
        for (int i = 0; i < 50; i++) {
            assertTrue(pool.contains(items.generateRandomMaterial()));
        }
    }
}
