package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Getter
@Setter
public class BackToBack {

    private boolean active;
    private String rarity;
    private double percentage;

    public BackToBack() {
        this.active = false;
        this.rarity = null;
        this.percentage = 0.0;
    }

    public BackToBack(boolean active) {
        this.active = active;
        this.rarity = null;
        this.percentage = 0.0;
    }

    public BackToBack setActive(boolean active) {
        this.active = active;
        return this;
    }

    public BackToBack setRarity(String rarity) {
        this.rarity = rarity;
        return this;
    }

    public BackToBack setPercentage(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        this.percentage = percentage;
        return this;
    }

    @Override
    public String toString() {
        return "Back2Back{" +
                "active=" + active +
                ", rarity='" + rarity + '\'' +
                ", percentage=" + percentage +
                '}';
    }

    /**
     * Optional helper: play a sound when a rare item is found.
     */
    public void playRaritySound(Player player) {
        if (rarity == null || player == null) return;

        switch (rarity) {
            case "EXTRAORDINARY", "LEGENDARY" -> player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0f);
            case "RNGESUS" -> player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1f);
            case "EPIC" -> player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
            case "RARE" -> player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        }
    }
}
