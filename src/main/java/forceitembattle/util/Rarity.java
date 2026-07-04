package forceitembattle.util;

import de.threeseconds.openapi.fibservice.client.model.FibRaritiesUpdateRequestDto;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public enum Rarity {

    RARE("<blue><b>RARE</b></blue>",
            Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f, false,
            b -> b.rareAdd(1L)),
    EPIC("<dark_purple><b>EPIC</b></dark_purple>",
            Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f, false,
            b -> b.epicAdd(1L)),
    LEGENDARY("<gold><b>LEGENDARY</b></gold>",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, true,
            b -> b.legendaryAdd(1L)),
    RNGESUS("<gradient:#E41EBC:#9A4992><b>RNGESUS</b></gradient>",
            Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1f, true,
            b -> b.rngesusAdd(1L)),
    EXTRAORDINARY("<gradient:#73FF00:#14C8FF><b>EXTRAORDINARY</b></gradient>",
            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f, false,
            b -> b.extraordinaryAdd(1L));

    private final String label;
    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final boolean broadcast;
    private final UnaryOperator<FibRaritiesUpdateRequestDto> statContribution;

    Rarity(String label, Sound sound, float volume, float pitch, boolean broadcast,
           UnaryOperator<FibRaritiesUpdateRequestDto> statContribution) {
        this.label = label;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.broadcast = broadcast;
        this.statContribution = statContribution;
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
}