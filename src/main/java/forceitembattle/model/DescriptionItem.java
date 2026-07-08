package forceitembattle.model;

import java.util.List;
import org.bukkit.Material;

public record DescriptionItem(Material material, List<String> lines) {
}
