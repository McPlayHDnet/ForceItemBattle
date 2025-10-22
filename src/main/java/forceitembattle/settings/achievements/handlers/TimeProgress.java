package forceitembattle.settings.achievements.handlers;

public class TimeProgress implements ProgressTracker {
    public int count = 0;
    public long gameStartTime = System.currentTimeMillis();
    public long lastItemTime = System.currentTimeMillis();
    public long itemReceivedTime = System.currentTimeMillis();
    public boolean firstItemCollected = false;
    public boolean hasSkipped = false;
}