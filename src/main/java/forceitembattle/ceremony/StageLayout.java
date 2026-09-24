package forceitembattle.ceremony;

import java.util.function.IntBinaryOperator;

/**
 * Where everything on the result stage sits, in stage space: x to the audience's right, y up, z
 * towards the audience. The canvas is nothing built: it is the plane z = 0 the grid is laid out
 * on, in open sky. The audience sits at +z facing north, so stage space is world space shifted by the anchor,
 * with no rotation.
 */
final class StageLayout {

    static final double CANVAS_WIDTH = 16.0;
    static final double CANVAS_HEIGHT = 8.0;
    // High enough that the podium in front of it never hides the bottom row from the seats.
    static final double CANVAS_BOTTOM = 4.0;
    static final double CANVAS_TOP = CANVAS_BOTTOM + CANVAS_HEIGHT;

    static final double GRID_DEPTH = 0.3;
    static final double MAX_CELL = 1.6;
    static final double ITEM_FILL = 0.8;

    static final Point CANVAS_CENTRE = new Point(0, CANVAS_BOTTOM + CANVAS_HEIGHT / 2, 0);

    // The card hangs below the canvas, not on it: in front of the grid, the settled items drew over its text.
    static final float CARD_SCALE = 1.5f;
    static final double CARD_HEIGHT = 2.0;
    static final Point CARD = new Point(0, 1.3, 3.0);
    static final float SPOTLIGHT_SCALE = 2.4f;
    static final Point SPOTLIGHT = new Point(0, CARD.y() + CARD_HEIGHT + 0.3 + SPOTLIGHT_SCALE / 2.0, 3.0);
    static final Point TITLE = new Point(0, CANVAS_TOP + 1.1, 0.2);
    static final Point COUNTER = new Point(0, CANVAS_TOP + 0.35, 0.2);

    // Measured in-game as the best view: about 10.5 out, eyes a little above the spotlight.
    static final double SEAT_DISTANCE = 10.5;
    static final int SEATS_PER_ROW = 7;
    static final double SEAT_SPACING = 1.2;
    static final double ROW_DEPTH = 2.0;
    static final double ROW_RISE = 1.0;
    static final double SEAT_HEIGHT = 4.3;
    /** Roughly where a player sitting on a cushion has their eyes, above the cushion's base. */
    static final double EYE_HEIGHT = 1.3;

    static final double FOOTPRINT_HALF_WIDTH = (SEATS_PER_ROW / 2 + 2) * SEAT_SPACING;
    static final double FOOTPRINT_FRONT = -1.5;

    static final double PODIUM_Z = 2.0;
    static final double STEP_WIDTH = 2.2;
    static final double STEP_DEPTH = 2.2;

    private static final double CLEARANCE = 3;

    private StageLayout() {
    }

    record Point(double x, double y, double z) {

        Point plus(double dx, double dy, double dz) {
            return new Point(this.x + dx, this.y + dy, this.z + dz);
        }
    }

    record Facing(float yaw, float pitch) {
    }

    /**
     * @param cell      the side of one square cell
     * @param itemScale the display scale of an item inside it
     */
    record Grid(int columns, int rows, double cell, float itemScale) {

        Point slot(int index) {
            int column = index % this.columns;
            int row = index / this.columns;
            double left = -this.columns * this.cell / 2;
            double top = CANVAS_CENTRE.y() + this.rows * this.cell / 2;
            return new Point(left + (column + 0.5) * this.cell, top - (row + 0.5) * this.cell, GRID_DEPTH);
        }
    }

    /** The column count that gives the largest cell while all {@code count} items still fit. */
    static Grid gridFor(int count) {
        int items = Math.max(1, count);
        int bestColumns = 1;
        double bestCell = 0;
        for (int columns = 1; columns <= items; columns++) {
            int rows = ceilDiv(items, columns);
            double cell = Math.min(CANVAS_WIDTH / columns, CANVAS_HEIGHT / rows);
            if (cell > bestCell) {
                bestCell = cell;
                bestColumns = columns;
            }
        }
        double cell = Math.min(bestCell, MAX_CELL);
        return new Grid(bestColumns, ceilDiv(items, bestColumns), cell, (float) (cell * ITEM_FILL));
    }

    /** Fills each row from the middle outwards, so a half-empty row is still centred on the canvas. */
    static Point seat(int index) {
        int row = index / SEATS_PER_ROW;
        int inRow = index % SEATS_PER_ROW;
        int offset = inRow % 2 == 1 ? (inRow + 1) / 2 : -(inRow / 2);
        return new Point(offset * SEAT_SPACING, rowFloor(row) + SEAT_HEIGHT, SEAT_DISTANCE + row * ROW_DEPTH);
    }

    static double rowFloor(int row) {
        return row * ROW_RISE;
    }

    /** Minecraft's yaw runs clockwise from south, and a positive pitch looks down. */
    static Facing lookAt(Point from, Point to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return new Facing(yaw, pitch);
    }

    /** Every item faces straight south, parallel to the canvas, so the grid reads like an inventory. */
    static final Facing ITEM_FACING = new Facing(0, 0);

    /** First place in the middle, second to the audience's left, third to their right. */
    static double podiumX(int place) {
        return switch (place) {
            case 1 -> 0;
            case 2 -> -STEP_WIDTH - 0.2;
            default -> STEP_WIDTH + 0.2;
        };
    }

    static double podiumHeight(int place) {
        return switch (place) {
            case 1 -> 1.4;
            case 2 -> 0.9;
            default -> 0.5;
        };
    }

    /** Side by side on one step, however many members a team brings. */
    static double memberOffset(int member, int members) {
        double spacing = Math.min(0.8, (STEP_WIDTH - 0.4) / Math.max(1, members - 1));
        return (member - (members - 1) / 2.0) * spacing;
    }

    /**
     * The anchor height: clear of all terrain under the stage's footprint, but low enough that the
     * canvas stays under the build limit.
     *
     * @param heightAt the highest block at a stage-space (x, z) offset from the anchor
     */
    static int anchorY(IntBinaryOperator heightAt, int maxHeight) {
        int highest = Integer.MIN_VALUE;
        int back = (int) Math.ceil(SEAT_DISTANCE + 4 * ROW_DEPTH);
        int side = (int) Math.ceil(FOOTPRINT_HALF_WIDTH);
        for (int x = -side; x <= side; x++) {
            for (int z = (int) FOOTPRINT_FRONT; z <= back; z++) {
                highest = Math.max(highest, heightAt.applyAsInt(x, z));
            }
        }
        int ceiling = maxHeight - (int) Math.ceil(CANVAS_TOP + 3);
        return Math.min(highest + (int) CLEARANCE, ceiling);
    }

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
