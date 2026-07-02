package forceitembattle.achievements.handlers;

public class SkipProgress implements ProgressTracker {
    public int skipCount = 0;
    public int itemReceivedSecondsLeft = 0;
    public boolean firstEvent = true;
}