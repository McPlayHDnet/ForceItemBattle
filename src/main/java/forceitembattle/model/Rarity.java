package forceitembattle.model;

import java.util.function.ToLongFunction;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum Rarity {

    RARE("<blue><b>RARE</b></blue>", "<blue>Rare",
            Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f, false,
            new RarityCounts(1, 0, 0, 0, 0), RarityCounts::rare),
    EPIC("<dark_purple><b>EPIC</b></dark_purple>", "<dark_purple>Epic",
            Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f, false,
            new RarityCounts(0, 1, 0, 0, 0), RarityCounts::epic),
    LEGENDARY("<gold><b>LEGENDARY</b></gold>", "<gold>Legendary",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, true,
            new RarityCounts(0, 0, 1, 0, 0), RarityCounts::legendary),
    RNGESUS("<gradient:#E41EBC:#9A4992><b>RNGESUS</b></gradient>", "<gradient:#E41EBC:#9A4992>RNGesus</gradient>",
            Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1f, true,
            new RarityCounts(0, 0, 0, 1, 0), RarityCounts::rngesus),
    EXTRAORDINARY("<gradient:#73FF00:#14C8FF><b>EXTRAORDINARY</b></gradient>", "<gradient:#73FF00:#14C8FF>Extraordinary</gradient>",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, false,
            new RarityCounts(0, 0, 0, 0, 1), RarityCounts::extraordinary);

    /** Shouty, bold — the found-item announcement. */
    private final String label;

    /** Title-case, unbold — stat listings. Deliberately distinct from {@link #label}. */
    private final String displayName;

    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final boolean broadcast;
    /** This rarity as a delta of one, which is what recording a find adds. */
    private final RarityCounts increment;
    private final ToLongFunction<RarityCounts> statAccessor;

    Rarity(String label, String displayName, Sound sound, float volume, float pitch, boolean broadcast,
           RarityCounts increment,
           ToLongFunction<RarityCounts> statAccessor) {
        this.label = label;
        this.displayName = displayName;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.broadcast = broadcast;
        this.increment = increment;
        this.statAccessor = statAccessor;
    }

    public static Rarity classify(double probability, boolean repeatOfPrevious) {
        if (repeatOfPrevious) return EXTRAORDINARY;
        if (probability <= 0.001) return RNGESUS;
        if (probability <= 0.01) return LEGENDARY;
        if (probability <= 0.05) return EPIC;
        return RARE;
    }

    public String label() {
        return label;
    }

    public String displayName() {
        return displayName;
    }

    public void playSound(Player player) {
        if (player == null) {
            return;
        }
        if (broadcast) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(player.getLocation(), sound, volume, pitch);
            }
        } else {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * One of this rarity, for the stats writer to add. Turning the delta into the request the service
     * wants is {@code FibStatisticsClient}'s job, behind the seam.
     */
    public RarityCounts asIncrement() {
        return increment;
    }

    /** How many of this rarity the given stats hold. Zero when the stats are absent. */
    public long count(@Nullable RarityCounts rarities) {
        return rarities == null ? 0 : this.statAccessor.applyAsLong(rarities);
    }

    /** Total back-to-backs across every rarity. */
    public static long total(@Nullable RarityCounts rarities) {
        long total = 0;
        for (Rarity rarity : values()) {
            total += rarity.count(rarities);
        }
        return total;
    }
}
