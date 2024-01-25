package forceitembattle.util;

import org.bukkit.Material;

public class ForceItem {

    private Material material;
    private String timeNeeded;
    private boolean usedSkip;

    public ForceItem(Material material, String timeNeeded, boolean usedSkip) {
        this.material = material;
        this.timeNeeded = timeNeeded;
        this.usedSkip = usedSkip;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getTimeNeeded() {
        return timeNeeded;
    }

    public void setTimeNeeded(String timeNeeded) {
        this.timeNeeded = timeNeeded;
    }

    public boolean isUsedSkip() {
        return usedSkip;
    }

    public void setUsedSkip(boolean usedSkip) {
        this.usedSkip = usedSkip;
    }
}
