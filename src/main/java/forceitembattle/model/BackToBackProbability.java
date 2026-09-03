package forceitembattle.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * How unlikely a back-to-back chain was, as a percentage, a {@link Rarity} and a label to print.
 *
 * <p>Pure arithmetic: the caller counts what is owned, this decides what that is worth.
 *
 * @param percentage the odds as a percentage, so 0.05 means one in two thousand
 * @param formatted  the percentage and rarity label, ready to drop into a message
 */
public record BackToBackProbability(double percentage, Rarity rarity, String formatted) {

    /**
     * @param uniqueOwned      distinct materials this owner already holds, inventories and backpack
     * @param poolSize         how many materials could currently be handed out
     * @param streak           the chain length <em>after</em> this find, so a third owned item is 3
     * @param repeatOfPrevious the same item they were just handed, which {@link Rarity} ranks apart
     */
    public static BackToBackProbability of(int uniqueOwned, int poolSize, int streak,
                                           boolean repeatOfPrevious) {
        double probability = probabilityOf(uniqueOwned, poolSize, streak);
        double percent = probability * 100;
        Rarity rarity = Rarity.classify(probability, repeatOfPrevious);

        return new BackToBackProbability(percent, rarity,
                formatPercent(percent) + " <dark_gray>(<reset>" + rarity.label() + "<dark_gray>)");
    }

    /** An empty pool yields 0, not the {@code Infinity}/{@code NaN} the raw division would give. */
    private static double probabilityOf(int uniqueOwned, int poolSize, int streak) {
        if (poolSize <= 0) {
            return 0.0;
        }

        double base = Math.min((double) uniqueOwned / poolSize, 1.0); // 100% cap
        return Math.pow(base, streak);
    }

    /**
     * Enough decimal places to show something: a long chain runs small enough that a fixed two places
     * would print every one as "0%", so this keeps two significant digits past the leading zeros.
     *
     * <p><b>Pinned to {@link Locale#ROOT}</b>, or {@code DecimalFormat} follows the JVM default and
     * prints "0,05%" in an English sentence on a European server.
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
