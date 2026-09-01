package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ActiveTrader;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.model.Locator;
import forceitembattle.model.TraderKind;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.view.MerchantView;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns and owns every trader in the round. The wandering trader arrives on its own timer;
 * the special trader is spawned by the random-event system. Both can be alive at once.
 */
@Getter
public class WanderingTraderManager implements Manager {

    private static final int SPAWN_CHUNK_RADIUS = 5;
    private static final int SPAWN_ATTEMPTS = 40;
    private static final int SPAWN_RETRY_SECONDS = 20;
    private static final int TRADER_LIFETIME_SECONDS = 5 * 60;
    /** Entering this spawn radius replays the trader direction line... */
    private static final int SPAWN_PING_RADIUS = 60;
    /** ...and it re-arms once the player left this radius (hysteresis against boundary flapping). */
    private static final int SPAWN_PING_EXIT_RADIUS = 80;

    private static final int SPECIAL_WHEEL_AMOUNT = 3;
    private static final int SPECIAL_WHEEL_PRICE = 1;
    private static final int SPECIAL_PRICE = 5;
    private static final int SPECIAL_MAX_USES = 1;
    private static final int SPECIAL_ENCHANT_LEVELS = 30;
    private static final Material[] SPECIAL_ARMOUR = {
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
    };

    private final ForceItemBattle plugin;

    /** Live traders, keyed by entity uuid. Insertion-ordered so the tab list is stable. */
    private final Map<UUID, ActiveTrader> traders = new LinkedHashMap<>();

    /** Player uuid -> the trader whose merchant they currently have open. */
    private final Map<UUID, UUID> tradingPlayers = new HashMap<>();

    /** Players currently inside the spawn ping zone; entering it replays the trader direction line. */
    private final Set<UUID> nearSpawnPlayers = new HashSet<>();

    private int randomAfterStartSpawnTime;
    private int timer;
    private BukkitTask spawnTimerTask;

