package forceitembattle.settings.achievements;

import forceitembattle.util.BiomeGroup;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class AchievementProgress {

    private int itemCount, back2BackCount, consecutiveCount, skipCount, deathCounter;
    private final Set<String> visitedDimensions;
    private final Set<BiomeGroup> visitedBiomes;
    private long startTime;
    private Material lastItemType;
    private boolean lastItemBackToBack;

    public AchievementProgress() {
        this.itemCount = 0;
        this.back2BackCount = 0;
        this.consecutiveCount = 0;
        this.skipCount = 0;
        this.deathCounter = 0;
        this.visitedDimensions = new HashSet<>();
        this.visitedBiomes = new HashSet<>();
        this.startTime = System.currentTimeMillis();
        this.lastItemType = null;
        this.lastItemBackToBack = false;
    }

    public void incrementItemCount() {
        this.itemCount++;
    }

    public void incrementConsecutiveCount() {
        this.consecutiveCount++;
    }

    public void incrementBack2BackCount() {
        this.back2BackCount++;
    }

    public void incrementSkipCount() {
        this.skipCount++;
    }

    public void incrementDeathCount() {
        this.deathCounter++;
    }

    public void resetItemCount() {
        this.itemCount = 0;
    }

    public void resetDeathCount() {
        this.deathCounter = 0;
    }

    public void resetConsecutiveCount() {
        this.consecutiveCount = 0;
    }

    public void resetBack2BackCount() {
        this.back2BackCount = 0;
    }

    public void resetSkipCount() {
        this.skipCount = 0;
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    public void addVisitedDimension(String dimension) {
        this.visitedDimensions.add(dimension);
    }

    public void addVisitedBiome(BiomeGroup biome) {
        this.visitedBiomes.add(biome);
    }

}
