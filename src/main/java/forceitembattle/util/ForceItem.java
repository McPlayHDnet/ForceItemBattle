package forceitembattle.util;

import org.bukkit.Material;

public record ForceItem(Material material, String timeNeeded, long timeStamp, boolean isBackToBack, boolean usedSkip) {}