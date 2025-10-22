package forceitembattle.settings.achievements.handlers;

import org.bukkit.Material;

public class BackToBackProgress implements ProgressTracker {
    public int b2bCount = 0;
    public Material lastItemType = null;
    public Material lastSkippedItem = null;
}