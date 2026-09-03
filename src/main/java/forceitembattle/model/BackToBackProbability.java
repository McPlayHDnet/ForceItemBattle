package forceitembattle.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * How unlikely a back-to-back chain was, as a percentage, a {@link Rarity} and a label to print.
 *
 * <p>Pure arithmetic over four numbers, which is the point: it used to be welded to
 * {@code player.getInventory()} and {@code backpacks.getTeamBackpack(...)} inside
 * {@code BackToBackManager}, so none of it — including the seventeen lines of leading-zero logic that
 * decide how a 0.0004% chain prints — could be reached by a test. The caller counts what is owned;
 * this decides what that is worth.
 *
 * @param percentage the odds as a percentage, so 0.05 means one in two thousand
 * @param formatted  the percentage and rarity label, ready to drop into a message
 */
public record BackToBackProbability(double percentage, Rarity rarity, String formatted) {

    /**
     * @param uniqueOwned       distinct materials this owner already holds, inventories and backpack
     * @param poolSize          how many materials could currently be handed out
     * @param streak            the chain length <em>after</em> this find, so a third consecutive
     *                          owned item is a 3
     * @param repeatOfPrevious  whether this item is the same one they were just handed, which
     *                          {@link Rarity} treats as its own thing
     */
    public static BackToBackProbability of(int uniqueOwned, int poolSize, int streak,
                                           boolean repeatOfPrevious) {
        double probability = probabilityOf(uniqueOwned, poolSize, streak);
        double percent = probability * 100;
        Rarity rarity = Rarity.classify(probability, repeatOfPrevious);

        return new BackToBackProbability(percent, rarity,
                formatPercent(percent) + " <dark_gray>(<reset>" + rarity.label() + "<dark_gray>)");
    }

    /**
     * <p>An empty pool yields 0 rather than the {@code Infinity}/{@code NaN} the raw division gives —
     * reachable when the settings exclude every item, and it used to print as a garbage percentage
     * because nothing downstream checks. Zero is also the honest reading: a chain over a pool nobody
     * could draw from is not an achievement anyone earned.
     */
    private static double probabilityOf(int uniqueOwned, int poolSize, int streak) {
        if (poolSize <= 0) {
            return 0.0;
        }

        double base = Math.min((double) uniqueOwned / poolSize, 1.0); // 100% cap
        return Math.pow(base, streak);
    }

    /**
     * Enough decimal places to show something. A long chain runs to a very small number, and a fixed
     * two places would print every one of them as "0%"; this counts the leading zeros and keeps two
     * significant digits past them.
     *
     * <p><b>Pinned to {@link Locale#ROOT}.</b> {@code DecimalFormat} otherwise follows the JVM's
     * default locale, so the same chain printed "0.05%" or "0,05%" depending on where the server
     * happened to be running — a decimal comma in the middle of an English sentence. It went
     * unnoticed because this code was unreachable from a test; the first assertion written against it
     * failed on a German-locale machine.
     */
    private static String formatPercent(double percent) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat df;

        if (percent >= 1) {
            df = new DecimalFormat("0.##", symbols);
        } else {
            int leadingZeros = 0;
            double temp = percent;
            while (temp < 1 && leadingZeros < 15) {
                temp *= 10;
                leadingZeros++;
            }
            df = new DecimalFormat("0." + "#".repeat(Math.max(0, leadingZeros + 2)), symbols);
        }

        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(percent) + "%";
    }
}
