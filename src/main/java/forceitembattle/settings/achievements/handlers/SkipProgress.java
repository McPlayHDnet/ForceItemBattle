package forceitembattle.settings.achievements.handlers;

public class SkipProgress implements ProgressTracker {
    public int skipCount = 0;
    public long itemReceivedTime = 0;
    public boolean firstEvent = true;
}