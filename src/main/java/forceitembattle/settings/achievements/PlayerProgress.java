package forceitembattle.settings.achievements;

import java.util.HashMap;
import java.util.Map;

public class PlayerProgress {

    private final Map<Achievements, AchievementProgress> progressMap;

    public PlayerProgress() {
        this.progressMap = new HashMap<>();
        for (Achievements achievement : Achievements.values()) {
            this.progressMap.put(achievement, new AchievementProgress());
        }
    }

    public AchievementProgress getProgress(Achievements achievement) {
        return this.progressMap.get(achievement);
    }

    public Map<Achievements, AchievementProgress> getAllProgress() {
        return this.progressMap;
    }
}