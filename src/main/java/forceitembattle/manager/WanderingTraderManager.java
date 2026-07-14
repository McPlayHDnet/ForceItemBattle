package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@Setter
@Getter
public class WanderingTraderManager implements Manager {

    private static final int SPAWN_CHUNK_RADIUS = 5;
    private static final int SPAWN_ATTEMPTS = 40;
    private static final int SPAWN_RETRY_SECONDS = 20;

    private final ForceItemBattle plugin;
    private final Map<UUID, Boolean> canBuyWheel;

    private final Set<UUID> tradingPlayers;

    private int randomAfterStartSpawnTime, timer, traderTimer;
    private BukkitTask spawnTimerTask;
    private BukkitTask traderTask;
    private Location traderLocation;
    private boolean traderActive;

    /** Our trader, as distinct from any wandering trader that spawns naturally. */
    private UUID traderUuid;

    /** The offers this trader was spawned with, copied per player into their own merchant. */
    private List<MerchantRecipe> traderRecipes;

    public WanderingTraderManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.randomAfterStartSpawnTime = ThreadLocalRandom.current().nextInt(7, 11) * 60; //random number between 7 and 10 -> [7, 10]
        this.timer = this.randomAfterStartSpawnTime;
        this.canBuyWheel = new HashMap<>();
        this.tradingPlayers = new HashSet<>();
    }

    @Override
    public void disable() {
        if (this.spawnTimerTask != null) {
            this.spawnTimerTask.cancel();
            this.spawnTimerTask = null;
        }
        if (this.traderTask != null) {
            this.traderTask.cancel();
            this.traderTask = null;
        }
    }

    public void startTimer() {
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getGamemanager().isMidGame()) {
                    return;
                }

                if (timer <= 0) {
                    if (spawnWanderingTrader()) {
                        randomAfterStartSpawnTime = (new Random().nextInt(4) + 7) * 60;
                        timer = randomAfterStartSpawnTime;
                    } else {
                        // No solid ground found near spawn (an ocean start, say). Retry shortly
                        // rather than skipping the trader for another 7-10 minutes.
                        timer = SPAWN_RETRY_SECONDS;
                    }
                } else {
                    timer--;
                }
            }
        };

        this.spawnTimerTask = bukkitRunnable.runTaskTimer(this.plugin, 0L, 20L);
    }

    public boolean spawnWanderingTrader() {
        World world = Dimension.OVERWORLD.world();
        if (world == null) return false;

        Location traderLocation = this.findSolidSpawnLocation(world.getSpawnLocation(), SPAWN_CHUNK_RADIUS);
        if (traderLocation == null) {
            this.plugin.getLogger().warning("Found no solid ground for the Wandering Trader near spawn after "
                    + SPAWN_ATTEMPTS + " attempts; retrying in " + SPAWN_RETRY_SECONDS + "s");
            return false;
        }

        WanderingTrader wanderingTrader = (WanderingTrader) world.spawnEntity(traderLocation.clone().add(0.0, 1.0, 0.0), EntityType.WANDERING_TRADER);
        wanderingTrader.setGlowing(true);
        wanderingTrader.setInvulnerable(true);
        wanderingTrader.setAI(false);
        wanderingTrader.setGravity(true);
        List<MerchantRecipe> merchantRecipes = new ArrayList<>(wanderingTrader.getRecipes());

        merchantRecipes.forEach(merchantReciper -> {
            List<ItemStack> ingredients = merchantReciper.getIngredients();
            ingredients.forEach(ingredient -> ingredient.setAmount(1));
            merchantReciper.setIngredients(ingredients);
            merchantReciper.setMaxUses(Integer.MAX_VALUE);
        });

        MerchantRecipe merchantRecipe = new MerchantRecipe(CustomMaterials.WHEEL_OF_FORTUNE.itemStack(), Integer.MAX_VALUE);
        merchantRecipe.addIngredient(new ItemStack(Material.EMERALD, 1));
        merchantRecipes.add(merchantRecipe);

        wanderingTrader.setRecipes(merchantRecipes);

        // The entity is now only a marker: right-clicking it is intercepted and each player is
        // handed their own merchant, so its own recipe list is never actually opened by anyone.
        this.traderUuid = wanderingTrader.getUniqueId();
        this.traderRecipes = List.copyOf(merchantRecipes);
        this.tradingPlayers.clear();

        this.canBuyWheel.clear();
        this.plugin.getGamemanager().forceItemPlayerMap().values().forEach(players -> {
            this.canBuyWheel.put(players.player().getUniqueId(), Boolean.TRUE);

            players.player().sendMessage(Text.of(Prefix.POSITION + "<gray>The <green>Wandering Trader <gray>just spawned at "
                    + LocationFormat.xyz(traderLocation)
                    + LocationFormat.distance(players.player().getLocation(), traderLocation)));
            this.plugin.getPositionManager().playParticleLine(players.player(), traderLocation, Color.LIME);
        });

        this.traderLocation = traderLocation;
        this.traderTimer = 5 * 60;
        this.traderActive = true;

        this.traderTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (traderTimer <= 0 || wanderingTrader.isDead()) {
                    traderActive = false;
                    traderUuid = null;
                    traderRecipes = null;
                    tradingPlayers.clear();
                    wanderingTrader.remove();
                    cancel();
                    Bukkit.broadcast(Text.of(Prefix.POSITION + "<gray>The <green>Wandering Trader <gray>just despawned! :("));
                    return;
                }

                traderTimer--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);

        return true;
    }

    public Merchant createMerchantFor(Player player) {
        Merchant merchant = Bukkit.createMerchant(Text.of("<dark_gray>» <green>Wandering Trader"));

        List<MerchantRecipe> recipes = new ArrayList<>();
        for (MerchantRecipe template : this.traderRecipes) {
            recipes.add(this.copyOf(template));
        }
        merchant.setRecipes(recipes);

        this.tradingPlayers.add(player.getUniqueId());
        return merchant;
    }

    private MerchantRecipe copyOf(MerchantRecipe source) {
        MerchantRecipe copy = new MerchantRecipe(source.getResult().clone(), source.getMaxUses());
        for (ItemStack ingredient : source.getIngredients()) {
            copy.addIngredient(ingredient.clone());
        }
        copy.setExperienceReward(source.hasExperienceReward());
        copy.setVillagerExperience(source.getVillagerExperience());
        copy.setPriceMultiplier(source.getPriceMultiplier());
        return copy;
    }

    private Location findSolidSpawnLocation(Location center, int chunkRadius) {
        World world = center.getWorld();
        if (world == null) return null;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double offsetX = (Math.random() - 0.5) * chunkRadius * 16 * 2;
            double offsetZ = (Math.random() - 0.5) * chunkRadius * 16 * 2;

            int blockX = (int) Math.floor(center.getX() + offsetX);
            int blockZ = (int) Math.floor(center.getZ() + offsetZ);

            Block ground = world.getHighestBlockAt(blockX, blockZ);
            if (!ground.getType().isSolid()) {
                continue; // water, lava, or a non-collidable plant
            }

            // Centre of the block, so the trader can't spawn clipped into a neighbouring wall.
            return new Location(world, blockX + 0.5, ground.getY(), blockZ + 0.5);
        }

        return null;
    }
}
