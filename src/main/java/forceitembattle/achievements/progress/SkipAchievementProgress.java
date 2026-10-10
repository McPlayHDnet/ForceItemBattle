package forceitembattle.achievements.progress;

import lombok.ToString;

@ToString
public class SkipAchievementProgress {
    public int skipCount = 0;
    public int itemReceivedSecondsLeft = 0;
    public boolean firstEvent = true;
}
