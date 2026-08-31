package forceitembattle.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.QuickieMode;
import forceitembattle.model.DescriptionItem;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class ItemDifficultiesManager implements Manager {

    private static final Random RANDOM = new Random();

    private final ForceItemBattle plugin;
    @Getter
    private final Map<Material, ItemDefinition> itemRegistry = new HashMap<>();
    private final Random FIXED_RANDOM;
    private final long FIXED_RANDOM_SEED;
    @Getter
    private HashMap<Material, DescriptionItem> descriptionItems;

    private Map<Material, String> smallIconUnicodes;
    private Map<Material, String> bigIconUnicodes;

    // Item pools (states) already announced as unlocked this game, so each pool is
    // only reported once. Cleared per game via resetUnlockAnnouncements().
    private final Set<State> announcedUnlockedStates = EnumSet.noneOf(State.class);

    /**
     * When each pool opens. Per-round state, so it belongs to the manager and not to the
     * {@link State} enum constants — but the arithmetic behind it is {@link UnlockSchedule}'s.
     */
    private UnlockSchedule unlockSchedule = UnlockSchedule.percentageBased();

    /** Materials per pool, derived from {@link #itemRegistry} once at enable. */
    private final Map<State, List<Material>> itemsByState = new EnumMap<>(State.class);

    /**
     * The generation pool for the current tick: every unlocked item minus the ones the current
     * settings exclude.
     *
     * Cached against {@link PoolKey} because a rebuild walks all ~1,367 registered items and
     * re-reads three settings per item, while {@link #getAvailableItems()} is called on every item
     * generation, every back-to-back probability, and once per candidate material when the /items
     * menu paints — uncached, that last one is an O(n²) scan for a single GUI open.
     */
    private List<Material> availablePool;
    private Set<Material> availablePoolIndex;
    private PoolKey availablePoolKey;

    /** Everything {@link #getAvailableItems()} depends on; a change in any of it rebuilds the pool. */
    private record PoolKey(List<State> activeStates, QuickieMode quickieMode,
                           boolean hard, boolean extreme, boolean end) {
    }

    public ItemDifficultiesManager(ForceItemBattle forceItemBattle) {
        this.plugin = forceItemBattle;
        this.FIXED_RANDOM_SEED = new Random().nextLong();
        this.FIXED_RANDOM = new Random(FIXED_RANDOM_SEED);
        this.descriptionItems = new HashMap<>();
    }

    @Override
    public void enable() {
        registerAllItems();
        for (State state : State.VALUES) {
            itemsByState.put(state, getItemsByState(state));
        }
        useDefaultUnlockSchedule();
        loadDescriptions();
    }

    /** Resets to the percentage-based schedule. */
    public void useDefaultUnlockSchedule() {
        this.applySchedule(UnlockSchedule.percentageBased());
    }

    /** Picks the unlock schedule for a round of {@code durationMinutes}. */
    public void configureUnlockSchedule(int durationMinutes) {
        this.applySchedule(UnlockSchedule.forRound(durationMinutes));
    }

    private void applySchedule(UnlockSchedule schedule) {
        this.unlockSchedule = schedule;
        invalidateAvailablePool();
    }

    private void loadDescriptions() {
        if (!this.plugin.getConfig().isConfigurationSection("descriptions")) {
            return;
        }
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("descriptions");
        if (section == null) {
            throw new NullPointerException("'descriptions' does not exist in the config.yml");
        }
        for (String key : section.getKeys(false)) {
            List<String> descriptions = section.getStringList(key);
            Material material = Material.valueOf(key.toUpperCase());
            this.descriptionItems.put(material, new DescriptionItem(material, descriptions));
        }
    }

    public List<Material> getNetherItems() {
        return getItemsByTag(ItemTag.NETHER);
    }

    public List<Material> getEndItems() {
        return getItemsByTag(ItemTag.END);
    }

    public List<Material> getExtremeItems() {
        return getItemsByTag(ItemTag.EXTREME);
    }

    public List<Material> getItemsByTag(ItemTag tag) {
        return itemRegistry.values().stream()
                .filter(def -> def.hasTag(tag))
                .map(ItemDefinition::material)
                .toList();
    }

    public List<Material> getItemsByState(State state) {
        return itemRegistry.values().stream()
                .filter(def -> def.state() == state)
                .map(ItemDefinition::material)
                .toList();
    }

    /**
     * The pool (state) an item belongs to, or {@code null} for a material that isn't a pool item
     * (a custom or unregistered material). The registry is the single source of truth for this.
     */
    @Nullable
    public State getState(Material material) {
        ItemDefinition definition = this.itemRegistry.get(material);
        return definition == null ? null : definition.state();
    }

    public Set<Material> getItemsIn(Dimension dimension) {
        return switch (dimension) {
            case OVERWORLD -> getOverworldItems();
            case NETHER -> Set.copyOf(getNetherItems());
            case END -> Set.copyOf(getEndItems());
        };
    }

    /** Everything not tagged NETHER or END. */
    public Set<Material> getOverworldItems() {
        return itemRegistry.values().stream()
                .filter(def -> !def.hasAnyTag(ItemTag.NETHER, ItemTag.END))
                .map(ItemDefinition::material)
                .collect(Collectors.toSet());
    }

    public Set<Material> getAllItems() {
        return new HashSet<>(itemRegistry.keySet());
    }

    /**
     * All registered items except EXTREME-tagged ones. Source for the collection book and the
     * collection achievement: EXTREME items are excluded because they're unrealistic to obtain,
     * so requiring them would make "collect everything" effectively uncompletable.
     */
    public Set<Material> getCollectableItems() {
        return itemRegistry.values().stream()
                .filter(def -> !def.hasTag(ItemTag.EXTREME))
                .map(ItemDefinition::material)
                .collect(Collectors.toSet());
    }

    /**
     * The items a force item can currently be drawn from: every unlocked pool, minus what the
     * current settings exclude.
     *
     * Cached — see {@link #availablePool} — and unmodifiable: the returned list is the live pool
     * every other reader shares this tick, so a caller that needs to sort or shuffle must copy
     * first. An in-place edit fails loudly rather than corrupting it for everyone.
     */
    public List<Material> getAvailableItems() {
        refreshAvailablePool();
        return this.availablePool;
    }

    /** Whether {@code material} is in the current generation pool. O(1), unlike scanning the list. */
    public boolean itemInList(Material material) {
        refreshAvailablePool();
        return this.availablePoolIndex.contains(material);
    }

    private void refreshAvailablePool() {
        GameSettings settings = this.plugin.getSettings();
        PoolKey key = new PoolKey(
                getActiveStates(),
                settings.getQuickieMode(),
                settings.isSettingEnabled(GameSetting.HARD),
                settings.isSettingEnabled(GameSetting.EXTREME),
                settings.isSettingEnabled(GameSetting.END));

        if (key.equals(this.availablePoolKey) && this.availablePool != null) {
            return;
        }

        List<Material> items = new ArrayList<>();
        for (State state : key.activeStates()) {
            items.addAll(itemsByState.getOrDefault(state, List.of()));
        }
        filterDisabledItems(items);

        this.availablePool = List.copyOf(items);
        this.availablePoolIndex = Set.copyOf(items);
        this.availablePoolKey = key;
    }

    /**
     * Drops the cached pool so the next read rebuilds it. Needed when something the {@link PoolKey}
     * cannot see changes — the unlock schedule, or the registry itself.
     */
    private void invalidateAvailablePool() {
        this.availablePoolKey = null;
    }

    /**
     * Returns the item pools (states) that have just become available this tick —
     * unlocked by elapsed time and permitted by the current quickie mode — and
     * haven't been announced yet. Each returned state is marked announced, so a
     * pool is only ever reported once per game. EARLY is the baseline start pool
     * and is never announced.
     */
    public List<State> pollNewlyUnlockedStates() {
        announcedUnlockedStates.add(State.EARLY); // baseline pool, never announced

        List<State> newlyUnlocked = new ArrayList<>();
        for (State state : getActiveStates()) {
            if (announcedUnlockedStates.add(state)) {
                newlyUnlocked.add(state);
            }
        }
        return newlyUnlocked;
    }

    /**
     * Clears the per-game record of which pools have been announced, so unlocks
     * are announced again next round. Call at the start of each game.
     */
    public void resetUnlockAnnouncements() {
        announcedUnlockedStates.clear();
    }

    /**
     * Item pools currently active this tick: permitted by the current quickie mode
     * and unlocked by elapsed time. Returned in unlock order (EARLY → LATE).
     */
    public List<State> getActiveStates() {
        return this.unlockSchedule.activeAt(
                elapsedSeconds() / 60, roundSeconds(), this.plugin.getSettings().getQuickieMode());
    }

    /**
     * The next pool scheduled to unlock, or {@code null} when none remain — all
     * permitted pools are already active (e.g. capped by the current quickie mode).
     */
    public State getNextState() {
        return this.unlockSchedule.nextAfter(
                elapsedSeconds() / 60, roundSeconds(), this.plugin.getSettings().getQuickieMode());
    }

    /**
     * Seconds until the next pool unlocks, or {@code -1} when none remain. Reaches 0
     * on the same tick {@link #pollNewlyUnlockedStates()} announces that pool.
     */
    public int secondsUntilNextPool() {
        return this.unlockSchedule.secondsUntilNext(
                elapsedSeconds(), roundSeconds(), this.plugin.getSettings().getQuickieMode());
    }

    private int roundSeconds() {
        return this.plugin.getRoundClock().totalSeconds();
    }

    /**
     * The third copy of this subtraction, now retired. It read a duration off the game manager and
     * a remaining time off the timer manager — one of the two edges that made this class and
     * {@code TimerManager} mutually dependent, for arithmetic the clock can do itself.
     */
    private int elapsedSeconds() {
        return this.plugin.getRoundClock().elapsedSeconds();
    }

    public Material generateRandomMaterial() {
        return drawFrom(RANDOM);
    }

    public Material generateSeededRandomMaterial() {
        return drawFrom(FIXED_RANDOM);
    }

    private Material drawFrom(Random random) {
        List<Material> items = getAvailableItems();

        if (items.isEmpty()) {
            throw new IllegalStateException("No available items after filtering.");
        }

        return items.get(random.nextInt(items.size()));
    }

    /**
     * Filter out items based on game settings. Applied once when the pool is built, not per draw —
     * {@link #getAvailableItems()} already hands back a filtered list.
     */
    private void filterDisabledItems(Collection<Material> items) {
        GameSettings settings = this.plugin.getSettings();
        boolean hard = settings.isSettingEnabled(GameSetting.HARD);
        boolean extreme = settings.isSettingEnabled(GameSetting.EXTREME);
        boolean end = settings.isSettingEnabled(GameSetting.END);

        // Read once rather than three times per item: this walks all ~1,367 registered materials.
        items.removeIf(material ->
                PoolExclusions.isExcluded(itemRegistry.get(material), hard, extreme, end));
    }

    public boolean itemInAllLists(Material material) {
        return itemRegistry.containsKey(material);
    }

    public boolean isItemInDescriptionList(Material material) {
        return this.descriptionItems.containsKey(material);
    }

    public boolean itemHasDescription(Material material) {
        return this.descriptionItems.get(material) != null;
    }

    public List<String> getDescriptionItemLines(Material material) {
        if (!isItemInDescriptionList(material)) {
            return new ArrayList<>();
        }

        if (!itemHasDescription(material)) {
            throw new NullPointerException(material.name() + " does not have a description");
        }

        return this.descriptionItems.get(material)
                .lines()
                .stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList();
    }

    public String getUnicodeFromMaterial(boolean smallIcon, Material material) {
        return this.getItemUnicodes(smallIcon).getOrDefault(material, "NULL");
    }

    public Map<Material, String> getItemUnicodes(boolean smallIcon) {
        if (smallIcon) {
            if (smallIconUnicodes == null) {
                smallIconUnicodes = readItemUnicodes(true);
            }
            return smallIconUnicodes;
        }

        if (bigIconUnicodes == null) {
            bigIconUnicodes = readItemUnicodes(false);
        }
        return bigIconUnicodes;
    }

    private Map<Material, String> readItemUnicodes(boolean smallIcon) {
        Map<Material, String> itemsUnicode = new HashMap<>();
        File file = new File(this.plugin.getDataFolder(), "unicodeItems.json");

        if (!file.exists()) {
            this.plugin.getLogger().warning("`unicodeItems.json` does not exist, not using custom icons");
            return new HashMap<>();
        }

        try (FileReader fileReader = new FileReader(file)) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>[]>() {
            }.getType();
            Map<String, String>[] items = gson.fromJson(fileReader, mapType);

            if (items == null) {
                this.plugin.getLogger().warning("`unicodeItems.json` could not be parsed.");
                return new HashMap<>();
            }

            for (Map<String, String> entry : items) {
                String materialName = entry.get("material");
                String unicode = entry.get("unicode");

                if (materialName != null && unicode != null) {
                    if (smallIcon && materialName.contains("_tabChat")) {
                        Material material = Material.getMaterial(materialName.replace("_tabChat", ""));
                        if (material != null) {
                            itemsUnicode.put(material, unicode);
                        }
                    } else if (!smallIcon && !materialName.contains("_tabChat")) {
                        Material material = Material.getMaterial(materialName);
                        if (material != null) {
                            itemsUnicode.put(material, unicode);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return itemsUnicode;
    }

    private void register(Material material, State state, ItemTag... tags) {
        if (itemRegistry.containsKey(material)) {
            plugin.getLogger().warning("Duplicate registration: " + material.name());
        }
        itemRegistry.put(material, new ItemDefinition(material, state, tags));
    }

    private void registerAllItems() {
        register(Material.ACACIA_BOAT, State.EARLY);
        register(Material.ACACIA_BUTTON, State.EARLY);
        register(Material.ACACIA_CHEST_BOAT, State.EARLY);
        register(Material.ACACIA_DOOR, State.EARLY);
        register(Material.ACACIA_FENCE, State.EARLY);
        register(Material.ACACIA_FENCE_GATE, State.EARLY);
        register(Material.ACACIA_HANGING_SIGN, State.EARLY);
        register(Material.ACACIA_LEAVES, State.EARLY);
        register(Material.ACACIA_LOG, State.EARLY);
        register(Material.ACACIA_PLANKS, State.EARLY);
        register(Material.ACACIA_PRESSURE_PLATE, State.EARLY);
        register(Material.ACACIA_SAPLING, State.EARLY);
        register(Material.ACACIA_SHELF, State.EARLY);
        register(Material.ACACIA_SIGN, State.EARLY);
        register(Material.ACACIA_SLAB, State.EARLY);
        register(Material.ACACIA_STAIRS, State.EARLY);
        register(Material.ACACIA_TRAPDOOR, State.EARLY);
        register(Material.ACACIA_WOOD, State.EARLY);
        register(Material.ACTIVATOR_RAIL, State.MID);
        register(Material.ALLIUM, State.MID);
        register(Material.AMETHYST_BLOCK, State.MID);
        register(Material.AMETHYST_CLUSTER, State.LATE);
        register(Material.AMETHYST_SHARD, State.MID);
        register(Material.ANCIENT_DEBRIS, State.LATE, ItemTag.NETHER);
        register(Material.ANDESITE, State.EARLY);
        register(Material.ANDESITE_SLAB, State.EARLY);
        register(Material.ANDESITE_STAIRS, State.EARLY);
        register(Material.ANDESITE_WALL, State.EARLY);
        register(Material.ANGLER_POTTERY_SHERD, State.LATE);
        register(Material.ANVIL, State.LATE);
        register(Material.APPLE, State.EARLY);
        register(Material.ARCHER_POTTERY_SHERD, State.LATE);
        register(Material.ARMADILLO_SCUTE, State.MID);
        register(Material.ARMOR_STAND, State.EARLY);
        register(Material.ARROW, State.EARLY);
        register(Material.AXOLOTL_BUCKET, State.MID);
        register(Material.AZALEA, State.MID);
        register(Material.AZALEA_LEAVES, State.MID);
        register(Material.AZURE_BLUET, State.EARLY);
        register(Material.BAKED_POTATO, State.EARLY);
        register(Material.BAMBOO, State.EARLY);
        register(Material.BAMBOO_BLOCK, State.EARLY);
        register(Material.BAMBOO_BUTTON, State.EARLY);
        register(Material.BAMBOO_CHEST_RAFT, State.EARLY);
        register(Material.BAMBOO_DOOR, State.EARLY);
        register(Material.BAMBOO_FENCE, State.EARLY);
        register(Material.BAMBOO_FENCE_GATE, State.EARLY);
        register(Material.BAMBOO_HANGING_SIGN, State.EARLY);
        register(Material.BAMBOO_MOSAIC, State.EARLY);
        register(Material.BAMBOO_MOSAIC_SLAB, State.EARLY);
        register(Material.BAMBOO_MOSAIC_STAIRS, State.EARLY);
        register(Material.BAMBOO_PLANKS, State.EARLY);
        register(Material.BAMBOO_PRESSURE_PLATE, State.EARLY);
        register(Material.BAMBOO_RAFT, State.EARLY);
        register(Material.BAMBOO_SHELF, State.EARLY);
        register(Material.BAMBOO_SIGN, State.EARLY);
        register(Material.BAMBOO_SLAB, State.EARLY);
        register(Material.BAMBOO_STAIRS, State.EARLY);
        register(Material.BAMBOO_TRAPDOOR, State.EARLY);
        register(Material.BARREL, State.EARLY);
        register(Material.BASALT, State.MID, ItemTag.NETHER);
        register(Material.BEE_NEST, State.LATE);
        register(Material.BEEF, State.EARLY);
        register(Material.BEEHIVE, State.LATE);
        register(Material.BEETROOT, State.MID);
        register(Material.BEETROOT_SEEDS, State.MID);
        register(Material.BEETROOT_SOUP, State.MID);
        register(Material.BELL, State.EARLY);
        register(Material.BIG_DRIPLEAF, State.MID);
        register(Material.BIRCH_BOAT, State.EARLY);
        register(Material.BIRCH_BUTTON, State.EARLY);
        register(Material.BIRCH_CHEST_BOAT, State.EARLY);
        register(Material.BIRCH_DOOR, State.EARLY);
        register(Material.BIRCH_FENCE, State.EARLY);
        register(Material.BIRCH_FENCE_GATE, State.EARLY);
        register(Material.BIRCH_HANGING_SIGN, State.EARLY);
        register(Material.BIRCH_LEAVES, State.EARLY);
        register(Material.BIRCH_LOG, State.EARLY);
        register(Material.BIRCH_PLANKS, State.EARLY);
        register(Material.BIRCH_PRESSURE_PLATE, State.EARLY);
        register(Material.BIRCH_SAPLING, State.EARLY);
        register(Material.BIRCH_SHELF, State.EARLY);
        register(Material.BIRCH_SIGN, State.EARLY);
        register(Material.BIRCH_SLAB, State.EARLY);
        register(Material.BIRCH_STAIRS, State.EARLY);
        register(Material.BIRCH_TRAPDOOR, State.EARLY);
        register(Material.BIRCH_WOOD, State.EARLY);
        register(Material.BLACK_BANNER, State.EARLY);
        register(Material.BLACK_BED, State.EARLY);
        register(Material.BLACK_BUNDLE, State.EARLY);
        register(Material.BLACK_CANDLE, State.LATE);
        register(Material.BLACK_CARPET, State.EARLY);
        register(Material.BLACK_CONCRETE, State.EARLY);
        register(Material.BLACK_CONCRETE_POWDER, State.EARLY);
        register(Material.BLACK_DYE, State.EARLY);
        register(Material.BLACK_GLAZED_TERRACOTTA, State.MID);
        register(Material.BLACK_HARNESS, State.EARLY);
        register(Material.BLACK_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.BLACK_STAINED_GLASS, State.EARLY);
        register(Material.BLACK_STAINED_GLASS_PANE, State.EARLY);
        register(Material.BLACK_TERRACOTTA, State.EARLY);
        register(Material.BLACK_WOOL, State.EARLY);
        register(Material.BLACKSTONE, State.MID, ItemTag.NETHER);
        register(Material.BLACKSTONE_SLAB, State.MID, ItemTag.NETHER);
        register(Material.BLACKSTONE_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.BLACKSTONE_WALL, State.MID, ItemTag.NETHER);
        register(Material.BLADE_POTTERY_SHERD, State.LATE);
        register(Material.BLAST_FURNACE, State.EARLY);
        register(Material.BLAZE_POWDER, State.LATE, ItemTag.NETHER);
        register(Material.BLAZE_ROD, State.LATE, ItemTag.NETHER);
        register(Material.BLUE_BANNER, State.EARLY);
        register(Material.BLUE_BED, State.EARLY);
        register(Material.BLUE_BUNDLE, State.EARLY);
        register(Material.BLUE_CANDLE, State.LATE);
        register(Material.BLUE_CARPET, State.EARLY);
        register(Material.BLUE_CONCRETE, State.EARLY);
        register(Material.BLUE_CONCRETE_POWDER, State.EARLY);
        register(Material.BLUE_DYE, State.EARLY);
        register(Material.BLUE_EGG, State.LATE);
        register(Material.BLUE_GLAZED_TERRACOTTA, State.MID);
        register(Material.BLUE_HARNESS, State.EARLY);
        register(Material.BLUE_ICE, State.LATE);
        register(Material.BLUE_ORCHID, State.MID);
        register(Material.BLUE_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.BLUE_STAINED_GLASS, State.EARLY);
        register(Material.BLUE_STAINED_GLASS_PANE, State.EARLY);
        register(Material.BLUE_TERRACOTTA, State.EARLY);
        register(Material.BLUE_WOOL, State.EARLY);
        register(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.BONE, State.EARLY);
        register(Material.BONE_BLOCK, State.EARLY);
        register(Material.BONE_MEAL, State.EARLY);
        register(Material.BOOK, State.EARLY);
        register(Material.BOOKSHELF, State.EARLY);
        register(Material.BORDURE_INDENTED_BANNER_PATTERN, State.EARLY);
        register(Material.BOW, State.MID);
        register(Material.BOWL, State.EARLY);
        register(Material.BRAIN_CORAL, State.LATE);
        register(Material.BRAIN_CORAL_BLOCK, State.LATE);
        register(Material.BRAIN_CORAL_FAN, State.LATE);
        register(Material.BREAD, State.EARLY);
        register(Material.BREEZE_ROD, State.LATE);
        register(Material.BREWING_STAND, State.LATE);
        register(Material.BRICK, State.EARLY);
        register(Material.BRICK_SLAB, State.EARLY);
        register(Material.BRICK_STAIRS, State.EARLY);
        register(Material.BRICK_WALL, State.EARLY);
        register(Material.BRICKS, State.EARLY);
        register(Material.BROWN_BANNER, State.EARLY);
        register(Material.BROWN_BED, State.EARLY);
        register(Material.BROWN_BUNDLE, State.EARLY);
        register(Material.BROWN_CANDLE, State.LATE);
        register(Material.BROWN_CARPET, State.EARLY);
        register(Material.BROWN_CONCRETE, State.EARLY);
        register(Material.BROWN_CONCRETE_POWDER, State.EARLY);
        register(Material.BROWN_DYE, State.EARLY);
        register(Material.BROWN_EGG, State.LATE);
        register(Material.BROWN_GLAZED_TERRACOTTA, State.MID);
        register(Material.BROWN_HARNESS, State.EARLY);
        register(Material.BROWN_MUSHROOM, State.EARLY);
        register(Material.BROWN_MUSHROOM_BLOCK, State.LATE);
        register(Material.BROWN_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.BROWN_STAINED_GLASS, State.EARLY);
        register(Material.BROWN_STAINED_GLASS_PANE, State.EARLY);
        register(Material.BROWN_TERRACOTTA, State.EARLY);
        register(Material.BROWN_WOOL, State.EARLY);
        register(Material.BRUSH, State.EARLY);
        register(Material.BUBBLE_CORAL, State.LATE);
        register(Material.BUBBLE_CORAL_BLOCK, State.LATE);
        register(Material.BUBBLE_CORAL_FAN, State.LATE);
        register(Material.BUCKET, State.EARLY);
        register(Material.BUNDLE, State.EARLY);
        register(Material.BURN_POTTERY_SHERD, State.LATE);
        register(Material.BUSH, State.EARLY);
        register(Material.CACTUS, State.MID);
        register(Material.CACTUS_FLOWER, State.MID);
        register(Material.CAKE, State.LATE);
        register(Material.CALCITE, State.MID);
        register(Material.CALIBRATED_SCULK_SENSOR, State.LATE);
        register(Material.CAMPFIRE, State.EARLY);
        register(Material.CANDLE, State.LATE);
        register(Material.CARROT, State.EARLY);
        register(Material.CARROT_ON_A_STICK, State.MID);
        register(Material.CARTOGRAPHY_TABLE, State.EARLY);
        register(Material.CARVED_PUMPKIN, State.EARLY);
        register(Material.CAULDRON, State.EARLY);
        register(Material.CHAINMAIL_BOOTS, State.LATE, ItemTag.EXTREME);
        register(Material.CHAINMAIL_CHESTPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.CHAINMAIL_HELMET, State.LATE, ItemTag.EXTREME);
        register(Material.CHAINMAIL_LEGGINGS, State.LATE, ItemTag.EXTREME);
        register(Material.CHARCOAL, State.EARLY);
        register(Material.CHERRY_BOAT, State.MID);
        register(Material.CHERRY_BUTTON, State.MID);
        register(Material.CHERRY_CHEST_BOAT, State.MID);
        register(Material.CHERRY_DOOR, State.MID);
        register(Material.CHERRY_FENCE, State.MID);
        register(Material.CHERRY_FENCE_GATE, State.MID);
        register(Material.CHERRY_HANGING_SIGN, State.MID);
        register(Material.CHERRY_LEAVES, State.MID);
        register(Material.CHERRY_LOG, State.MID);
        register(Material.CHERRY_PLANKS, State.MID);
        register(Material.CHERRY_PRESSURE_PLATE, State.MID);
        register(Material.CHERRY_SAPLING, State.MID);
        register(Material.CHERRY_SHELF, State.MID);
        register(Material.CHERRY_SIGN, State.MID);
        register(Material.CHERRY_SLAB, State.MID);
        register(Material.CHERRY_STAIRS, State.MID);
        register(Material.CHERRY_TRAPDOOR, State.MID);
        register(Material.CHERRY_WOOD, State.MID);
        register(Material.CHEST, State.EARLY);
        register(Material.CHEST_MINECART, State.EARLY);
        register(Material.CHICKEN, State.EARLY);
        register(Material.CHIPPED_ANVIL, State.LATE);
        register(Material.CHISELED_BOOKSHELF, State.EARLY);
        register(Material.CHISELED_CINNABAR, State.MID);
        register(Material.CHISELED_COPPER, State.EARLY);
        register(Material.CHISELED_DEEPSLATE, State.EARLY);
        register(Material.CHISELED_NETHER_BRICKS, State.MID);
        register(Material.CHISELED_POLISHED_BLACKSTONE, State.MID, ItemTag.NETHER);
        register(Material.CHISELED_QUARTZ_BLOCK, State.MID, ItemTag.NETHER);
        register(Material.CHISELED_RED_SANDSTONE, State.LATE);
        register(Material.CHISELED_RESIN_BRICKS, State.LATE);
        register(Material.CHISELED_SANDSTONE, State.EARLY);
        register(Material.CHISELED_STONE_BRICKS, State.EARLY);
        register(Material.CHISELED_SULFUR, State.MID);
        register(Material.CHISELED_TUFF, State.EARLY);
        register(Material.CHISELED_TUFF_BRICKS, State.EARLY);
        register(Material.CHORUS_FLOWER, State.LATE, ItemTag.END);
        register(Material.CHORUS_FRUIT, State.LATE, ItemTag.END);
        register(Material.CINNABAR, State.MID);
        register(Material.CINNABAR_BRICK_SLAB, State.MID);
        register(Material.CINNABAR_BRICK_STAIRS, State.MID);
        register(Material.CINNABAR_BRICK_WALL, State.MID);
        register(Material.CINNABAR_BRICKS, State.MID);
        register(Material.CINNABAR_SLAB, State.MID);
        register(Material.CINNABAR_STAIRS, State.MID);
        register(Material.CINNABAR_WALL, State.MID);
        register(Material.CLAY, State.EARLY);
        register(Material.CLAY_BALL, State.EARLY);
        register(Material.CLOCK, State.EARLY);
        register(Material.CLOSED_EYEBLOSSOM, State.LATE);
        register(Material.COAL, State.EARLY);
        register(Material.COAL_BLOCK, State.EARLY);
        register(Material.COAL_ORE, State.LATE);
        register(Material.COARSE_DIRT, State.EARLY);
        register(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, State.MID);
        register(Material.COBBLED_DEEPSLATE, State.EARLY);
        register(Material.COBBLED_DEEPSLATE_SLAB, State.EARLY);
        register(Material.COBBLED_DEEPSLATE_STAIRS, State.EARLY);
        register(Material.COBBLED_DEEPSLATE_WALL, State.EARLY);
        register(Material.COBBLESTONE, State.EARLY);
        register(Material.COBBLESTONE_SLAB, State.EARLY);
        register(Material.COBBLESTONE_STAIRS, State.EARLY);
        register(Material.COBBLESTONE_WALL, State.EARLY);
        register(Material.COBWEB, State.MID);
        register(Material.COCOA_BEANS, State.EARLY);
        register(Material.COD, State.EARLY);
        register(Material.COD_BUCKET, State.EARLY);
        register(Material.COMPARATOR, State.MID, ItemTag.NETHER);
        register(Material.COMPASS, State.EARLY);
        register(Material.COMPOSTER, State.EARLY);
        register(Material.CONDUIT, State.LATE, ItemTag.EXTREME);
        register(Material.COOKED_BEEF, State.EARLY);
        register(Material.COOKED_CHICKEN, State.EARLY);
        register(Material.COOKED_COD, State.EARLY);
        register(Material.COOKED_MUTTON, State.EARLY);
        register(Material.COOKED_PORKCHOP, State.EARLY);
        register(Material.COOKED_RABBIT, State.MID);
        register(Material.COOKED_SALMON, State.EARLY);
        register(Material.COOKIE, State.EARLY);
        register(Material.COPPER_AXE, State.EARLY);
        register(Material.COPPER_BARS, State.EARLY);
        register(Material.COPPER_BLOCK, State.EARLY);
        register(Material.COPPER_BOOTS, State.EARLY);
        register(Material.COPPER_BULB, State.LATE);
        register(Material.COPPER_CHAIN, State.EARLY);
        register(Material.COPPER_CHEST, State.EARLY);
        register(Material.COPPER_CHESTPLATE, State.EARLY);
        register(Material.COPPER_DOOR, State.EARLY);
        register(Material.COPPER_GOLEM_STATUE, State.LATE);
        register(Material.COPPER_GRATE, State.EARLY);
        register(Material.COPPER_HELMET, State.EARLY);
        register(Material.COPPER_HOE, State.EARLY);
        register(Material.COPPER_HORSE_ARMOR, State.LATE);
        register(Material.COPPER_INGOT, State.EARLY);
        register(Material.COPPER_LANTERN, State.EARLY);
        register(Material.COPPER_LEGGINGS, State.EARLY);
        register(Material.COPPER_NAUTILUS_ARMOR, State.LATE);
        register(Material.COPPER_NUGGET, State.EARLY);
        register(Material.COPPER_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.COPPER_PICKAXE, State.EARLY);
        register(Material.COPPER_SHOVEL, State.EARLY);
        register(Material.COPPER_SPEAR, State.EARLY);
        register(Material.COPPER_SWORD, State.EARLY);
        register(Material.COPPER_TORCH, State.EARLY);
        register(Material.COPPER_TRAPDOOR, State.EARLY);
        register(Material.CORNFLOWER, State.EARLY);
        register(Material.CRACKED_DEEPSLATE_BRICKS, State.EARLY);
        register(Material.CRACKED_DEEPSLATE_TILES, State.EARLY);
        register(Material.CRACKED_NETHER_BRICKS, State.MID);
        register(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, State.MID, ItemTag.NETHER);
        register(Material.CRACKED_STONE_BRICKS, State.EARLY);
        register(Material.CRAFTER, State.MID);
        register(Material.CRAFTING_TABLE, State.EARLY);
        register(Material.CREAKING_HEART, State.LATE);
        register(Material.CRIMSON_BUTTON, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_DOOR, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_FENCE, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_FENCE_GATE, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_FUNGUS, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_HANGING_SIGN, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_HYPHAE, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_NYLIUM, State.LATE, ItemTag.NETHER);
        register(Material.CRIMSON_PLANKS, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_PRESSURE_PLATE, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_ROOTS, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_SHELF, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_SIGN, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_SLAB, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_STEM, State.MID, ItemTag.NETHER);
        register(Material.CRIMSON_TRAPDOOR, State.MID, ItemTag.NETHER);
        register(Material.CROSSBOW, State.MID);
        register(Material.CRYING_OBSIDIAN, State.MID);
        register(Material.CUT_COPPER, State.EARLY);
        register(Material.CUT_COPPER_SLAB, State.EARLY);
        register(Material.CUT_COPPER_STAIRS, State.EARLY);
        register(Material.CUT_RED_SANDSTONE, State.LATE);
        register(Material.CUT_RED_SANDSTONE_SLAB, State.LATE);
        register(Material.CUT_SANDSTONE, State.EARLY);
        register(Material.CUT_SANDSTONE_SLAB, State.EARLY);
        register(Material.CYAN_BANNER, State.MID);
        register(Material.CYAN_BED, State.MID);
        register(Material.CYAN_BUNDLE, State.MID);
        register(Material.CYAN_CANDLE, State.LATE);
        register(Material.CYAN_CARPET, State.MID);
        register(Material.CYAN_CONCRETE, State.MID);
        register(Material.CYAN_CONCRETE_POWDER, State.MID);
        register(Material.CYAN_DYE, State.MID);
        register(Material.CYAN_GLAZED_TERRACOTTA, State.MID);
        register(Material.CYAN_HARNESS, State.MID);
        register(Material.CYAN_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.CYAN_STAINED_GLASS, State.MID);
        register(Material.CYAN_STAINED_GLASS_PANE, State.MID);
        register(Material.CYAN_TERRACOTTA, State.MID);
        register(Material.CYAN_WOOL, State.MID);
        register(Material.DAMAGED_ANVIL, State.LATE);
        register(Material.DANDELION, State.EARLY);
        register(Material.DANGER_POTTERY_SHERD, State.LATE);
        register(Material.DARK_OAK_BOAT, State.EARLY);
        register(Material.DARK_OAK_BUTTON, State.EARLY);
        register(Material.DARK_OAK_CHEST_BOAT, State.EARLY);
        register(Material.DARK_OAK_DOOR, State.EARLY);
        register(Material.DARK_OAK_FENCE, State.EARLY);
        register(Material.DARK_OAK_FENCE_GATE, State.EARLY);
        register(Material.DARK_OAK_HANGING_SIGN, State.EARLY);
        register(Material.DARK_OAK_LEAVES, State.EARLY);
        register(Material.DARK_OAK_LOG, State.EARLY);
        register(Material.DARK_OAK_PLANKS, State.EARLY);
        register(Material.DARK_OAK_PRESSURE_PLATE, State.EARLY);
        register(Material.DARK_OAK_SAPLING, State.EARLY);
        register(Material.DARK_OAK_SHELF, State.EARLY);
        register(Material.DARK_OAK_SIGN, State.EARLY);
        register(Material.DARK_OAK_SLAB, State.EARLY);
        register(Material.DARK_OAK_STAIRS, State.EARLY);
        register(Material.DARK_OAK_TRAPDOOR, State.EARLY);
        register(Material.DARK_OAK_WOOD, State.EARLY);
        register(Material.DARK_PRISMARINE, State.MID);
        register(Material.DARK_PRISMARINE_SLAB, State.MID);
        register(Material.DARK_PRISMARINE_STAIRS, State.MID);
        register(Material.DAYLIGHT_DETECTOR, State.MID, ItemTag.NETHER);
        register(Material.DEAD_BRAIN_CORAL, State.LATE);
        register(Material.DEAD_BRAIN_CORAL_BLOCK, State.LATE);
        register(Material.DEAD_BRAIN_CORAL_FAN, State.LATE);
        register(Material.DEAD_BUBBLE_CORAL, State.LATE);
        register(Material.DEAD_BUBBLE_CORAL_BLOCK, State.LATE);
        register(Material.DEAD_BUBBLE_CORAL_FAN, State.LATE);
        register(Material.DEAD_BUSH, State.MID);
        register(Material.DEAD_FIRE_CORAL, State.LATE);
        register(Material.DEAD_FIRE_CORAL_BLOCK, State.LATE);
        register(Material.DEAD_FIRE_CORAL_FAN, State.LATE);
        register(Material.DEAD_HORN_CORAL, State.LATE);
        register(Material.DEAD_HORN_CORAL_BLOCK, State.LATE);
        register(Material.DEAD_HORN_CORAL_FAN, State.LATE);
        register(Material.DEAD_TUBE_CORAL, State.LATE);
        register(Material.DEAD_TUBE_CORAL_BLOCK, State.LATE);
        register(Material.DEAD_TUBE_CORAL_FAN, State.LATE);
        register(Material.DECORATED_POT, State.EARLY);
        register(Material.DEEPSLATE, State.EARLY);
        register(Material.DEEPSLATE_BRICK_SLAB, State.EARLY);
        register(Material.DEEPSLATE_BRICK_STAIRS, State.EARLY);
        register(Material.DEEPSLATE_BRICK_WALL, State.EARLY);
        register(Material.DEEPSLATE_BRICKS, State.EARLY);
        register(Material.DEEPSLATE_COAL_ORE, State.LATE);
        register(Material.DEEPSLATE_COPPER_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_DIAMOND_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_EMERALD_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_GOLD_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_IRON_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_LAPIS_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_REDSTONE_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DEEPSLATE_TILE_SLAB, State.EARLY);
        register(Material.DEEPSLATE_TILE_STAIRS, State.EARLY);
        register(Material.DEEPSLATE_TILE_WALL, State.EARLY);
        register(Material.DEEPSLATE_TILES, State.EARLY);
        register(Material.DETECTOR_RAIL, State.MID);
        register(Material.DIAMOND, State.EARLY);
        register(Material.DIAMOND_AXE, State.EARLY);
        register(Material.DIAMOND_BLOCK, State.MID);
        register(Material.DIAMOND_BOOTS, State.EARLY);
        register(Material.DIAMOND_CHESTPLATE, State.MID);
        register(Material.DIAMOND_HELMET, State.MID);
        register(Material.DIAMOND_HOE, State.EARLY);
        register(Material.DIAMOND_HORSE_ARMOR, State.LATE);
        register(Material.DIAMOND_LEGGINGS, State.MID);
        register(Material.DIAMOND_NAUTILUS_ARMOR, State.LATE);
        register(Material.DIAMOND_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.DIAMOND_PICKAXE, State.EARLY);
        register(Material.DIAMOND_SHOVEL, State.EARLY);
        register(Material.DIAMOND_SPEAR, State.EARLY);
        register(Material.DIAMOND_SWORD, State.EARLY);
        register(Material.DIORITE, State.EARLY);
        register(Material.DIORITE_SLAB, State.EARLY);
        register(Material.DIORITE_STAIRS, State.EARLY);
        register(Material.DIORITE_WALL, State.EARLY);
        register(Material.DIRT, State.EARLY);
        register(Material.DISC_FRAGMENT_5, State.LATE);
        register(Material.DISPENSER, State.MID);
        register(Material.DRAGON_HEAD, State.LATE, ItemTag.END);
        register(Material.DRIED_GHAST, State.MID, ItemTag.NETHER);
        register(Material.DRIED_KELP, State.EARLY);
        register(Material.DRIED_KELP_BLOCK, State.EARLY);
        register(Material.DRIPSTONE_BLOCK, State.MID);
        register(Material.DROPPER, State.EARLY);
        register(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.ECHO_SHARD, State.LATE);
        register(Material.EGG, State.LATE);
        register(Material.ELYTRA, State.LATE, ItemTag.END);
        register(Material.EMERALD, State.EARLY);
        register(Material.EMERALD_BLOCK, State.MID);
        register(Material.EMERALD_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.ENCHANTED_BOOK, State.LATE);
        register(Material.ENCHANTED_GOLDEN_APPLE, State.LATE);
        register(Material.ENCHANTING_TABLE, State.MID);
        register(Material.END_CRYSTAL, State.LATE, ItemTag.EXTREME);
        register(Material.END_ROD, State.LATE, ItemTag.END);
        register(Material.END_STONE, State.LATE, ItemTag.END);
        register(Material.END_STONE_BRICK_SLAB, State.LATE, ItemTag.END);
        register(Material.END_STONE_BRICK_STAIRS, State.LATE, ItemTag.END);
        register(Material.END_STONE_BRICK_WALL, State.LATE, ItemTag.END);
        register(Material.END_STONE_BRICKS, State.LATE, ItemTag.END);
        register(Material.ENDER_CHEST, State.LATE);
        register(Material.ENDER_EYE, State.LATE, ItemTag.NETHER);
        register(Material.ENDER_PEARL, State.LATE);
        register(Material.EXPERIENCE_BOTTLE, State.LATE);
        register(Material.EXPLORER_POTTERY_SHERD, State.LATE);
        register(Material.EXPOSED_CHISELED_COPPER, State.LATE);
        register(Material.EXPOSED_COPPER, State.LATE);
        register(Material.EXPOSED_COPPER_BARS, State.LATE);
        register(Material.EXPOSED_COPPER_BULB, State.LATE);
        register(Material.EXPOSED_COPPER_CHAIN, State.LATE);
        register(Material.EXPOSED_COPPER_CHEST, State.LATE);
        register(Material.EXPOSED_COPPER_DOOR, State.LATE);
        register(Material.EXPOSED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.EXPOSED_COPPER_GRATE, State.LATE);
        register(Material.EXPOSED_COPPER_LANTERN, State.LATE);
        register(Material.EXPOSED_COPPER_TRAPDOOR, State.LATE);
        register(Material.EXPOSED_CUT_COPPER, State.LATE);
        register(Material.EXPOSED_CUT_COPPER_SLAB, State.LATE);
        register(Material.EXPOSED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.EXPOSED_LIGHTNING_ROD, State.LATE);
        register(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.FEATHER, State.EARLY);
        register(Material.FERMENTED_SPIDER_EYE, State.MID);
        register(Material.FERN, State.EARLY);
        register(Material.FIELD_MASONED_BANNER_PATTERN, State.EARLY);
        register(Material.FILLED_MAP, State.EARLY);
        register(Material.FIRE_CHARGE, State.MID);
        register(Material.FIRE_CORAL, State.LATE);
        register(Material.FIRE_CORAL_BLOCK, State.LATE);
        register(Material.FIRE_CORAL_FAN, State.LATE);
        register(Material.FIREFLY_BUSH, State.EARLY);
        register(Material.FIREWORK_ROCKET, State.MID);
        register(Material.FIREWORK_STAR, State.MID);
        register(Material.FISHING_ROD, State.MID);
        register(Material.FLETCHING_TABLE, State.EARLY);
        register(Material.FLINT, State.EARLY);
        register(Material.FLINT_AND_STEEL, State.EARLY);
        register(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.FLOW_BANNER_PATTERN, State.LATE, ItemTag.EXTREME);
        register(Material.FLOW_POTTERY_SHERD, State.LATE);
        register(Material.FLOWER_BANNER_PATTERN, State.EARLY);
        register(Material.FLOWER_POT, State.EARLY);
        register(Material.FLOWERING_AZALEA, State.MID);
        register(Material.FLOWERING_AZALEA_LEAVES, State.MID);
        register(Material.FRIEND_POTTERY_SHERD, State.LATE);
        register(Material.FURNACE, State.EARLY);
        register(Material.FURNACE_MINECART, State.EARLY);
        register(Material.GHAST_TEAR, State.MID, ItemTag.NETHER);
        register(Material.GILDED_BLACKSTONE, State.LATE, ItemTag.NETHER);
        register(Material.GLASS, State.EARLY);
        register(Material.GLASS_BOTTLE, State.EARLY);
        register(Material.GLASS_PANE, State.EARLY);
        register(Material.GLISTERING_MELON_SLICE, State.EARLY);
        register(Material.GLOBE_BANNER_PATTERN, State.LATE, ItemTag.EXTREME);
        register(Material.GLOW_BERRIES, State.MID);
        register(Material.GLOW_INK_SAC, State.EARLY);
        register(Material.GLOW_ITEM_FRAME, State.EARLY);
        register(Material.GLOW_LICHEN, State.EARLY);
        register(Material.GLOWSTONE, State.MID, ItemTag.NETHER);
        register(Material.GLOWSTONE_DUST, State.MID, ItemTag.NETHER);
        register(Material.GOAT_HORN, State.LATE);
        register(Material.GOLD_BLOCK, State.EARLY);
        register(Material.GOLD_INGOT, State.EARLY);
        register(Material.GOLD_NUGGET, State.EARLY);
        register(Material.GOLD_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.GOLDEN_APPLE, State.EARLY);
        register(Material.GOLDEN_AXE, State.EARLY);
        register(Material.GOLDEN_BOOTS, State.EARLY);
        register(Material.GOLDEN_CARROT, State.MID);
        register(Material.GOLDEN_CHESTPLATE, State.EARLY);
        register(Material.GOLDEN_DANDELION, State.EARLY);
        register(Material.GOLDEN_HELMET, State.EARLY);
        register(Material.GOLDEN_HOE, State.EARLY);
        register(Material.GOLDEN_HORSE_ARMOR, State.LATE);
        register(Material.GOLDEN_LEGGINGS, State.EARLY);
        register(Material.GOLDEN_NAUTILUS_ARMOR, State.LATE);
        register(Material.GOLDEN_PICKAXE, State.EARLY);
        register(Material.GOLDEN_SHOVEL, State.EARLY);
        register(Material.GOLDEN_SPEAR, State.EARLY);
        register(Material.GOLDEN_SWORD, State.EARLY);
        register(Material.GRANITE, State.EARLY);
        register(Material.GRANITE_SLAB, State.EARLY);
        register(Material.GRANITE_STAIRS, State.EARLY);
        register(Material.GRANITE_WALL, State.EARLY);
        register(Material.GRASS_BLOCK, State.LATE);
        register(Material.GRAVEL, State.EARLY);
        register(Material.GRAY_BANNER, State.EARLY);
        register(Material.GRAY_BED, State.EARLY);
        register(Material.GRAY_BUNDLE, State.EARLY);
        register(Material.GRAY_CANDLE, State.LATE);
        register(Material.GRAY_CARPET, State.EARLY);
        register(Material.GRAY_CONCRETE, State.EARLY);
        register(Material.GRAY_CONCRETE_POWDER, State.EARLY);
        register(Material.GRAY_DYE, State.EARLY);
        register(Material.GRAY_GLAZED_TERRACOTTA, State.MID);
        register(Material.GRAY_HARNESS, State.EARLY);
        register(Material.GRAY_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.GRAY_STAINED_GLASS, State.EARLY);
        register(Material.GRAY_STAINED_GLASS_PANE, State.EARLY);
        register(Material.GRAY_TERRACOTTA, State.EARLY);
        register(Material.GRAY_WOOL, State.EARLY);
        register(Material.GREEN_BANNER, State.MID);
        register(Material.GREEN_BED, State.MID);
        register(Material.GREEN_BUNDLE, State.MID);
        register(Material.GREEN_CANDLE, State.LATE);
        register(Material.GREEN_CARPET, State.MID);
        register(Material.GREEN_CONCRETE, State.MID);
        register(Material.GREEN_CONCRETE_POWDER, State.MID);
        register(Material.GREEN_DYE, State.MID);
        register(Material.GREEN_GLAZED_TERRACOTTA, State.MID);
        register(Material.GREEN_HARNESS, State.MID);
        register(Material.GREEN_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.GREEN_STAINED_GLASS, State.MID);
        register(Material.GREEN_STAINED_GLASS_PANE, State.MID);
        register(Material.GREEN_TERRACOTTA, State.MID);
        register(Material.GREEN_WOOL, State.MID);
        register(Material.GRINDSTONE, State.EARLY);
        register(Material.GUNPOWDER, State.EARLY);
        register(Material.GUSTER_BANNER_PATTERN, State.LATE, ItemTag.EXTREME);
        register(Material.GUSTER_POTTERY_SHERD, State.LATE);
        register(Material.HANGING_ROOTS, State.MID);
        register(Material.HAY_BLOCK, State.EARLY);
        register(Material.HEART_OF_THE_SEA, State.MID);
        register(Material.HEART_POTTERY_SHERD, State.LATE);
        register(Material.HEARTBREAK_POTTERY_SHERD, State.LATE);
        register(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, State.EARLY);
        register(Material.HONEY_BLOCK, State.LATE);
        register(Material.HONEY_BOTTLE, State.LATE);
        register(Material.HONEYCOMB, State.LATE);
        register(Material.HONEYCOMB_BLOCK, State.LATE);
        register(Material.HOPPER, State.EARLY);
        register(Material.HOPPER_MINECART, State.EARLY);
        register(Material.HORN_CORAL, State.LATE);
        register(Material.HORN_CORAL_BLOCK, State.LATE);
        register(Material.HORN_CORAL_FAN, State.LATE);
        register(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.HOWL_POTTERY_SHERD, State.LATE);
        register(Material.ICE, State.LATE);
        register(Material.INK_SAC, State.EARLY);
        register(Material.IRON_AXE, State.EARLY);
        register(Material.IRON_BARS, State.EARLY);
        register(Material.IRON_BLOCK, State.EARLY);
        register(Material.IRON_BOOTS, State.EARLY);
        register(Material.IRON_CHAIN, State.EARLY);
        register(Material.IRON_CHESTPLATE, State.EARLY);
        register(Material.IRON_DOOR, State.EARLY);
        register(Material.IRON_HELMET, State.EARLY);
        register(Material.IRON_HOE, State.EARLY);
        register(Material.IRON_HORSE_ARMOR, State.LATE);
        register(Material.IRON_INGOT, State.EARLY);
        register(Material.IRON_LEGGINGS, State.EARLY);
        register(Material.IRON_NAUTILUS_ARMOR, State.LATE);
        register(Material.IRON_NUGGET, State.EARLY);
        register(Material.IRON_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.IRON_PICKAXE, State.EARLY);
        register(Material.IRON_SHOVEL, State.EARLY);
        register(Material.IRON_SPEAR, State.EARLY);
        register(Material.IRON_SWORD, State.EARLY);
        register(Material.IRON_TRAPDOOR, State.EARLY);
        register(Material.ITEM_FRAME, State.EARLY);
        register(Material.JACK_O_LANTERN, State.EARLY);
        register(Material.JUKEBOX, State.EARLY);
        register(Material.JUNGLE_BOAT, State.EARLY);
        register(Material.JUNGLE_BUTTON, State.EARLY);
        register(Material.JUNGLE_CHEST_BOAT, State.EARLY);
        register(Material.JUNGLE_DOOR, State.EARLY);
        register(Material.JUNGLE_FENCE, State.EARLY);
        register(Material.JUNGLE_FENCE_GATE, State.EARLY);
        register(Material.JUNGLE_HANGING_SIGN, State.EARLY);
        register(Material.JUNGLE_LEAVES, State.EARLY);
        register(Material.JUNGLE_LOG, State.EARLY);
        register(Material.JUNGLE_PLANKS, State.EARLY);
        register(Material.JUNGLE_PRESSURE_PLATE, State.EARLY);
        register(Material.JUNGLE_SAPLING, State.EARLY);
        register(Material.JUNGLE_SHELF, State.EARLY);
        register(Material.JUNGLE_SIGN, State.EARLY);
        register(Material.JUNGLE_SLAB, State.EARLY);
        register(Material.JUNGLE_STAIRS, State.EARLY);
        register(Material.JUNGLE_TRAPDOOR, State.EARLY);
        register(Material.JUNGLE_WOOD, State.EARLY);
        register(Material.KELP, State.EARLY);
        register(Material.KNOWLEDGE_BOOK, State.MID, ItemTag.NETHER);
        register(Material.LADDER, State.EARLY);
        register(Material.LANTERN, State.EARLY);
        register(Material.LAPIS_BLOCK, State.EARLY);
        register(Material.LAPIS_LAZULI, State.EARLY);
        register(Material.LAPIS_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.LARGE_AMETHYST_BUD, State.LATE);
        register(Material.LARGE_FERN, State.LATE);
        register(Material.LAVA_BUCKET, State.EARLY);
        register(Material.LEAD, State.MID);
        register(Material.LEAF_LITTER, State.EARLY);
        register(Material.LEATHER, State.EARLY);
        register(Material.LEATHER_BOOTS, State.EARLY);
        register(Material.LEATHER_CHESTPLATE, State.EARLY);
        register(Material.LEATHER_HELMET, State.EARLY);
        register(Material.LEATHER_HORSE_ARMOR, State.EARLY);
        register(Material.LEATHER_LEGGINGS, State.EARLY);
        register(Material.LECTERN, State.EARLY);
        register(Material.LEVER, State.EARLY);
        register(Material.LIGHT_BLUE_BANNER, State.EARLY);
        register(Material.LIGHT_BLUE_BED, State.EARLY);
        register(Material.LIGHT_BLUE_BUNDLE, State.EARLY);
        register(Material.LIGHT_BLUE_CANDLE, State.LATE);
        register(Material.LIGHT_BLUE_CARPET, State.EARLY);
        register(Material.LIGHT_BLUE_CONCRETE, State.EARLY);
        register(Material.LIGHT_BLUE_CONCRETE_POWDER, State.EARLY);
        register(Material.LIGHT_BLUE_DYE, State.EARLY);
        register(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, State.MID);
        register(Material.LIGHT_BLUE_HARNESS, State.EARLY);
        register(Material.LIGHT_BLUE_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.LIGHT_BLUE_STAINED_GLASS, State.EARLY);
        register(Material.LIGHT_BLUE_STAINED_GLASS_PANE, State.EARLY);
        register(Material.LIGHT_BLUE_TERRACOTTA, State.EARLY);
        register(Material.LIGHT_BLUE_WOOL, State.EARLY);
        register(Material.LIGHT_GRAY_BANNER, State.EARLY);
        register(Material.LIGHT_GRAY_BED, State.EARLY);
        register(Material.LIGHT_GRAY_BUNDLE, State.EARLY);
        register(Material.LIGHT_GRAY_CANDLE, State.LATE);
        register(Material.LIGHT_GRAY_CARPET, State.EARLY);
        register(Material.LIGHT_GRAY_CONCRETE, State.EARLY);
        register(Material.LIGHT_GRAY_CONCRETE_POWDER, State.EARLY);
        register(Material.LIGHT_GRAY_DYE, State.EARLY);
        register(Material.LIGHT_GRAY_GLAZED_TERRACOTTA, State.MID);
        register(Material.LIGHT_GRAY_HARNESS, State.EARLY);
        register(Material.LIGHT_GRAY_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.LIGHT_GRAY_STAINED_GLASS, State.EARLY);
        register(Material.LIGHT_GRAY_STAINED_GLASS_PANE, State.EARLY);
        register(Material.LIGHT_GRAY_TERRACOTTA, State.EARLY);
        register(Material.LIGHT_GRAY_WOOL, State.EARLY);
        register(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, State.EARLY);
        register(Material.LIGHTNING_ROD, State.EARLY);
        register(Material.LILAC, State.EARLY);
        register(Material.LILY_OF_THE_VALLEY, State.EARLY);
        register(Material.LILY_PAD, State.MID);
        register(Material.LIME_BANNER, State.MID);
        register(Material.LIME_BED, State.MID);
        register(Material.LIME_BUNDLE, State.MID);
        register(Material.LIME_CANDLE, State.LATE);
        register(Material.LIME_CARPET, State.MID);
        register(Material.LIME_CONCRETE, State.MID);
        register(Material.LIME_CONCRETE_POWDER, State.MID);
        register(Material.LIME_DYE, State.MID);
        register(Material.LIME_GLAZED_TERRACOTTA, State.MID);
        register(Material.LIME_HARNESS, State.MID);
        register(Material.LIME_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.LIME_STAINED_GLASS, State.MID);
        register(Material.LIME_STAINED_GLASS_PANE, State.MID);
        register(Material.LIME_TERRACOTTA, State.MID);
        register(Material.LIME_WOOL, State.MID);
        register(Material.LODESTONE, State.EARLY);
        register(Material.LOOM, State.MID);
        register(Material.MACE, State.LATE, ItemTag.EXTREME);
        register(Material.MAGENTA_BANNER, State.EARLY);
        register(Material.MAGENTA_BED, State.EARLY);
        register(Material.MAGENTA_BUNDLE, State.EARLY);
        register(Material.MAGENTA_CANDLE, State.LATE);
        register(Material.MAGENTA_CARPET, State.EARLY);
        register(Material.MAGENTA_CONCRETE, State.EARLY);
        register(Material.MAGENTA_CONCRETE_POWDER, State.EARLY);
        register(Material.MAGENTA_DYE, State.EARLY);
        register(Material.MAGENTA_GLAZED_TERRACOTTA, State.MID);
        register(Material.MAGENTA_HARNESS, State.EARLY);
        register(Material.MAGENTA_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.MAGENTA_STAINED_GLASS, State.EARLY);
        register(Material.MAGENTA_STAINED_GLASS_PANE, State.EARLY);
        register(Material.MAGENTA_TERRACOTTA, State.EARLY);
        register(Material.MAGENTA_WOOL, State.EARLY);
        register(Material.MAGMA_BLOCK, State.EARLY);
        register(Material.MAGMA_CREAM, State.LATE, ItemTag.NETHER);
        register(Material.MANGROVE_BOAT, State.MID);
        register(Material.MANGROVE_BUTTON, State.MID);
        register(Material.MANGROVE_CHEST_BOAT, State.MID);
        register(Material.MANGROVE_DOOR, State.MID);
        register(Material.MANGROVE_FENCE, State.MID);
        register(Material.MANGROVE_FENCE_GATE, State.MID);
        register(Material.MANGROVE_HANGING_SIGN, State.MID);
        register(Material.MANGROVE_LEAVES, State.MID);
        register(Material.MANGROVE_LOG, State.MID);
        register(Material.MANGROVE_PLANKS, State.MID);
        register(Material.MANGROVE_PRESSURE_PLATE, State.MID);
        register(Material.MANGROVE_PROPAGULE, State.MID);
        register(Material.MANGROVE_ROOTS, State.MID);
        register(Material.MANGROVE_SHELF, State.MID);
        register(Material.MANGROVE_SIGN, State.MID);
        register(Material.MANGROVE_SLAB, State.MID);
        register(Material.MANGROVE_STAIRS, State.MID);
        register(Material.MANGROVE_TRAPDOOR, State.MID);
        register(Material.MANGROVE_WOOD, State.MID);
        register(Material.MAP, State.EARLY);
        register(Material.MEDIUM_AMETHYST_BUD, State.LATE);
        register(Material.MELON, State.EARLY);
        register(Material.MELON_SEEDS, State.EARLY);
        register(Material.MELON_SLICE, State.EARLY);
        register(Material.MILK_BUCKET, State.EARLY);
        register(Material.MINECART, State.EARLY);
        register(Material.MINER_POTTERY_SHERD, State.LATE);
        register(Material.MOJANG_BANNER_PATTERN, State.LATE);
        register(Material.MOSS_BLOCK, State.MID);
        register(Material.MOSS_CARPET, State.MID);
        register(Material.MOSSY_COBBLESTONE, State.EARLY);
        register(Material.MOSSY_COBBLESTONE_SLAB, State.EARLY);
        register(Material.MOSSY_COBBLESTONE_STAIRS, State.EARLY);
        register(Material.MOSSY_COBBLESTONE_WALL, State.EARLY);
        register(Material.MOSSY_STONE_BRICK_SLAB, State.EARLY);
        register(Material.MOSSY_STONE_BRICK_STAIRS, State.EARLY);
        register(Material.MOSSY_STONE_BRICK_WALL, State.EARLY);
        register(Material.MOSSY_STONE_BRICKS, State.EARLY);
        register(Material.MOURNER_POTTERY_SHERD, State.LATE);
        register(Material.MUD, State.EARLY);
        register(Material.MUD_BRICK_SLAB, State.EARLY);
        register(Material.MUD_BRICK_STAIRS, State.EARLY);
        register(Material.MUD_BRICK_WALL, State.EARLY);
        register(Material.MUD_BRICKS, State.EARLY);
        register(Material.MUDDY_MANGROVE_ROOTS, State.MID);
        register(Material.MUSHROOM_STEM, State.LATE);
        register(Material.MUSHROOM_STEW, State.MID);
        register(Material.MUSIC_DISC_13, State.LATE);
        register(Material.MUSIC_DISC_BOUNCE, State.LATE);
        register(Material.MUSIC_DISC_CAT, State.LATE);
        register(Material.MUSIC_DISC_CHIRP, State.MID);
        register(Material.MUSIC_DISC_CREATOR, State.LATE, ItemTag.EXTREME);
        register(Material.MUSIC_DISC_CREATOR_MUSIC_BOX, State.LATE);
        register(Material.MUSIC_DISC_LAVA_CHICKEN, State.LATE);
        register(Material.MUSIC_DISC_OTHERSIDE, State.LATE);
        register(Material.MUSIC_DISC_PIGSTEP, State.LATE, ItemTag.NETHER);
        register(Material.MUSIC_DISC_PRECIPICE, State.LATE, ItemTag.EXTREME);
        register(Material.MUSIC_DISC_RELIC, State.LATE);
        register(Material.MUSIC_DISC_TEARS, State.MID, ItemTag.NETHER);
        register(Material.MUTTON, State.EARLY);
        register(Material.MYCELIUM, State.LATE, ItemTag.EXTREME);
        register(Material.NAME_TAG, State.EARLY);
        register(Material.NAUTILUS_SHELL, State.MID);
        register(Material.NETHER_BRICK, State.MID);
        register(Material.NETHER_BRICK_FENCE, State.MID);
        register(Material.NETHER_BRICK_SLAB, State.MID);
        register(Material.NETHER_BRICK_STAIRS, State.MID);
        register(Material.NETHER_BRICK_WALL, State.MID);
        register(Material.NETHER_BRICKS, State.MID);
        register(Material.NETHER_GOLD_ORE, State.LATE, ItemTag.NETHER);
        register(Material.NETHER_QUARTZ_ORE, State.LATE, ItemTag.NETHER);
        register(Material.NETHER_SPROUTS, State.MID, ItemTag.NETHER);
        register(Material.NETHER_STAR, State.EARLY);
        register(Material.NETHER_WART, State.MID, ItemTag.NETHER);
        register(Material.NETHER_WART_BLOCK, State.MID, ItemTag.NETHER);
        register(Material.NETHERITE_AXE, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_BOOTS, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_CHESTPLATE, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_HELMET, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_HOE, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_HORSE_ARMOR, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_INGOT, State.LATE, ItemTag.NETHER);
        register(Material.NETHERITE_LEGGINGS, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_NAUTILUS_ARMOR, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_PICKAXE, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_SCRAP, State.LATE, ItemTag.NETHER);
        register(Material.NETHERITE_SHOVEL, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_SPEAR, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_SWORD, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, State.LATE, ItemTag.NETHER);
        register(Material.NETHERRACK, State.EARLY);
        register(Material.NOTE_BLOCK, State.EARLY);
        register(Material.OAK_BOAT, State.EARLY);
        register(Material.OAK_BUTTON, State.EARLY);
        register(Material.OAK_CHEST_BOAT, State.EARLY);
        register(Material.OAK_DOOR, State.EARLY);
        register(Material.OAK_FENCE, State.EARLY);
        register(Material.OAK_FENCE_GATE, State.EARLY);
        register(Material.OAK_HANGING_SIGN, State.EARLY);
        register(Material.OAK_LEAVES, State.EARLY);
        register(Material.OAK_LOG, State.EARLY);
        register(Material.OAK_PLANKS, State.EARLY);
        register(Material.OAK_PRESSURE_PLATE, State.EARLY);
        register(Material.OAK_SAPLING, State.EARLY);
        register(Material.OAK_SHELF, State.EARLY);
        register(Material.OAK_SIGN, State.EARLY);
        register(Material.OAK_SLAB, State.EARLY);
        register(Material.OAK_STAIRS, State.EARLY);
        register(Material.OAK_TRAPDOOR, State.EARLY);
        register(Material.OAK_WOOD, State.EARLY);
        register(Material.OBSERVER, State.MID);
        register(Material.OBSIDIAN, State.MID);
        register(Material.OMINOUS_BOTTLE, State.LATE, ItemTag.EXTREME);
        register(Material.OMINOUS_TRIAL_KEY, State.LATE, ItemTag.EXTREME);
        register(Material.OPEN_EYEBLOSSOM, State.LATE);
        register(Material.ORANGE_BANNER, State.EARLY);
        register(Material.ORANGE_BED, State.EARLY);
        register(Material.ORANGE_BUNDLE, State.EARLY);
        register(Material.ORANGE_CANDLE, State.LATE);
        register(Material.ORANGE_CARPET, State.EARLY);
        register(Material.ORANGE_CONCRETE, State.EARLY);
        register(Material.ORANGE_CONCRETE_POWDER, State.EARLY);
        register(Material.ORANGE_DYE, State.EARLY);
        register(Material.ORANGE_GLAZED_TERRACOTTA, State.MID);
        register(Material.ORANGE_HARNESS, State.EARLY);
        register(Material.ORANGE_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.ORANGE_STAINED_GLASS, State.EARLY);
        register(Material.ORANGE_STAINED_GLASS_PANE, State.EARLY);
        register(Material.ORANGE_TERRACOTTA, State.EARLY);
        register(Material.ORANGE_TULIP, State.MID);
        register(Material.ORANGE_WOOL, State.EARLY);
        register(Material.OXEYE_DAISY, State.EARLY);
        register(Material.OXIDIZED_CHISELED_COPPER, State.LATE);
        register(Material.OXIDIZED_COPPER, State.LATE);
        register(Material.OXIDIZED_COPPER_BARS, State.LATE);
        register(Material.OXIDIZED_COPPER_BULB, State.LATE);
        register(Material.OXIDIZED_COPPER_CHAIN, State.LATE);
        register(Material.OXIDIZED_COPPER_CHEST, State.LATE);
        register(Material.OXIDIZED_COPPER_DOOR, State.LATE);
        register(Material.OXIDIZED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.OXIDIZED_COPPER_GRATE, State.LATE);
        register(Material.OXIDIZED_COPPER_LANTERN, State.LATE);
        register(Material.OXIDIZED_COPPER_TRAPDOOR, State.LATE);
        register(Material.OXIDIZED_CUT_COPPER, State.LATE);
        register(Material.OXIDIZED_CUT_COPPER_SLAB, State.LATE);
        register(Material.OXIDIZED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.OXIDIZED_LIGHTNING_ROD, State.LATE);
        register(Material.PACKED_ICE, State.LATE);
        register(Material.PACKED_MUD, State.EARLY);
        register(Material.PAINTING, State.EARLY);
        register(Material.PALE_HANGING_MOSS, State.LATE);
        register(Material.PALE_MOSS_BLOCK, State.LATE);
        register(Material.PALE_MOSS_CARPET, State.LATE);
        register(Material.PALE_OAK_BOAT, State.LATE);
        register(Material.PALE_OAK_BUTTON, State.LATE);
        register(Material.PALE_OAK_CHEST_BOAT, State.LATE);
        register(Material.PALE_OAK_DOOR, State.LATE);
        register(Material.PALE_OAK_FENCE, State.LATE);
        register(Material.PALE_OAK_FENCE_GATE, State.LATE);
        register(Material.PALE_OAK_HANGING_SIGN, State.LATE);
        register(Material.PALE_OAK_LEAVES, State.LATE);
        register(Material.PALE_OAK_LOG, State.LATE);
        register(Material.PALE_OAK_PLANKS, State.LATE);
        register(Material.PALE_OAK_PRESSURE_PLATE, State.LATE);
        register(Material.PALE_OAK_SAPLING, State.LATE);
        register(Material.PALE_OAK_SHELF, State.LATE);
        register(Material.PALE_OAK_SIGN, State.LATE);
        register(Material.PALE_OAK_SLAB, State.LATE);
        register(Material.PALE_OAK_STAIRS, State.LATE);
        register(Material.PALE_OAK_TRAPDOOR, State.LATE);
        register(Material.PALE_OAK_WOOD, State.LATE);
        register(Material.PAPER, State.EARLY);
        register(Material.PEONY, State.EARLY);
        register(Material.PIGLIN_BANNER_PATTERN, State.LATE, ItemTag.NETHER);
        register(Material.PINK_BANNER, State.EARLY);
        register(Material.PINK_BED, State.EARLY);
        register(Material.PINK_BUNDLE, State.EARLY);
        register(Material.PINK_CANDLE, State.LATE);
        register(Material.PINK_CARPET, State.EARLY);
        register(Material.PINK_CONCRETE, State.EARLY);
        register(Material.PINK_CONCRETE_POWDER, State.EARLY);
        register(Material.PINK_DYE, State.EARLY);
        register(Material.PINK_GLAZED_TERRACOTTA, State.MID);
        register(Material.PINK_HARNESS, State.EARLY);
        register(Material.PINK_PETALS, State.MID);
        register(Material.PINK_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.PINK_STAINED_GLASS, State.EARLY);
        register(Material.PINK_STAINED_GLASS_PANE, State.EARLY);
        register(Material.PINK_TERRACOTTA, State.EARLY);
        register(Material.PINK_TULIP, State.MID);
        register(Material.PINK_WOOL, State.EARLY);
        register(Material.PISTON, State.EARLY);
        register(Material.PLENTY_POTTERY_SHERD, State.LATE);
        register(Material.PODZOL, State.LATE);
        register(Material.POINTED_DRIPSTONE, State.MID);
        register(Material.POISONOUS_POTATO, State.MID);
        register(Material.POLISHED_ANDESITE, State.EARLY);
        register(Material.POLISHED_ANDESITE_SLAB, State.EARLY);
        register(Material.POLISHED_ANDESITE_STAIRS, State.EARLY);
        register(Material.POLISHED_BASALT, State.MID);
        register(Material.POLISHED_BLACKSTONE, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_BRICK_SLAB, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_BRICK_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_BRICK_WALL, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_BRICKS, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_BUTTON, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, State.MID);
        register(Material.POLISHED_BLACKSTONE_SLAB, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_BLACKSTONE_WALL, State.MID, ItemTag.NETHER);
        register(Material.POLISHED_CINNABAR, State.MID);
        register(Material.POLISHED_CINNABAR_SLAB, State.MID);
        register(Material.POLISHED_CINNABAR_STAIRS, State.MID);
        register(Material.POLISHED_CINNABAR_WALL, State.MID);
        register(Material.POLISHED_DEEPSLATE, State.EARLY);
        register(Material.POLISHED_DEEPSLATE_SLAB, State.EARLY);
        register(Material.POLISHED_DEEPSLATE_STAIRS, State.EARLY);
        register(Material.POLISHED_DEEPSLATE_WALL, State.EARLY);
        register(Material.POLISHED_DIORITE, State.EARLY);
        register(Material.POLISHED_DIORITE_SLAB, State.EARLY);
        register(Material.POLISHED_DIORITE_STAIRS, State.EARLY);
        register(Material.POLISHED_GRANITE, State.EARLY);
        register(Material.POLISHED_GRANITE_SLAB, State.EARLY);
        register(Material.POLISHED_GRANITE_STAIRS, State.EARLY);
        register(Material.POLISHED_SULFUR, State.MID);
        register(Material.POLISHED_SULFUR_SLAB, State.MID);
        register(Material.POLISHED_SULFUR_STAIRS, State.MID);
        register(Material.POLISHED_SULFUR_WALL, State.MID);
        register(Material.POLISHED_TUFF, State.EARLY);
        register(Material.POLISHED_TUFF_SLAB, State.EARLY);
        register(Material.POLISHED_TUFF_STAIRS, State.EARLY);
        register(Material.POLISHED_TUFF_WALL, State.EARLY);
        register(Material.POPPED_CHORUS_FRUIT, State.LATE, ItemTag.END);
        register(Material.POPPY, State.EARLY);
        register(Material.PORKCHOP, State.EARLY);
        register(Material.POTATO, State.EARLY);
        register(Material.POTENT_SULFUR, State.MID);
        register(Material.POTION, State.MID);
        register(Material.POWDER_SNOW_BUCKET, State.MID);
        register(Material.POWERED_RAIL, State.MID);
        register(Material.PRISMARINE, State.MID);
        register(Material.PRISMARINE_BRICK_SLAB, State.MID);
        register(Material.PRISMARINE_BRICK_STAIRS, State.MID);
        register(Material.PRISMARINE_BRICKS, State.MID);
        register(Material.PRISMARINE_CRYSTALS, State.MID);
        register(Material.PRISMARINE_SHARD, State.MID);
        register(Material.PRISMARINE_SLAB, State.MID);
        register(Material.PRISMARINE_STAIRS, State.MID);
        register(Material.PRISMARINE_WALL, State.MID);
        register(Material.PRIZE_POTTERY_SHERD, State.LATE);
        register(Material.PUFFERFISH, State.MID);
        register(Material.PUFFERFISH_BUCKET, State.MID);
        register(Material.PUMPKIN, State.EARLY);
        register(Material.PUMPKIN_PIE, State.LATE);
        register(Material.PUMPKIN_SEEDS, State.EARLY);
        register(Material.PURPLE_BANNER, State.EARLY);
        register(Material.PURPLE_BED, State.EARLY);
        register(Material.PURPLE_BUNDLE, State.EARLY);
        register(Material.PURPLE_CANDLE, State.LATE);
        register(Material.PURPLE_CARPET, State.EARLY);
        register(Material.PURPLE_CONCRETE, State.EARLY);
        register(Material.PURPLE_CONCRETE_POWDER, State.EARLY);
        register(Material.PURPLE_DYE, State.EARLY);
        register(Material.PURPLE_GLAZED_TERRACOTTA, State.MID);
        register(Material.PURPLE_HARNESS, State.EARLY);
        register(Material.PURPLE_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.PURPLE_STAINED_GLASS, State.EARLY);
        register(Material.PURPLE_STAINED_GLASS_PANE, State.EARLY);
        register(Material.PURPLE_TERRACOTTA, State.EARLY);
        register(Material.PURPLE_WOOL, State.EARLY);
        register(Material.PURPUR_BLOCK, State.LATE, ItemTag.END);
        register(Material.PURPUR_PILLAR, State.LATE, ItemTag.END);
        register(Material.PURPUR_SLAB, State.LATE, ItemTag.END);
        register(Material.PURPUR_STAIRS, State.LATE, ItemTag.END);
        register(Material.QUARTZ, State.MID);
        register(Material.QUARTZ_BLOCK, State.MID, ItemTag.NETHER);
        register(Material.QUARTZ_BRICKS, State.MID, ItemTag.NETHER);
        register(Material.QUARTZ_PILLAR, State.MID, ItemTag.NETHER);
        register(Material.QUARTZ_SLAB, State.MID, ItemTag.NETHER);
        register(Material.QUARTZ_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.RABBIT, State.MID);
        register(Material.RABBIT_FOOT, State.LATE);
        register(Material.RABBIT_HIDE, State.MID);
        register(Material.RABBIT_STEW, State.LATE);
        register(Material.RAIL, State.EARLY);
        register(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.RAW_COPPER, State.EARLY);
        register(Material.RAW_COPPER_BLOCK, State.EARLY);
        register(Material.RAW_GOLD, State.EARLY);
        register(Material.RAW_GOLD_BLOCK, State.EARLY);
        register(Material.RAW_IRON, State.EARLY);
        register(Material.RAW_IRON_BLOCK, State.EARLY);
        register(Material.RECOVERY_COMPASS, State.LATE, ItemTag.EXTREME);
        register(Material.RED_BANNER, State.EARLY);
        register(Material.RED_BED, State.EARLY);
        register(Material.RED_BUNDLE, State.EARLY);
        register(Material.RED_CANDLE, State.LATE);
        register(Material.RED_CARPET, State.EARLY);
        register(Material.RED_CONCRETE, State.EARLY);
        register(Material.RED_CONCRETE_POWDER, State.EARLY);
        register(Material.RED_DYE, State.EARLY);
        register(Material.RED_GLAZED_TERRACOTTA, State.MID);
        register(Material.RED_HARNESS, State.EARLY);
        register(Material.RED_MUSHROOM, State.EARLY);
        register(Material.RED_MUSHROOM_BLOCK, State.LATE);
        register(Material.RED_NETHER_BRICK_SLAB, State.MID, ItemTag.NETHER);
        register(Material.RED_NETHER_BRICK_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.RED_NETHER_BRICK_WALL, State.MID, ItemTag.NETHER);
        register(Material.RED_NETHER_BRICKS, State.MID, ItemTag.NETHER);
        register(Material.RED_SAND, State.MID);
        register(Material.RED_SANDSTONE, State.MID);
        register(Material.RED_SANDSTONE_SLAB, State.MID);
        register(Material.RED_SANDSTONE_STAIRS, State.MID);
        register(Material.RED_SANDSTONE_WALL, State.MID);
        register(Material.RED_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.RED_STAINED_GLASS, State.EARLY);
        register(Material.RED_STAINED_GLASS_PANE, State.EARLY);
        register(Material.RED_TERRACOTTA, State.EARLY);
        register(Material.RED_TULIP, State.MID);
        register(Material.RED_WOOL, State.EARLY);
        register(Material.REDSTONE, State.EARLY);
        register(Material.REDSTONE_BLOCK, State.EARLY);
        register(Material.REDSTONE_LAMP, State.MID, ItemTag.NETHER);
        register(Material.REDSTONE_ORE, State.LATE, ItemTag.EXTREME);
        register(Material.REDSTONE_TORCH, State.EARLY);
        register(Material.REPEATER, State.EARLY);
        register(Material.RESIN_BLOCK, State.LATE);
        register(Material.RESIN_BRICK, State.LATE);
        register(Material.RESIN_BRICK_SLAB, State.LATE);
        register(Material.RESIN_BRICK_STAIRS, State.LATE);
        register(Material.RESIN_BRICK_WALL, State.LATE);
        register(Material.RESIN_BRICKS, State.LATE);
        register(Material.RESIN_CLUMP, State.LATE);
        register(Material.RESPAWN_ANCHOR, State.LATE, ItemTag.NETHER);
        register(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.NETHER);
        register(Material.ROOTED_DIRT, State.MID);
        register(Material.ROSE_BUSH, State.EARLY);
        register(Material.ROTTEN_FLESH, State.EARLY);
        register(Material.SADDLE, State.EARLY);
        register(Material.SALMON, State.EARLY);
        register(Material.SALMON_BUCKET, State.EARLY);
        register(Material.SAND, State.EARLY);
        register(Material.SANDSTONE, State.EARLY);
        register(Material.SANDSTONE_SLAB, State.EARLY);
        register(Material.SANDSTONE_STAIRS, State.EARLY);
        register(Material.SANDSTONE_WALL, State.EARLY);
        register(Material.SCAFFOLDING, State.MID);
        register(Material.SCRAPE_POTTERY_SHERD, State.LATE);
        register(Material.SCULK, State.LATE);
        register(Material.SCULK_CATALYST, State.LATE);
        register(Material.SCULK_SENSOR, State.LATE);
        register(Material.SCULK_SHRIEKER, State.LATE);
        register(Material.SCULK_VEIN, State.LATE);
        register(Material.SEA_LANTERN, State.MID);
        register(Material.SEA_PICKLE, State.MID);
        register(Material.SEAGRASS, State.EARLY);
        register(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.SHEAF_POTTERY_SHERD, State.LATE);
        register(Material.SHEARS, State.EARLY);
        register(Material.SHELTER_POTTERY_SHERD, State.LATE);
        register(Material.SHIELD, State.EARLY);
        register(Material.SHORT_DRY_GRASS, State.MID);
        register(Material.SHORT_GRASS, State.EARLY);
        register(Material.SHROOMLIGHT, State.MID, ItemTag.NETHER);
        register(Material.SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.SHULKER_SHELL, State.LATE, ItemTag.END);
        register(Material.SKULL_BANNER_PATTERN, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.SKULL_POTTERY_SHERD, State.LATE);
        register(Material.SLIME_BALL, State.MID);
        register(Material.SLIME_BLOCK, State.LATE);
        register(Material.SMALL_AMETHYST_BUD, State.LATE);
        register(Material.SMALL_DRIPLEAF, State.MID);
        register(Material.SMITHING_TABLE, State.EARLY);
        register(Material.SMOKER, State.EARLY);
        register(Material.SMOOTH_BASALT, State.MID);
        register(Material.SMOOTH_QUARTZ, State.MID, ItemTag.NETHER);
        register(Material.SMOOTH_QUARTZ_SLAB, State.MID, ItemTag.NETHER);
        register(Material.SMOOTH_QUARTZ_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.SMOOTH_RED_SANDSTONE, State.MID);
        register(Material.SMOOTH_RED_SANDSTONE_SLAB, State.MID);
        register(Material.SMOOTH_RED_SANDSTONE_STAIRS, State.MID);
        register(Material.SMOOTH_SANDSTONE, State.EARLY);
        register(Material.SMOOTH_SANDSTONE_SLAB, State.EARLY);
        register(Material.SMOOTH_SANDSTONE_STAIRS, State.EARLY);
        register(Material.SMOOTH_STONE, State.EARLY);
        register(Material.SMOOTH_STONE_SLAB, State.EARLY);
        register(Material.SNIFFER_EGG, State.LATE);
        register(Material.SNORT_POTTERY_SHERD, State.LATE);
        register(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.NETHER);
        register(Material.SNOW, State.EARLY);
        register(Material.SNOW_BLOCK, State.EARLY);
        register(Material.SNOWBALL, State.EARLY);
        register(Material.SOUL_CAMPFIRE, State.MID, ItemTag.NETHER);
        register(Material.SOUL_LANTERN, State.MID, ItemTag.NETHER);
        register(Material.SOUL_SAND, State.MID, ItemTag.NETHER);
        register(Material.SOUL_SOIL, State.MID, ItemTag.NETHER);
        register(Material.SOUL_TORCH, State.MID);
        register(Material.SPECTRAL_ARROW, State.MID, ItemTag.NETHER);
        register(Material.SPIDER_EYE, State.MID);
        register(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.END);
        register(Material.SPLASH_POTION, State.LATE, ItemTag.NETHER);
        register(Material.SPONGE, State.LATE);
        register(Material.SPORE_BLOSSOM, State.MID);
        register(Material.SPRUCE_BOAT, State.EARLY);
        register(Material.SPRUCE_BUTTON, State.EARLY);
        register(Material.SPRUCE_CHEST_BOAT, State.EARLY);
        register(Material.SPRUCE_DOOR, State.EARLY);
        register(Material.SPRUCE_FENCE, State.EARLY);
        register(Material.SPRUCE_FENCE_GATE, State.EARLY);
        register(Material.SPRUCE_HANGING_SIGN, State.EARLY);
        register(Material.SPRUCE_LEAVES, State.EARLY);
        register(Material.SPRUCE_LOG, State.EARLY);
        register(Material.SPRUCE_PLANKS, State.EARLY);
        register(Material.SPRUCE_PRESSURE_PLATE, State.EARLY);
        register(Material.SPRUCE_SAPLING, State.EARLY);
        register(Material.SPRUCE_SHELF, State.EARLY);
        register(Material.SPRUCE_SIGN, State.EARLY);
        register(Material.SPRUCE_SLAB, State.EARLY);
        register(Material.SPRUCE_STAIRS, State.EARLY);
        register(Material.SPRUCE_TRAPDOOR, State.EARLY);
        register(Material.SPRUCE_WOOD, State.EARLY);
        register(Material.SPYGLASS, State.MID);
        register(Material.STICK, State.EARLY);
        register(Material.STICKY_PISTON, State.MID);
        register(Material.STONE, State.EARLY);
        register(Material.STONE_AXE, State.EARLY);
        register(Material.STONE_BRICK_SLAB, State.EARLY);
        register(Material.STONE_BRICK_STAIRS, State.EARLY);
        register(Material.STONE_BRICK_WALL, State.EARLY);
        register(Material.STONE_BRICKS, State.EARLY);
        register(Material.STONE_BUTTON, State.EARLY);
        register(Material.STONE_HOE, State.EARLY);
        register(Material.STONE_PICKAXE, State.EARLY);
        register(Material.STONE_PRESSURE_PLATE, State.EARLY);
        register(Material.STONE_SHOVEL, State.EARLY);
        register(Material.STONE_SLAB, State.EARLY);
        register(Material.STONE_SPEAR, State.EARLY);
        register(Material.STONE_STAIRS, State.EARLY);
        register(Material.STONE_SWORD, State.EARLY);
        register(Material.STONECUTTER, State.EARLY);
        register(Material.STRING, State.MID);
        register(Material.STRIPPED_ACACIA_LOG, State.EARLY);
        register(Material.STRIPPED_ACACIA_WOOD, State.EARLY);
        register(Material.STRIPPED_BAMBOO_BLOCK, State.EARLY);
        register(Material.STRIPPED_BIRCH_LOG, State.EARLY);
        register(Material.STRIPPED_BIRCH_WOOD, State.EARLY);
        register(Material.STRIPPED_CHERRY_LOG, State.MID);
        register(Material.STRIPPED_CHERRY_WOOD, State.MID);
        register(Material.STRIPPED_CRIMSON_HYPHAE, State.MID, ItemTag.NETHER);
        register(Material.STRIPPED_CRIMSON_STEM, State.MID, ItemTag.NETHER);
        register(Material.STRIPPED_DARK_OAK_LOG, State.EARLY);
        register(Material.STRIPPED_DARK_OAK_WOOD, State.EARLY);
        register(Material.STRIPPED_JUNGLE_LOG, State.EARLY);
        register(Material.STRIPPED_JUNGLE_WOOD, State.EARLY);
        register(Material.STRIPPED_MANGROVE_LOG, State.MID);
        register(Material.STRIPPED_MANGROVE_WOOD, State.MID);
        register(Material.STRIPPED_OAK_LOG, State.EARLY);
        register(Material.STRIPPED_OAK_WOOD, State.EARLY);
        register(Material.STRIPPED_PALE_OAK_LOG, State.LATE);
        register(Material.STRIPPED_PALE_OAK_WOOD, State.LATE);
        register(Material.STRIPPED_SPRUCE_LOG, State.EARLY);
        register(Material.STRIPPED_SPRUCE_WOOD, State.EARLY);
        register(Material.STRIPPED_WARPED_HYPHAE, State.MID, ItemTag.NETHER);
        register(Material.STRIPPED_WARPED_STEM, State.MID, ItemTag.NETHER);
        register(Material.SUGAR, State.EARLY);
        register(Material.SUGAR_CANE, State.EARLY);
        register(Material.SULFUR, State.MID);
        register(Material.SULFUR_BRICK_SLAB, State.MID);
        register(Material.SULFUR_BRICK_STAIRS, State.MID);
        register(Material.SULFUR_BRICK_WALL, State.MID);
        register(Material.SULFUR_BRICKS, State.MID);
        register(Material.SULFUR_CUBE_BUCKET, State.MID);
        register(Material.SULFUR_SLAB, State.MID);
        register(Material.SULFUR_SPIKE, State.MID);
        register(Material.SULFUR_STAIRS, State.MID);
        register(Material.SULFUR_WALL, State.MID);
        register(Material.SUNFLOWER, State.EARLY);
        register(Material.SUSPICIOUS_STEW, State.EARLY);
        register(Material.SWEET_BERRIES, State.EARLY);
        register(Material.TALL_DRY_GRASS, State.MID);
        register(Material.TALL_GRASS, State.LATE);
        register(Material.TARGET, State.EARLY);
        register(Material.TERRACOTTA, State.EARLY);
        register(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.TINTED_GLASS, State.MID);
        register(Material.TIPPED_ARROW, State.LATE);
        register(Material.TNT, State.MID);
        register(Material.TNT_MINECART, State.MID);
        register(Material.TORCH, State.EARLY);
        register(Material.TORCHFLOWER, State.LATE);
        register(Material.TORCHFLOWER_SEEDS, State.MID);
        register(Material.TOTEM_OF_UNDYING, State.LATE, ItemTag.EXTREME);
        register(Material.TRAPPED_CHEST, State.EARLY);
        register(Material.TRIAL_KEY, State.LATE);
        register(Material.TRIDENT, State.LATE);
        register(Material.TRIPWIRE_HOOK, State.EARLY);
        register(Material.TROPICAL_FISH, State.MID);
        register(Material.TROPICAL_FISH_BUCKET, State.MID);
        register(Material.TUBE_CORAL, State.LATE);
        register(Material.TUBE_CORAL_BLOCK, State.LATE);
        register(Material.TUBE_CORAL_FAN, State.LATE);
        register(Material.TUFF, State.EARLY);
        register(Material.TUFF_BRICK_SLAB, State.EARLY);
        register(Material.TUFF_BRICK_STAIRS, State.EARLY);
        register(Material.TUFF_BRICK_WALL, State.EARLY);
        register(Material.TUFF_BRICKS, State.EARLY);
        register(Material.TUFF_SLAB, State.EARLY);
        register(Material.TUFF_STAIRS, State.EARLY);
        register(Material.TUFF_WALL, State.EARLY);
        register(Material.TWISTING_VINES, State.MID, ItemTag.NETHER);
        register(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.VINE, State.EARLY);
        register(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE, ItemTag.EXTREME);
        register(Material.WARPED_BUTTON, State.MID, ItemTag.NETHER);
        register(Material.WARPED_DOOR, State.MID, ItemTag.NETHER);
        register(Material.WARPED_FENCE, State.MID, ItemTag.NETHER);
        register(Material.WARPED_FENCE_GATE, State.MID, ItemTag.NETHER);
        register(Material.WARPED_FUNGUS, State.MID, ItemTag.NETHER);
        register(Material.WARPED_FUNGUS_ON_A_STICK, State.MID, ItemTag.NETHER);
        register(Material.WARPED_HANGING_SIGN, State.MID, ItemTag.NETHER);
        register(Material.WARPED_HYPHAE, State.MID, ItemTag.NETHER);
        register(Material.WARPED_NYLIUM, State.LATE, ItemTag.NETHER);
        register(Material.WARPED_PLANKS, State.MID, ItemTag.NETHER);
        register(Material.WARPED_PRESSURE_PLATE, State.MID, ItemTag.NETHER);
        register(Material.WARPED_ROOTS, State.MID, ItemTag.NETHER);
        register(Material.WARPED_SHELF, State.MID, ItemTag.NETHER);
        register(Material.WARPED_SIGN, State.MID, ItemTag.NETHER);
        register(Material.WARPED_SLAB, State.MID, ItemTag.NETHER);
        register(Material.WARPED_STAIRS, State.MID, ItemTag.NETHER);
        register(Material.WARPED_STEM, State.MID, ItemTag.NETHER);
        register(Material.WARPED_TRAPDOOR, State.MID, ItemTag.NETHER);
        register(Material.WARPED_WART_BLOCK, State.MID, ItemTag.NETHER);
        register(Material.WATER_BUCKET, State.EARLY);
        register(Material.WAXED_CHISELED_COPPER, State.LATE);
        register(Material.WAXED_COPPER_BARS, State.LATE);
        register(Material.WAXED_COPPER_BLOCK, State.LATE);
        register(Material.WAXED_COPPER_BULB, State.LATE);
        register(Material.WAXED_COPPER_CHAIN, State.LATE);
        register(Material.WAXED_COPPER_CHEST, State.LATE);
        register(Material.WAXED_COPPER_DOOR, State.LATE);
        register(Material.WAXED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.WAXED_COPPER_GRATE, State.LATE);
        register(Material.WAXED_COPPER_LANTERN, State.LATE);
        register(Material.WAXED_COPPER_TRAPDOOR, State.LATE);
        register(Material.WAXED_CUT_COPPER, State.LATE);
        register(Material.WAXED_CUT_COPPER_SLAB, State.LATE);
        register(Material.WAXED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.WAXED_EXPOSED_CHISELED_COPPER, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_BARS, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_BULB, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_CHAIN, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_CHEST, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_DOOR, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_GRATE, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_LANTERN, State.LATE);
        register(Material.WAXED_EXPOSED_COPPER_TRAPDOOR, State.LATE);
        register(Material.WAXED_EXPOSED_CUT_COPPER, State.LATE);
        register(Material.WAXED_EXPOSED_CUT_COPPER_SLAB, State.LATE);
        register(Material.WAXED_EXPOSED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.WAXED_EXPOSED_LIGHTNING_ROD, State.LATE);
        register(Material.WAXED_LIGHTNING_ROD, State.LATE);
        register(Material.WAXED_OXIDIZED_CHISELED_COPPER, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_BARS, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_BULB, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_CHAIN, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_CHEST, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_DOOR, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_GRATE, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_LANTERN, State.LATE);
        register(Material.WAXED_OXIDIZED_COPPER_TRAPDOOR, State.LATE);
        register(Material.WAXED_OXIDIZED_CUT_COPPER, State.LATE);
        register(Material.WAXED_OXIDIZED_CUT_COPPER_SLAB, State.LATE);
        register(Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.WAXED_OXIDIZED_LIGHTNING_ROD, State.LATE);
        register(Material.WAXED_WEATHERED_CHISELED_COPPER, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_BARS, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_BULB, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_CHAIN, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_CHEST, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_DOOR, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_GRATE, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_LANTERN, State.LATE);
        register(Material.WAXED_WEATHERED_COPPER_TRAPDOOR, State.LATE);
        register(Material.WAXED_WEATHERED_CUT_COPPER, State.LATE);
        register(Material.WAXED_WEATHERED_CUT_COPPER_SLAB, State.LATE);
        register(Material.WAXED_WEATHERED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.WAXED_WEATHERED_LIGHTNING_ROD, State.LATE);
        register(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.WEATHERED_CHISELED_COPPER, State.LATE);
        register(Material.WEATHERED_COPPER, State.LATE);
        register(Material.WEATHERED_COPPER_BARS, State.LATE);
        register(Material.WEATHERED_COPPER_BULB, State.LATE);
        register(Material.WEATHERED_COPPER_CHAIN, State.LATE);
        register(Material.WEATHERED_COPPER_CHEST, State.LATE);
        register(Material.WEATHERED_COPPER_DOOR, State.LATE);
        register(Material.WEATHERED_COPPER_GOLEM_STATUE, State.LATE);
        register(Material.WEATHERED_COPPER_GRATE, State.LATE);
        register(Material.WEATHERED_COPPER_LANTERN, State.LATE);
        register(Material.WEATHERED_COPPER_TRAPDOOR, State.LATE);
        register(Material.WEATHERED_CUT_COPPER, State.LATE);
        register(Material.WEATHERED_CUT_COPPER_SLAB, State.LATE);
        register(Material.WEATHERED_CUT_COPPER_STAIRS, State.LATE);
        register(Material.WEATHERED_LIGHTNING_ROD, State.LATE);
        register(Material.WEEPING_VINES, State.MID, ItemTag.NETHER);
        register(Material.WET_SPONGE, State.LATE);
        register(Material.WHEAT, State.EARLY);
        register(Material.WHEAT_SEEDS, State.EARLY);
        register(Material.WHITE_BANNER, State.EARLY);
        register(Material.WHITE_BED, State.EARLY);
        register(Material.WHITE_BUNDLE, State.EARLY);
        register(Material.WHITE_CANDLE, State.LATE);
        register(Material.WHITE_CARPET, State.EARLY);
        register(Material.WHITE_CONCRETE, State.EARLY);
        register(Material.WHITE_CONCRETE_POWDER, State.EARLY);
        register(Material.WHITE_DYE, State.EARLY);
        register(Material.WHITE_GLAZED_TERRACOTTA, State.MID);
        register(Material.WHITE_HARNESS, State.EARLY);
        register(Material.WHITE_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.WHITE_STAINED_GLASS, State.EARLY);
        register(Material.WHITE_STAINED_GLASS_PANE, State.EARLY);
        register(Material.WHITE_TERRACOTTA, State.EARLY);
        register(Material.WHITE_TULIP, State.MID);
        register(Material.WHITE_WOOL, State.EARLY);
        register(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, State.LATE);
        register(Material.WILDFLOWERS, State.EARLY);
        register(Material.WIND_CHARGE, State.LATE);
        register(Material.WITHER_ROSE, State.MID);
        register(Material.WITHER_SKELETON_SKULL, State.LATE, ItemTag.NETHER, ItemTag.EXTREME);
        register(Material.WOLF_ARMOR, State.LATE);
        register(Material.WOODEN_AXE, State.EARLY);
        register(Material.WOODEN_HOE, State.EARLY);
        register(Material.WOODEN_PICKAXE, State.EARLY);
        register(Material.WOODEN_SHOVEL, State.EARLY);
        register(Material.WOODEN_SPEAR, State.EARLY);
        register(Material.WOODEN_SWORD, State.EARLY);
        register(Material.WRITABLE_BOOK, State.EARLY);
        register(Material.WRITTEN_BOOK, State.EARLY);
        register(Material.YELLOW_BANNER, State.EARLY);
        register(Material.YELLOW_BED, State.EARLY);
        register(Material.YELLOW_BUNDLE, State.EARLY);
        register(Material.YELLOW_CANDLE, State.LATE);
        register(Material.YELLOW_CARPET, State.EARLY);
        register(Material.YELLOW_CONCRETE, State.EARLY);
        register(Material.YELLOW_CONCRETE_POWDER, State.EARLY);
        register(Material.YELLOW_DYE, State.EARLY);
        register(Material.YELLOW_GLAZED_TERRACOTTA, State.MID);
        register(Material.YELLOW_HARNESS, State.EARLY);
        register(Material.YELLOW_SHULKER_BOX, State.LATE, ItemTag.END);
        register(Material.YELLOW_STAINED_GLASS, State.EARLY);
        register(Material.YELLOW_STAINED_GLASS_PANE, State.EARLY);
        register(Material.YELLOW_TERRACOTTA, State.EARLY);
        register(Material.YELLOW_WOOL, State.EARLY);
    }

    /** Tags that describe item properties/requirements. An item can have multiple tags. */
    public enum ItemTag {
        /** Item requires nether access to obtain */
        NETHER,
        /** Item requires end access to obtain */
        END,
        /** Item is extremely hard/unrealistic to obtain in 45 minutes */
        EXTREME
    }

    /**
     * The three item pools, in unlock order. Carries only its own identity — label and colour.
     * A pool's contents and the point in the round it opens at are per-game facts and stay on
     * {@link ItemDifficultiesManager}; keep mutable state off these constants.
     */
    @Getter
    public enum State {
        EARLY("Early", "green"),
        MID("Mid", "yellow"),
        LATE("Late", "red");

        static final State[] VALUES = values();

        private final String displayName;
        private final String color;

        State(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /** Defines an item with its game state (when it unlocks) and tags (requirements/properties). */
    public record ItemDefinition(Material material, State state, Set<ItemTag> tags) {
        public ItemDefinition(Material material, State state, ItemTag... tags) {
            this(material, state, tags.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(tags)));
        }

        public boolean hasTag(ItemTag tag) {
            return tags.contains(tag);
        }

        public boolean hasAnyTag(ItemTag... checkTags) {
            for (ItemTag tag : checkTags) {
                if (tags.contains(tag)) return true;
            }
            return false;
        }
    }
}
