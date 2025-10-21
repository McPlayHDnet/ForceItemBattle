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

    private int itemCount, back2BackCount, consecutiveCount, skipCount, deathCounter, tradeCount, lootCount;
    private final Set<String> visitedDimensions;
    private final Set<BiomeGroup> visitedBiomes;
    private final Set<Material> woodTypesCollected;
    private final Set<Material> stoneTypesCollected;
    private final Set<Material> lootedMaterials;
    private long startTime, lastItemTime, itemReceivedTime;
    private Material lastItemType, lastSkippedItem;
    private boolean lastItemBackToBack, firstItemCollected;

    public AchievementProgress() {
        this.itemCount = 0;
        this.back2BackCount = 0;
        this.consecutiveCount = 0;
        this.skipCount = 0;
        this.deathCounter = 0;
        this.tradeCount = 0;
        this.lootCount = 0;
        this.visitedDimensions = new HashSet<>();
        this.visitedBiomes = new HashSet<>();
        this.woodTypesCollected = new HashSet<>();
        this.stoneTypesCollected = new HashSet<>();
        this.lootedMaterials = new HashSet<>();
        this.startTime = System.currentTimeMillis();
        this.lastItemTime = System.currentTimeMillis();
        this.itemReceivedTime = System.currentTimeMillis();
        this.lastItemType = null;
        this.lastSkippedItem = null;
        this.lastItemBackToBack = false;
        this.firstItemCollected = false;
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

    public void incrementTradeCount() {
        this.tradeCount++;
    }

    public void incrementLootCount() {
        this.lootCount++;
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

    public void resetTradeCount() {
        this.tradeCount = 0;
    }

    public void resetLootCount() {
        this.lootCount = 0;
    }

    public void addVisitedDimension(String dimension) {
        this.visitedDimensions.add(dimension);
    }

    public void addVisitedBiome(BiomeGroup biome) {
        this.visitedBiomes.add(biome);
    }

    public void addWoodType(Material wood) {
        this.woodTypesCollected.add(wood);
    }

    public void addStoneType(Material stone) {
        this.stoneTypesCollected.add(stone);
    }

    public void addLootedMaterial(Material material) {
        this.lootedMaterials.add(material);
    }
}