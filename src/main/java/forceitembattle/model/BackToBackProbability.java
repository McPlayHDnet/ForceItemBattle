package forceitembattle.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @param percentage the odds as a percentage, so 0.05 means one in two thousand
 * @param formatted  the percentage and rarity label, ready to drop into a message
 */
public record BackToBackProbability(double percentage, Rarity rarity, String formatted) {

    /**
     * @param streak           the chain length after this find, so a third owned item is 3
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

    /** Two significant digits past the leading zeros, in {@link Locale#ROOT} so it never prints "0,05%". */
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
