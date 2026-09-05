package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.plugin.java.JavaPlugin;
import forceitembattle.model.RoundClock;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.QuickieMode;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The draw itself: which item a player is actually handed.
 *
 * <p>{@code ItemPoolTest} covers what the pool <em>contains</em> as the round unlocks and the
 * settings change. This covers the step after it — that every material handed out comes from that
 * pool. A break here is the worst kind the game has: a player is given an item the settings or the
 * schedule say cannot be obtained yet, and the only way out is a joker.
 *
 * <p>Both rolls are exercised. The seeded one is what run mode draws from, and it filters exactly
 * like the ordinary one; its seed is generated per boot, so what is testable is not reproducibility
 * across servers but that it never escapes the pool.
 */
class ItemRollTest {

    private static final int DRAWS = 300;

    private GameSettings settings;
    private RoundClock clock;
    private ItemDifficultiesManager items;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        this.settings = mock(GameSettings.class);
        this.clock = new RoundClock();

        when(this.settings.getQuickieMode()).thenReturn(QuickieMode.DISABLED);
        allow(GameSetting.HARD, true);
        allow(GameSetting.EXTREME, true);
        allow(GameSetting.END, true);

        this.items = new ItemDifficultiesManager(plugin, this.clock, this.settings);
        this.items.enable();
    }

    private void allow(GameSetting setting, boolean enabled) {
        when(this.settings.isSettingEnabled(setting)).thenReturn(enabled);
    }

    /** Puts the clock {@code elapsedMinutes} into a round of {@code durationMinutes}. */
    private void clock(int durationMinutes, int elapsedMinutes) {
        this.items.configureUnlockSchedule(durationMinutes);
        this.clock.startRound(durationMinutes * 60);
        this.clock.setSecondsLeft((durationMinutes - elapsedMinutes) * 60);
    }

    @Test
    void anOrdinaryDrawIsAlwaysFromTheAvailablePool() {
        clock(60, 30);
        Set<Material> available = Set.copyOf(this.items.getAvailableItems());

        for (int i = 0; i < DRAWS; i++) {
            Material drawn = this.items.generateRandomMaterial();
            assertTrue(available.contains(drawn), drawn + " is not in the available pool");
        }
    }

    @Test
    void aSeededDrawIsAlsoAlwaysFromTheAvailablePool() {
        clock(60, 30);
        Set<Material> available = Set.copyOf(this.items.getAvailableItems());

        for (int i = 0; i < DRAWS; i++) {
            Material drawn = this.items.generateSeededRandomMaterial();
            assertTrue(available.contains(drawn), drawn + " is not in the available pool");
        }
    }

    /**
     * At the start of a round only the EARLY pool is open, so nothing later may be drawn. Handing
     * out a LATE item in the first minute is unobtainable-by-construction.
     */
    @Test
    void nothingFromAClosedPoolIsEverDrawn() {
        clock(60, 0);
        Set<Material> open = Set.copyOf(this.items.getAvailableItems());
        List<Material> late = this.items.getItemsByState(ItemDifficultiesManager.State.LATE);

        assertFalse(late.isEmpty(), "the fixture assumes a non-empty LATE pool");

        for (int i = 0; i < DRAWS; i++) {
            Material drawn = this.items.generateRandomMaterial();
            assertTrue(open.contains(drawn));
            assertFalse(late.contains(drawn), drawn + " is LATE but the round just started");
        }
    }

    /**
     * A setting that removes items has to remove them from the draw too, not just from the listing.
     * With HARD off the nether is unreachable, so a nether item cannot be handed out.
     */
    @Test
    void aDisabledSettingRemovesItemsFromTheDrawAndNotJustTheListing() {
        allow(GameSetting.HARD, false);
        clock(60, 59);
        Set<Material> nether = Set.copyOf(this.items.getNetherItems());

        assertFalse(nether.isEmpty(), "the fixture assumes a non-empty nether set");

        for (int i = 0; i < DRAWS; i++) {
            Material drawn = this.items.generateRandomMaterial();
            assertFalse(nether.contains(drawn), drawn + " is a nether item but HARD is off");
        }
    }

    /**
     * The rule that keeps "collect everything" completable: EXTREME items are registered and can be
     * drawn as force items, but are excluded from the collection catalogue, because requiring them
     * would make the collection achievement effectively unwinnable.
     */
    @Test
    void theCollectionCatalogueExcludesExtremeItems() {
        Set<Material> collectable = this.items.getCollectableItems();
        List<Material> extreme = this.items.getExtremeItems();

        assertFalse(extreme.isEmpty(), "the fixture assumes EXTREME items exist");
        for (Material material : extreme) {
            assertFalse(collectable.contains(material),
                    material + " is EXTREME and must not be required for the collection");
        }
    }

    @Test
    void theCollectionCatalogueIsOtherwiseEverythingRegistered() {
        Set<Material> all = this.items.getAllItems();
        Set<Material> collectable = this.items.getCollectableItems();
        Set<Material> extreme = Set.copyOf(this.items.getExtremeItems());

        assertEquals(all.size() - extreme.size(), collectable.size(),
                "the catalogue is every registered item minus exactly the EXTREME ones");
    }

    /** Every registered material belongs to exactly one pool, or the schedule cannot place it. */
    @Test
    void everyRegisteredItemHasAState() {
        for (Material material : this.items.getAllItems()) {
            assertTrue(this.items.getState(material) != null,
                    material + " is registered but belongs to no pool");
        }
    }
}
