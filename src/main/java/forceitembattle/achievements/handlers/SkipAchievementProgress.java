package forceitembattle.achievements.handlers;

public class SkipAchievementProgress implements AchievementProgressTracker {
    public int skipCount = 0;
    public int itemReceivedSecondsLeft = 0;
    public boolean firstEvent = true;
}
