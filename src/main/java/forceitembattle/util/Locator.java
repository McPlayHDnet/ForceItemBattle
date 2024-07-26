package forceitembattle.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class Locator {

    private String structureId;
    private String structureName;
    private Material locatorMaterial;

}
