package forceitembattle.ceremony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.ceremony.StageLayout.Facing;
import forceitembattle.ceremony.StageLayout.Grid;
import forceitembattle.ceremony.StageLayout.Point;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StageLayoutTest {

    private static final double EPSILON = 1e-9;

    @Nested
    class TheGrid {

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 7, 12, 35, 36, 60, 99, 150})
        void everyItemFitsOnTheCanvas(int count) {
            Grid grid = StageLayout.gridFor(count);
            Set<String> taken = new HashSet<>();

            for (int index = 0; index < count; index++) {
                Point slot = grid.slot(index);
                double half = grid.cell() / 2;
                assertTrue(Math.abs(slot.x()) + half <= StageLayout.CANVAS_WIDTH / 2 + EPSILON, "x of " + index);
                assertTrue(slot.y() - half >= StageLayout.CANVAS_BOTTOM - EPSILON, "bottom of " + index);
                assertTrue(slot.y() + half <= StageLayout.CANVAS_TOP + EPSILON, "top of " + index);
                assertTrue(taken.add(Math.round(slot.x() * 1000) + ":" + Math.round(slot.y() * 1000)),
                        "slot " + index + " is shared");
            }
        }

        /** A short list would otherwise blow each item up to fill the whole canvas. */
        @Test
        void aFewItemsAreCappedInSize() {
            assertEquals(StageLayout.MAX_CELL, StageLayout.gridFor(3).cell(), EPSILON);
        }

        @Test
        void moreItemsMeanSmallerCells() {
            assertTrue(StageLayout.gridFor(120).cell() < StageLayout.gridFor(30).cell());
        }

        @Test
        void itemsSitInFrontOfTheCanvas() {
            assertTrue(StageLayout.gridFor(10).slot(0).z() > 0);
        }
    }

    @Nested
    class TheSeats {

        @Test
        void theFirstSeatIsDeadCentre() {
            assertEquals(0, StageLayout.seat(0).x(), EPSILON);
        }

        /** Filled from the middle outwards, so three people are not all sat to one side. */
        @Test
        void aRowFillsFromTheMiddleOut() {
            assertEquals(StageLayout.SEAT_SPACING, StageLayout.seat(1).x(), EPSILON);
            assertEquals(-StageLayout.SEAT_SPACING, StageLayout.seat(2).x(), EPSILON);
        }

        @Test
        void aSecondRowIsBehindAndAboveTheFirst() {
            Point front = StageLayout.seat(0);
            Point back = StageLayout.seat(StageLayout.SEATS_PER_ROW);

            assertTrue(back.z() > front.z());
            assertTrue(back.y() > front.y());
        }

        @Test
        void noTwoPeopleShareASeat() {
            Set<Point> seats = new HashSet<>();
            for (int index = 0; index < 40; index++) {
                assertTrue(seats.add(StageLayout.seat(index)), "seat " + index);
            }
        }

        /** Tuned in-game: the best seat looked north and about 8 degrees down, onto the spotlight. */
        @Test
        void theFrontSeatLooksGentlyDownOntoTheSpotlight() {
            Point eye = StageLayout.seat(0).plus(0, StageLayout.EYE_HEIGHT, 0);
            Facing facing = StageLayout.lookAt(eye, StageLayout.SPOTLIGHT);

            assertEquals(180, Math.abs(facing.yaw()), 1e-3);
            assertTrue(facing.pitch() > 0 && facing.pitch() < 15, "pitch " + facing.pitch());
        }

        @Test
        void everySeatLooksNorthAtTheSpotlight() {
            for (int index = 0; index < 20; index++) {
                Point eye = StageLayout.seat(index).plus(0, StageLayout.EYE_HEIGHT, 0);
                Facing facing = StageLayout.lookAt(eye, StageLayout.SPOTLIGHT);

                assertTrue(Math.abs(Math.abs(facing.yaw()) - 180) < 30, "seat " + index + " yaw " + facing.yaw());
            }
        }

        /**
         * The card used to hang in front of the grid, and the settled items drew over its text. From
         * every row it has to read as below the canvas, not on it.
         */
        @Test
        void theCardNeverCoversTheCanvas() {
            for (int row = 0; row < 4; row++) {
                Point eye = StageLayout.seat(row * StageLayout.SEATS_PER_ROW).plus(0, StageLayout.EYE_HEIGHT, 0);
                double towardsCard = (eye.z() - StageLayout.CARD.z()) / eye.z();
                double sightLine = eye.y() + (StageLayout.CANVAS_BOTTOM - eye.y()) * towardsCard;
                double cardTop = StageLayout.CARD.y() + StageLayout.CARD_HEIGHT;

                assertTrue(cardTop < sightLine, "row " + row + ": card top " + cardTop + " vs " + sightLine);
            }
        }

        @Test
        void theSpotlightSitsJustAboveItsCard() {
            double spotlightBottom = StageLayout.SPOTLIGHT.y() - StageLayout.SPOTLIGHT_SCALE / 2.0;
            assertTrue(spotlightBottom > StageLayout.CARD.y() + StageLayout.CARD_HEIGHT);
        }
    }

    @Nested
    class TheAnchor {

        @Test
        void itClearsFlatGround() {
            assertEquals(67, StageLayout.anchorY((x, z) -> 64, 320));
        }

        @Test
        void itClearsTheTallestThingUnderTheStage() {
            int anchor = StageLayout.anchorY((x, z) -> x == 0 && z == 8 ? 110 : 64, 320);
            assertEquals(113, anchor);
        }

        @Test
        void theCanvasStaysUnderTheBuildLimit() {
            int anchor = StageLayout.anchorY((x, z) -> 318, 320);
            assertTrue(anchor + StageLayout.CANVAS_TOP < 320);
        }
    }

    @Nested
    class ThePodium {

        @Test
        void theWinnerStandsHighestInTheMiddle() {
            assertEquals(0, StageLayout.podiumX(1), EPSILON);
            assertTrue(StageLayout.podiumHeight(1) > StageLayout.podiumHeight(2));
            assertTrue(StageLayout.podiumHeight(2) > StageLayout.podiumHeight(3));
            assertTrue(StageLayout.podiumX(2) < 0 && StageLayout.podiumX(3) > 0);
        }

        @Test
        void neighbouringLabelsNeverMeet() {
            double widest = StageLayout.podiumLabelLineWidth() * StageLayout.TEXT_PIXEL * StageLayout.PODIUM_LABEL_SCALE;
            double apart = StageLayout.podiumX(1) - StageLayout.podiumX(2);
            assertTrue(widest < apart, "a label " + widest + " wide reaches the next step " + apart + " away");
            assertEquals(apart, StageLayout.podiumX(3) - StageLayout.podiumX(1), EPSILON);
        }

        /** Sixteen of Minecraft's widest glyphs, the longest a player name gets, still fit on one line. */
        @Test
        void aFullLengthNameIsNotBroken() {
            assertTrue(StageLayout.podiumLabelLineWidth() >= 16 * 6);
        }

        @Test
        void aTeamStandsSideBySideOnItsStep() {
            double half = StageLayout.STEP_WIDTH / 2;
            for (int members = 1; members <= 4; members++) {
                double first = StageLayout.memberOffset(0, members);
                double last = StageLayout.memberOffset(members - 1, members);
                assertEquals(-first, last, EPSILON, "centred for " + members);
                assertTrue(Math.abs(first) < half, "on the step for " + members);
            }
        }

        /** Seen from a front-row seat, the winner's head must stay under the canvas's bottom row. */
        @Test
        void thePodiumNeverHidesTheCanvas() {
            Point eye = StageLayout.seat(0).plus(0, StageLayout.EYE_HEIGHT, 0);
            double canvasBottom = StageLayout.CANVAS_BOTTOM;
            double towardsPodium = (eye.z() - StageLayout.PODIUM_Z) / eye.z();
            double sightLine = eye.y() + (canvasBottom - eye.y()) * towardsPodium;
            double winnersHead = StageLayout.podiumHeight(1) + 1.9;

            assertTrue(winnersHead < sightLine, "head " + winnersHead + " vs sight line " + sightLine);
        }
    }
}
