package forceitembattle.achievements.handlers;

public class TimeProgress implements ProgressTracker {
    public int count = 0;
    public int lastItemSecondsLeft = -1;
    public int itemReceivedSecondsLeft = -1;
    public boolean initialized = false;
    public boolean firstItemCollected = false;
    public boolean hasSkipped = false;
}