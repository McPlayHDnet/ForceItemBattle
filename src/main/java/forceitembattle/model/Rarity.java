package forceitembattle.model;

import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesUpdateRequestDto;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum Rarity {

    RARE("<blue><b>RARE</b></blue>", "<blue>Rare",
            Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f, false,
            b -> b.rareAdd(1L), FibRaritiesDto::getRare),
    EPIC("<dark_purple><b>EPIC</b></dark_purple>", "<dark_purple>Epic",
            Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f, false,
            b -> b.epicAdd(1L), FibRaritiesDto::getEpic),
    LEGENDARY("<gold><b>LEGENDARY</b></gold>", "<gold>Legendary",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, true,
            b -> b.legendaryAdd(1L), FibRaritiesDto::getLegendary),
    RNGESUS("<gradient:#E41EBC:#9A4992><b>RNGESUS</b></gradient>", "<gradient:#E41EBC:#9A4992>RNGesus</gradient>",
            Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1f, true,
            b -> b.rngesusAdd(1L), FibRaritiesDto::getRngesus),
    EXTRAORDINARY("<gradient:#73FF00:#14C8FF><b>EXTRAORDINARY</b></gradient>", "<gradient:#73FF00:#14C8FF>Extraordinary</gradient>",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, false,
            b -> b.extraordinaryAdd(1L), FibRaritiesDto::getExtraordinary);

    /** Shouty, bold — the found-item announcement. */
    private final String label;

    /** Title-case, unbold — stat listings. Deliberately distinct from {@link #label}. */
    private final String displayName;

    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final boolean broadcast;
    private final UnaryOperator<FibRaritiesUpdateRequestDto> statContribution;
    private final Function<FibRaritiesDto, Long> statAccessor;

    Rarity(String label, String displayName, Sound sound, float volume, float pitch, boolean broadcast,
           UnaryOperator<FibRaritiesUpdateRequestDto> statContribution,
           Function<FibRaritiesDto, Long> statAccessor) {
        this.label = label;
        this.displayName = displayName;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.broadcast = broadcast;
        this.statContribution = statContribution;
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

    public FibRaritiesUpdateRequestDto toRaritiesUpdate() {
        return statContribution.apply(new FibRaritiesUpdateRequestDto());
    }

    /**
     * How many of this rarity the given stats hold. Zero when the stats or the
     * field are absent.
     */
    public long count(@Nullable FibRaritiesDto rarities) {
        if (rarities == null) {
            return 0;
        }
        Long value = this.statAccessor.apply(rarities);
        return value != null ? value : 0;
    }

    /** Total back-to-backs across every rarity. */
    public static long total(@Nullable FibRaritiesDto rarities) {
        long total = 0;
        for (Rarity rarity : values()) {
            total += rarity.count(rarities);
        }
        return total;
    }
}
