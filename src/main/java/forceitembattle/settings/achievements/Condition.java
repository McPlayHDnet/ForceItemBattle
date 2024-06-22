package forceitembattle.settings.achievements;

import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import lombok.Getter;

import java.util.List;

@Getter
public class Condition {

    private final Trigger trigger;

    private int amount, withinSeconds, timeFrame;
    private List<String> dimensions;
    private List<BiomeGroup> biomeList;
    private CustomItem customItem;
    private boolean consecutive, sameItem, noSkip;

    public Condition(Trigger trigger) {
        this.trigger = trigger;
    }

    public Condition amount(int amount) {
        this.amount = amount;
        return this;
    }

    public Condition consecutive() {
        this.consecutive = true;
        return this;
    }

    public Condition withinSeconds(int seconds) {
        this.withinSeconds = seconds;
        return this;
    }

    public Condition sameItem() {
        this.sameItem = true;
        return this;
    }

    public Condition noSkip() {
        this.noSkip = true;
        return this;
    }

    public Condition timeFrameInSeconds(int seconds) {
        this.timeFrame = seconds;
        return this;
    }

    public Condition customItem(CustomItem customItem) {
        this.customItem = customItem;
        return this;
    }

    public Condition dimension(String... dimensions) {
        this.dimensions = List.of(dimensions);
        return this;
    }

    public Condition biomeList(BiomeGroup... biomeList) {
        this.biomeList = List.of(biomeList);
        return this;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "trigger=" + trigger +
                ", amount=" + amount +
                ", withinSeconds=" + withinSeconds +
                ", timeFrame=" + timeFrame +
                ", dimensions=" + dimensions +
                ", consecutive=" + consecutive +
                ", sameItem=" + sameItem +
                '}';
    }
}
