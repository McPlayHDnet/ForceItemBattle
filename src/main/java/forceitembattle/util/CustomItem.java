package forceitembattle.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class CustomItem {

    private final Material material;
    private final int customModelData;
    private final String checkedName;

    public CustomItem(Material material) {
        this(material, 0, null);
    }
}