    public WanderingTraderManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.randomAfterStartSpawnTime = ThreadLocalRandom.current().nextInt(7, 11) * 60; // [7, 10] minutes
        this.timer = this.randomAfterStartSpawnTime;
    }

    @Override
    public void disable() {
        if (this.spawnTimerTask != null) {
            this.spawnTimerTask.cancel();
            this.spawnTimerTask = null;
        }

        this.traders.values().forEach(trader -> {
            if (trader.getTask() != null) {
                trader.getTask().cancel();
            }
        });
        this.traders.clear();
        this.tradingPlayers.clear();
        this.nearSpawnPlayers.clear();
    }

    public void startTimer() {
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getRoundPhase().roundRunning()) {
                    return;
                }

                if (timer <= 0) {
                    if (spawnWanderingTrader()) {
                        randomAfterStartSpawnTime = TRADER_LIFETIME_SECONDS + ThreadLocalRandom.current().nextInt(7, 11) * 60;
                        timer = randomAfterStartSpawnTime;
                    } else {
                        // No solid ground found near spawn (an ocean start, say). Retry shortly
                        // rather than skipping the trader for another 7-10 minutes.
                        timer = SPAWN_RETRY_SECONDS;
                    }
                } else {
                    timer--;
                }

                rePingTradersNearSpawn();
            }
        };

        this.spawnTimerTask = bukkitRunnable.runTaskTimer(this.plugin, 0L, 20L);
    }

    public boolean spawnWanderingTrader() {
        return this.spawnTrader(TraderKind.WANDERING) != null;
    }

    public boolean spawnSpecialTrader() {
        return this.spawnTrader(TraderKind.SPECIAL) != null;
    }

    @Nullable
    private ActiveTrader spawnTrader(TraderKind kind) {
        World world = Dimension.OVERWORLD.world();
        if (world == null) return null;

        Location location = this.findSolidSpawnLocation(world.getSpawnLocation(), SPAWN_CHUNK_RADIUS);
        if (location == null) {
            this.plugin.getLogger().warning("Found no solid ground for the " + kind.getDisplayName()
                    + " near spawn after " + SPAWN_ATTEMPTS + " attempts");
            return null;
        }

        WanderingTrader entity = (WanderingTrader) world.spawnEntity(
                location.clone().add(0.0, 1.0, 0.0), EntityType.WANDERING_TRADER);
        entity.setGlowing(true);
        entity.setInvulnerable(true);
        entity.setAI(false);
        entity.setGravity(true);

        // The wandering trader stays anonymous; the special one is worth walking to.
        if (kind == TraderKind.SPECIAL) {
            entity.customName(Text.of(kind.boldColoredName()));
            entity.setCustomNameVisible(true);
        }

        List<MerchantRecipe> recipes = switch (kind) {
            case WANDERING -> this.wanderingRecipes(entity);
            case SPECIAL -> this.specialRecipes();
        };

        // Only a template: right-clicking the entity is intercepted and each player is handed their
        // own merchant, so nobody ever opens this recipe list.
        entity.setRecipes(recipes);

        ActiveTrader trader = new ActiveTrader(entity.getUniqueId(), kind, location, recipes);
        trader.setTimer(TRADER_LIFETIME_SECONDS);
        this.traders.put(trader.getUuid(), trader);
        this.plugin.getScoreboardManager().updateAllPlayers();

        this.announce(trader);
        trader.setTask(this.startDespawnTimer(trader, entity));

        return trader;
    }

    private BukkitTask startDespawnTimer(ActiveTrader trader, WanderingTrader entity) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (trader.getTimer() <= 0 || entity.isDead()) {
                    despawn(trader, entity);
                    cancel();
                    return;
                }

                if (plugin.getRoundPhase().isPausedGame()) {
                    return; // the trader's lifetime freezes while the game is paused
                }

                trader.setTimer(trader.getTimer() - 1);
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    /**
     * Replays the direction line for players who (re-)enter the spawn area, since the one-shot line
     * on spawn is long gone by then. Zone membership is tracked continuously, so a trader spawning
     * while a player already stands at spawn does not ping them twice.
     */
    private void rePingTradersNearSpawn() {
        World world = Dimension.OVERWORLD.world();
        if (world == null) return;

        Location spawn = world.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != world) {
                this.nearSpawnPlayers.remove(player.getUniqueId());
                continue;
            }

            double distance = player.getLocation().distance(spawn);
            if (distance <= SPAWN_PING_RADIUS) {
                if (this.nearSpawnPlayers.add(player.getUniqueId()) && !this.traders.isEmpty()) {
                    this.traders.values().forEach(trader -> this.plugin.getPositionManager()
                            .playParticleLine(player, trader.getLocation(), trader.getKind().getParticleColor()));
                }
            } else if (distance > SPAWN_PING_EXIT_RADIUS) {
                this.nearSpawnPlayers.remove(player.getUniqueId());
            }
        }
    }

    private void despawn(ActiveTrader trader, Entity entity) {
        entity.remove();
        this.traders.remove(trader.getUuid());
        this.tradingPlayers.values().removeIf(traderUuid -> traderUuid.equals(trader.getUuid()));

        Bukkit.broadcast(Text.of(Prefix.POSITION + "<gray>The " + trader.getKind().coloredName()
                + " <gray>just despawned! :("));
    }

    private void announce(ActiveTrader trader) {
        this.plugin.getRoster().players().values().forEach(forceItemPlayer -> {
            Player player = forceItemPlayer.player();

            player.sendMessage(Text.of(Prefix.POSITION + "<gray>The " + trader.getKind().coloredName()
                    + " <gray>just spawned at "
                    + LocationFormat.xyz(trader.getLocation())
                    + LocationFormat.distance(player.getLocation(), trader.getLocation())));

            this.plugin.getPositionManager().playParticleLine(player, trader.getLocation(),
                    trader.getKind().getParticleColor());
        });

        if (trader.getKind() == TraderKind.SPECIAL) {
            Bukkit.getOnlinePlayers().forEach(players ->
                    players.playSound(players.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1));
        }
    }

    /** Vanilla's offers, normalised to a single-item price and unlimited uses, plus the wheel. */
    private List<MerchantRecipe> wanderingRecipes(WanderingTrader entity) {
        List<MerchantRecipe> recipes = new ArrayList<>(entity.getRecipes());

        recipes.forEach(recipe -> {
            List<ItemStack> ingredients = recipe.getIngredients();
            ingredients.forEach(ingredient -> ingredient.setAmount(1));
            recipe.setIngredients(ingredients);
        });

        MerchantRecipe wheel = new MerchantRecipe(CustomMaterials.WHEEL_OF_FORTUNE.itemStack(), Integer.MAX_VALUE);
        wheel.addIngredient(new ItemStack(Material.EMERALD, 1));
        recipes.add(wheel);

        return recipes;
    }

    /** Five one-emerald offers, rolled once at spawn so everyone sees the same trader. */
    private List<MerchantRecipe> specialRecipes() {
        List<MerchantRecipe> recipes = new ArrayList<>();

        recipes.add(this.specialOffer(CustomMaterials.WHEEL_OF_FORTUNE.itemStack(SPECIAL_WHEEL_AMOUNT), SPECIAL_WHEEL_PRICE));
        recipes.add(this.specialOffer(CustomMaterials.WEATHERED_CAPTAINS_JOURNAL.itemStack(), SPECIAL_PRICE));

        Locator locator = this.plugin.getLocatorManager().randomLocator();
        if (locator != null) {
            recipes.add(this.specialOffer(locator.getLocatorItem().itemStack(), SPECIAL_PRICE));
        }

        recipes.add(this.specialOffer(this.enchanted(new ItemStack(Material.IRON_PICKAXE)), SPECIAL_PRICE));

        Material armour = SPECIAL_ARMOUR[ThreadLocalRandom.current().nextInt(SPECIAL_ARMOUR.length)];
        recipes.add(this.specialOffer(this.enchanted(new ItemStack(armour)), SPECIAL_PRICE));

        return recipes;
    }

    private MerchantRecipe specialOffer(ItemStack result, int emeralds) {
        MerchantRecipe recipe = new MerchantRecipe(result, SPECIAL_MAX_USES);
        recipe.addIngredient(new ItemStack(Material.EMERALD, emeralds));
        recipe.setExperienceReward(false);
        recipe.setPriceMultiplier(0.0F);
        return recipe;
    }

    /** A vanilla level-30 table roll. Treasure is excluded, so no mending — a table would not give it. */
    private ItemStack enchanted(ItemStack itemStack) {
        RegistryKeySet<Enchantment> tableEnchantments = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .getTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE);

        return itemStack.enchantWithLevels(SPECIAL_ENCHANT_LEVELS, tableEnchantments, ThreadLocalRandom.current());
    }

    @Nullable
    public ActiveTrader getTrader(UUID entityUuid) {
        return this.traders.get(entityUuid);
    }

    @Nullable
    public ActiveTrader traderOf(UUID playerUuid) {
        UUID traderUuid = this.tradingPlayers.get(playerUuid);
        return traderUuid == null ? null : this.traders.get(traderUuid);
    }

    public boolean isTrading(UUID playerUuid) {
        return this.tradingPlayers.containsKey(playerUuid);
    }

    public void stopTrading(UUID playerUuid) {
        this.tradingPlayers.remove(playerUuid);
    }

    public Collection<ActiveTrader> activeTraders() {
        return this.traders.values();
    }

    /**
     * This player's own view of {@code trader}, with their own use counts. Returns a view rather than
     * a {@link Merchant} because the title belongs to the builder now, not to the deprecated
     * {@code Bukkit.createMerchant}. The merchant is virtual — one per call, never shared — so
     * {@code checkReachable} is left alone: Paper documents it as having no effect on those.
     */
    public MerchantView createMerchantViewFor(Player player, ActiveTrader trader) {
        Merchant merchant = Bukkit.createMerchant();

        List<MerchantRecipe> templates = trader.getRecipes();
        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int index = 0; index < templates.size(); index++) {
            MerchantRecipe copy = this.copyOf(templates.get(index));
            copy.setUses(trader.usesOf(player.getUniqueId(), index));
            recipes.add(copy);
        }

        merchant.setRecipes(recipes);

        this.tradingPlayers.put(player.getUniqueId(), trader.getUuid());

        return MenuType.MERCHANT.builder()
                .merchant(merchant)
                .title(Text.of("<dark_gray>» " + trader.getKind().coloredName()))
                .build(player);
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
