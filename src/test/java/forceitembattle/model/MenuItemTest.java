package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.MenuItem.Menu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link MenuItem}: the table itself, with no server anywhere near it.
 *
 * <p>Headless on purpose, and the reason the table carries no behaviour: an action that opened a GUI
 * would drag {@code ItemStack} and the plugin graph in behind it, and this file would need MockBukkit
 * to ask arithmetic questions.
 */
class MenuItemTest {

    /** Where a menu is actually laid out. Nothing draws a bar in the other three phases. */
    private static final List<GameState> LAYOUT_PHASES =
            List.of(GameState.PRE_GAME, GameState.END_GAME);

    @Nested
    class TheBars {

        /** Two buttons on one bar in one slot means one silently wins by declaration order. */
        @Test
        void noTwoOpeningButtonsWantTheSameSlot() {
            for (Menu menu : Menu.values()) {
                for (GameState state : LAYOUT_PHASES) {
                    Map<Integer, MenuItem> bySlot = new HashMap<>();
                    for (MenuItem menuItem : MenuItem.openingBar(menu, state)) {
                        MenuItem clash = bySlot.put(menuItem.slot(), menuItem);
                        assertNull(clash, menu + " in " + state + ": " + menuItem + " and " + clash
                                + " both want slot " + menuItem.slot());
                    }
                }
            }
        }

        /** Nine slots on a hotbar, and a button outside them is a button nobody can press. */
        @ParameterizedTest
        @EnumSource(MenuItem.class)
        void everyButtonSitsOnTheHotbar(MenuItem menuItem) {
            assertTrue(menuItem.slot() >= 0 && menuItem.slot() <= 8,
                    menuItem + " is at slot " + menuItem.slot());
        }

        @Test
        void theLobbyBarIsCollectionAchievementsAndTheSpectateToggle() {
            List<MenuItem> bar = MenuItem.openingBar(Menu.LOBBY, GameState.PRE_GAME);

            assertEquals(List.of(MenuItem.COLLECTION, MenuItem.ACHIEVEMENTS,
                    MenuItem.SPECTATE_ROUND), bar);
        }

        @Test
        void theResultBarIsTheSevenButtonsItAlwaysWas() {
            List<MenuItem> bar = MenuItem.openingBar(Menu.RESULT, GameState.END_GAME);

            assertEquals(7, bar.size(), bar.toString());
            assertTrue(bar.contains(MenuItem.TELEPORTER), bar.toString());
            assertTrue(bar.contains(MenuItem.SPECTATE_RESULT), bar.toString());
        }

        /**
         * A lobby player at {@code END_GAME} — which happens, because {@code END_GAME} shares the
         * {@code LOBBY} admission with {@code PRE_GAME} — keeps only the buttons that still work.
         */
        @Test
        void theLobbyBarLosesOnlyTheSpectateToggleOnceTheRoundIsOver() {
            List<MenuItem> afterTheRound = MenuItem.openingBar(Menu.LOBBY, GameState.END_GAME);

            assertEquals(List.of(MenuItem.COLLECTION, MenuItem.ACHIEVEMENTS), afterTheRound);
            assertFalse(afterTheRound.contains(MenuItem.SPECTATE_ROUND),
                    "there is nothing to opt out of once the round is over");
        }

