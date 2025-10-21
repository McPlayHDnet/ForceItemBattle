package forceitembattle.settings.achievements;

import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class Condition {

    private final Trigger trigger;
    private int amount = 1;
    private boolean consecutive = false;
    private boolean noSkip = false;
    private boolean sameItem = false;
    private Set<String> dimensions = null;
    private Set<BiomeGroup> biomeGroups = null;
    private long withinSeconds = 0;
    private long timeFrame = 0;
    private long skipAfterSeconds = 0;
    private int closeCallSeconds = 0;
    private boolean firstPlayer = false;
    private CustomItem customItem = null;
    private boolean woodTypes = false;
    private boolean stoneTypes = false;
    private boolean rareMobDrop = false;
    private boolean playerBased = false;
    private boolean skippedThenGot = false;
    private Material material = null;
    private boolean neededItem = false;

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

    public Condition noSkip() {
        this.noSkip = true;
        return this;
    }

    public Condition sameItem() {
        this.sameItem = true;
        return this;
    }

    public Condition dimension(String... dimensions) {
        this.dimensions = new HashSet<>(Arrays.asList(dimensions));
        return this;
    }

    public Condition biomeList(BiomeGroup... biomes) {
        this.biomeGroups = new HashSet<>(Arrays.asList(biomes));
        return this;
    }

    public Condition withinSeconds(long seconds) {
        this.withinSeconds = seconds;
        return this;
    }

    public Condition timeFrameInSeconds(long seconds) {
        this.timeFrame = seconds;
        return this;
    }

    public Condition skipAfterSeconds(long seconds) {
        this.skipAfterSeconds = seconds;
        return this;
    }

    public Condition closeCall(int seconds) {
        this.closeCallSeconds = seconds;
        return this;
    }

    public Condition firstPlayer() {
        this.firstPlayer = true;
        return this;
    }

    public Condition customItem(CustomItem customItem) {
        this.customItem = customItem;
        return this;
    }

    public Condition woodTypes() {
        this.woodTypes = true;
        return this;
    }

    public Condition stoneTypes() {
        this.stoneTypes = true;
        return this;
    }

    public Condition rareMobDrop() {
        this.rareMobDrop = true;
        return this;
    }

    public Condition playerBased() {
        this.playerBased = true;
        return this;
    }

    public Condition skippedThenGot() {
        this.skippedThenGot = true;
        return this;
    }

    public Condition material(Material material) {
        this.material = material;
        return this;
    }

    public Condition neededItem() {
        this.neededItem = true;
        return this;
    }
}