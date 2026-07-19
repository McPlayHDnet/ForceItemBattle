package forceitembattle.util;

public final class ProgressBar {

    /** Character width of the bar itself, excluding the trailing percentage. */
    private static final int BAR_WIDTH = 24;

    private ProgressBar() {
    }

    public static String of(long current, long target) {
        double ratio = target <= 0 ? 1.0 : Math.clamp((double) current / target, 0.0, 1.0);
        double pct = Math.round(ratio * 1000) / 10.0;
        int filled = (int) Math.round(ratio * BAR_WIDTH);

        // Never read visually full below 100%, nor visually empty once started.
        if (pct >= 100.0) {
            filled = BAR_WIDTH;
        } else if (pct > 0.0) {
            filled = Math.clamp(filled, 1, BAR_WIDTH - 1);
        }

        return "<dark_gray><st><green>" + " ".repeat(filled) + "</st>"
                + "<dark_gray><st>" + " ".repeat(BAR_WIDTH - filled) + "</st>"
                + " <yellow>" + pct + "%";
    }
}
