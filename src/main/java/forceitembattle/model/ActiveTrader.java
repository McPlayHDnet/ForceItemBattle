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

    /**
     * One wheel purchase per player per spawn. Only the wandering trader enforces this — its
     * wheel offer has unlimited uses, whereas the special trader's offers are capped at one each.
     */
    private final Map<UUID, Boolean> canBuyWheel = new HashMap<>();

    /**
     * Per-player use counts, keyed by recipe index. Merchants are rebuilt from the templates on
     * every open, and a fresh MerchantRecipe starts at zero uses — so the count has to live here,
     * on the trader, or players could reset their own limits by closing and reopening.
     */
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
