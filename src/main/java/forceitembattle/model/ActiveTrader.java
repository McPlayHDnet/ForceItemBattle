package forceitembattle.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class ActiveTrader {

    private final UUID uuid;
    private final TraderKind kind;
    private final Location location;

    /** The offers this trader was spawned with, copied per player into their own merchant. */
    private final List<MerchantRecipe> recipes;

    /** Only the wandering trader needs this: its wheel offer has unlimited uses. */
    private final Map<UUID, Boolean> canBuyWheel = new HashMap<>();

    /** Lives on the trader because merchants are rebuilt on every open, which would reset the counts. */
    private final Map<UUID, Map<Integer, Integer>> uses = new HashMap<>();

    public int usesOf(UUID playerUuid, int recipeIndex) {
        return this.uses.getOrDefault(playerUuid, Map.of()).getOrDefault(recipeIndex, 0);
    }

    public void recordUse(UUID playerUuid, int recipeIndex) {
        this.uses.computeIfAbsent(playerUuid, uuid -> new HashMap<>())
                .merge(recipeIndex, 1, Integer::sum);
    }

    @Setter
    private int timer;

    @Setter
    private BukkitTask task;

    public ActiveTrader(UUID uuid, TraderKind kind, Location location, List<MerchantRecipe> recipes) {
        this.uuid = uuid;
        this.kind = kind;
        this.location = location;
        this.recipes = List.copyOf(recipes);
    }
}