        /** No bar is drawn while a round is being played, or during the countdown. */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"STARTING", "MID_GAME", "PAUSED_GAME"})
        void noBarIsDrawnDuringARound(GameState state) {
            for (Menu menu : Menu.values()) {
                assertTrue(MenuItem.openingBar(menu, state).isEmpty(),
                        menu + " should draw nothing in " + state);
            }
        }

        /**
         * The one button that is never laid out, only swapped in. If it ever became an opening
         * button it would fight {@link MenuItem#SPECTATE_ROUND} for slot 8.
         */
        @Test
        void theOptBackInButtonIsNeverPartOfAnOpeningBar() {
            assertFalse(MenuItem.PLAY_ROUND.isOpeningButton());
            for (GameState state : LAYOUT_PHASES) {
                assertFalse(MenuItem.openingBar(Menu.LOBBY, state).contains(MenuItem.PLAY_ROUND));
            }
            assertEquals(MenuItem.SPECTATE_ROUND.slot(), MenuItem.PLAY_ROUND.slot(),
                    "it replaces that button, so it has to take that slot");
        }
    }

    @Nested
    class Liveness {

        /** A button that answers in no phase is dead weight. */
        @ParameterizedTest
        @EnumSource(MenuItem.class)
        void everyButtonIsClickableSomewhere(MenuItem menuItem) {
            boolean live = false;
            for (GameState state : GameState.values()) {
                live |= menuItem.isLiveIn(state);
            }
            assertTrue(live, menuItem + " can never be clicked");
        }

        /** And a button that is laid out has to answer where it is laid out. */
        @Test
        void everyButtonOnABarIsClickableThere() {
            for (Menu menu : Menu.values()) {
                for (GameState state : LAYOUT_PHASES) {
                    for (MenuItem menuItem : MenuItem.openingBar(menu, state)) {
                        assertTrue(menuItem.isLiveIn(state),
                                menuItem + " is drawn in " + state + " but does nothing there");
                    }
                }
            }
        }

        /** Nothing is clickable mid-round: the joker and backpack are handled elsewhere. */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"STARTING", "MID_GAME", "PAUSED_GAME"})
        void nothingIsClickableDuringARound(GameState state) {
            for (MenuItem menuItem : MenuItem.values()) {
                assertFalse(menuItem.isLiveIn(state), menuItem + " answers in " + state);
            }
        }

        /** The lobby's two reference buttons keep working while a result screen is up. */
        @Test
        void theLobbyReferenceButtonsWorkInBothPhases() {
            for (GameState state : LAYOUT_PHASES) {
                assertTrue(MenuItem.COLLECTION.isLiveIn(state));
                assertTrue(MenuItem.ACHIEVEMENTS.isLiveIn(state));
            }
        }
    }

    @Nested
    class TheMarker {

        @ParameterizedTest
        @EnumSource(MenuItem.class)
        void everyButtonIsFoundByItsOwnName(MenuItem menuItem) {
            assertEquals(menuItem, MenuItem.byMarker(menuItem.name()));
        }

        /**
         * A value from a constant that has since been renamed leaves an inert item, not an
         * exception inside an event handler.
         */
        @Test
        void anUnknownMarkerIsNotAButton() {
            assertNull(MenuItem.byMarker("SOMETHING_WE_DELETED"));
        }

        @Test
        void aMissingMarkerIsNotAButton() {
            assertNull(MenuItem.byMarker(null));
        }

        @Test
        void theKeyIsInThePluginsNamespace() {
            assertEquals("fib", MenuItem.markerKey().getNamespace());
        }
    }

    @Nested
    class TheCollisionsTheTableMakesVisible {

        /**
         * Two constants, one material, unrelated meanings. Only safe because the reader dispatches on
         * the marker; pinned so the pair stays visible rather than looking like a duplicate.
         */
        @Test
        void twoButtonsShareTheEnderEyeAndMeanDifferentThings() {
            assertEquals(MenuItem.PLAY_ROUND.material(), MenuItem.TO_END.material());
            assertFalse(MenuItem.PLAY_ROUND.isLiveIn(GameState.END_GAME));
            assertFalse(MenuItem.TO_END.isLiveIn(GameState.PRE_GAME));
        }

        /** Collection and Achievements exist twice, once per bar, in different slots. */
        @Test
        void twoActionsExistOnceForEachBar() {
            assertEquals(MenuItem.COLLECTION.material(), MenuItem.RESULT_COLLECTION.material());
            assertEquals(MenuItem.ACHIEVEMENTS.material(), MenuItem.RESULT_ACHIEVEMENTS.material());

            assertNotNull(MenuItem.COLLECTION.menu());
            assertFalse(MenuItem.COLLECTION.slot() == MenuItem.RESULT_COLLECTION.slot(),
                    "the two bars disagree about where Collection goes, and always have");
            assertFalse(MenuItem.ACHIEVEMENTS.slot() == MenuItem.RESULT_ACHIEVEMENTS.slot());
        }

        /** Every constant belongs to exactly one bar, so "which bar" is never ambiguous. */
        @Test
        void everyButtonBelongsToOneBar() {
            List<MenuItem> placed = new ArrayList<>();
            for (Menu menu : Menu.values()) {
                for (MenuItem menuItem : MenuItem.values()) {
                    if (menuItem.menu() == menu) {
                        placed.add(menuItem);
                    }
                }
            }
            assertEquals(MenuItem.values().length, placed.size());
        }
    }
}
