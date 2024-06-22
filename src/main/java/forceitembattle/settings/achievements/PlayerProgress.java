package forceitembattle.settings.achievements;

import java.util.HashMap;
import java.util.Map;

public class PlayerProgress {

    private Map<Achievements, AchievementProgress> achievementProgressMap;

    public PlayerProgress() {
        this.achievementProgressMap = new HashMap<>();
        for(Achievements achievements : Achievements.values()) {
            this.achievementProgressMap.put(achievements, new AchievementProgress());
        }
    }

    public AchievementProgress getProgress(Achievements achievement) {
        return this.achievementProgressMap.get(achievement);
    }
}
