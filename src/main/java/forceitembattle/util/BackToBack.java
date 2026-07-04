package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackToBack {

    private boolean active;
    private String rarity;
    private double percentage;

    public BackToBack(boolean active) {
        this.active = active;
        this.rarity = null;
        this.percentage = 0.0;
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
}
