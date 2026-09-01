package forceitembattle;

import forceitembattle.commands.CommandsManager;
import forceitembattle.commands.admin.CommandForceItem;
import forceitembattle.commands.admin.CommandForceTeam;
import forceitembattle.commands.admin.CommandItems;
import forceitembattle.commands.admin.CommandRandomEvent;
import forceitembattle.commands.admin.CommandReset;
import forceitembattle.commands.admin.CommandSettings;
import forceitembattle.commands.admin.CommandSkip;
import forceitembattle.commands.admin.CommandStart;
import forceitembattle.commands.admin.CommandStopTimer;
import forceitembattle.commands.player.CommandAchievement;
import forceitembattle.commands.player.CommandBed;
import forceitembattle.commands.player.CommandBp;
import forceitembattle.commands.player.CommandCollection;
import forceitembattle.commands.player.CommandFixSkips;
import forceitembattle.commands.player.CommandHelp;
import forceitembattle.commands.player.CommandInfo;
import forceitembattle.commands.player.CommandInfoWiki;
import forceitembattle.commands.player.CommandLeaderboard;
import forceitembattle.commands.player.CommandFixLocate;
import forceitembattle.commands.player.CommandPause;
import forceitembattle.commands.player.CommandPing;
import forceitembattle.commands.player.CommandPosition;
import forceitembattle.commands.player.CommandResult;
import forceitembattle.commands.player.CommandResume;
import forceitembattle.commands.player.CommandShout;
import forceitembattle.commands.player.CommandSpawn;
import forceitembattle.commands.player.CommandSpectate;
import forceitembattle.commands.player.CommandStats;
import forceitembattle.commands.player.CommandTeams;
import forceitembattle.commands.player.CommandVote;
import forceitembattle.commands.player.CommandVoteSkip;
import forceitembattle.listener.AchievementListener;
import forceitembattle.listener.AntimatterPortalListener;
import forceitembattle.listener.ChatListener;
import forceitembattle.listener.ClickableItemsListener;
import forceitembattle.listener.FoundItemListener;
import forceitembattle.listener.GameRulesListener;
import forceitembattle.listener.GuiListener;
import forceitembattle.listener.ItemsListener;
import forceitembattle.listener.JournalListener;
import forceitembattle.listener.PlayerLifecycleListener;
import forceitembattle.listener.PortalListener;
import forceitembattle.listener.PreGameLockListener;
import forceitembattle.listener.ProtectionListener;
import forceitembattle.listener.PvPListener;
import forceitembattle.listener.RecipeListener;
import forceitembattle.listener.SettingsListener;
import forceitembattle.listener.TradeListener;
import forceitembattle.listener.VillagerTradeListener;
import forceitembattle.manager.AchievementManager;
import forceitembattle.manager.CollectionManager;
import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.manager.CustomItemManager;
import forceitembattle.manager.BackToBackManager;
import forceitembattle.manager.FoundItemResolver;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.LocatorManager;
import forceitembattle.manager.Manager;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.manager.PositionManager;
import forceitembattle.manager.ProtectionManager;
import forceitembattle.manager.RandomEventManager;
import forceitembattle.manager.RecipeManager;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TabListManager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.VoteSkipManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.manager.BackpackManager;
import forceitembattle.util.FileLogger;
import forceitembattle.util.Scheduler;
import forceitembattle.manager.TimerManager;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.util.SeedPool;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForceItemBattle extends JavaPlugin {

    /** Every manager, in construction order. Membership only — see {@link #lifecycleOrder()}. */
    private final List<Manager> managers = new ArrayList<>();

    /** The lifecycle order, resolved once at boot so disable() reverses exactly what enable() ran. */
    private List<Manager> lifecycle = List.of();
    /**
     * Who is in the round. Constructed before every manager and depending on none of them, which
     * is what keeps the manager graph acyclic — see {@link forceitembattle.model.Roster}.
     */
    @Getter
    private final Roster roster = new Roster();

    /** Where the round is. Like the roster: constructed before every manager, depending on none. */
    @Getter
    private final RoundPhase roundPhase = new RoundPhase();

    /**
     * How much of the round is left, and how long it is. Shared rather than owned by
     * {@code TimerManager}, which merely drives it — see {@link forceitembattle.model.RoundClock}.
     */
    @Getter
    private final RoundClock roundClock = new RoundClock();

    /**
     * The end-of-round reveal. Owned here rather than by a manager for the same reason as the
     * roster and the phase: it depends on nothing, so nothing depends on a manager to reach it.
     */
    @Getter
    private final ResultCeremony resultCeremony = new ResultCeremony();
    @Getter
    private Gamemanager gamemanager;
    @Getter
    private FoundItemResolver foundItemResolver;
    @Getter
    private TimerManager timerManager;
    @Getter
    private BackpackManager backpackManager;
    @Getter
    private BackToBackManager backToBackManager;
    @Getter
    private ItemDifficultiesManager itemDifficultiesManager;
    @Getter
    private AntimatterPortalManager antimatterPortalManager;
    @Getter
    private CustomItemManager customItemManager;
    @Getter
    private RecipeManager recipeManager;
    @Getter
    private PositionManager positionManager;
    @Getter
    private WanderingTraderManager wanderingTraderManager;
    @Getter
    private RandomEventManager randomEventManager;
    @Getter
    private TabListManager tabListManager;
    @Getter
    @Setter
    private CommandsManager commandsManager;
    @Getter
    @Setter
    private TeamsManager teamManager;
    @Getter
    private AchievementManager achievementManager;
    @Getter
    private CollectionManager collectionManager;
    @Getter
    private AchievementListener achievementListener;
    @Getter
    private LocatorManager locatorManager;
    @Getter
    private ProtectionManager protectionManager;
    @Getter
    private VoteSkipManager voteSkipManager;
    @Getter
    private ScoreboardManager scoreboardManager;
    @Getter
    private FIBServiceClient fibService;
    @Getter
    @Setter
    private Location spawnLocation;
    @Getter
    private GameSettings settings;
    @Getter
    private SeedPool seedPool;

    @Override
    public void onLoad() {
        saveConfig();

        this.settings = new GameSettings(this);

        saveConfig();
    }

    /**
     * Records a manager for its lifecycle. <b>Does not decide when it is enabled</b> — that is
     * {@link #lifecycleOrder()}, declared separately and deliberately. Why the two are separate
     * lists is in {@code CONTEXT.md § Manager Lifecycle}.
     */
    private <T extends Manager> T register(T manager) {
        this.managers.add(manager);
        return manager;
    }

    /**
     * The order managers are enabled in, and — reversed — disabled in.
     *
     * <p><b>Load-bearing.</b> {@code TimerManager} starting its task earlier or saving its config
     * later are live behaviour changes no test would catch, which is why this is a list edited
     * deliberately rather than a side effect of where a {@code new} happens to sit.
     */
    private List<Manager> lifecycleOrder() {
        return List.of(
                this.gamemanager,
                this.timerManager,
                this.backpackManager,
                this.backToBackManager,
                this.customItemManager,
                this.itemDifficultiesManager,
                this.recipeManager,
                this.antimatterPortalManager,
                this.positionManager,
                this.teamManager,
                this.commandsManager,
                this.achievementManager,
                this.collectionManager,
                this.locatorManager,
                this.protectionManager,
                this.wanderingTraderManager,
                this.randomEventManager,
                this.tabListManager,
                this.voteSkipManager,
                this.scoreboardManager,
                this.fibService,
                this.foundItemResolver);
    }

    /**
     * Every registered manager appears in the lifecycle order exactly once. A manager registered
     * but left out of {@link #lifecycleOrder()} would never be enabled and nothing else would say
     * so, hence the boot failure — the same bargain {@code CommandsManager} makes for an
     * unregistered command.
     */
    private void verifyLifecycleCoversEveryManager(List<Manager> order) {
        if (order.size() != this.managers.size() || !order.containsAll(this.managers)) {
            List<Manager> missing = new ArrayList<>(this.managers);
            missing.removeAll(order);
            throw new IllegalStateException(
                    "lifecycleOrder() must list every registered manager exactly once; missing: "
                            + missing.stream().map(m -> m.getClass().getSimpleName()).toList());
        }
    }

    @Override
    public void onEnable() {
        Scheduler.init(this);
        FileLogger.init(getDataFolder());
        this.seedPool = new SeedPool(this);
        this.seedPool.load();

        this.gamemanager = register(new Gamemanager(this, this.roster, this.roundPhase));
        this.timerManager = register(new TimerManager(this, this.roundClock));
        this.backpackManager = register(new BackpackManager(this));
        this.backToBackManager = register(new BackToBackManager(this));
        this.customItemManager = register(new CustomItemManager(this));
        this.itemDifficultiesManager = register(new ItemDifficultiesManager(this));
        this.recipeManager = register(new RecipeManager(this, this.settings));
        this.antimatterPortalManager = register(new AntimatterPortalManager(this));
        this.positionManager = register(new PositionManager(this));
        this.teamManager = register(new TeamsManager(this));
        this.commandsManager = register(new CommandsManager(this));
        this.achievementManager = register(new AchievementManager(this));

        // Constructed here rather than last: CollectionManager's loaders need it, and it needs
        // AchievementManager's cache. Its enable()/disable() position is unchanged -- that is
        // lifecycleOrder()'s business now, not this line's, which is the whole point of the split.
        this.fibService = register(new FIBServiceClient(this, this.achievementManager.getGlobalStatsCache()));

        this.collectionManager = register(
                new CollectionManager(this.itemDifficultiesManager, this.fibService));
        this.locatorManager = register(new LocatorManager(this, this.positionManager));
        this.protectionManager = register(new ProtectionManager(this.roster, this.gamemanager));
        this.wanderingTraderManager = register(new WanderingTraderManager(this));
        this.randomEventManager = register(new RandomEventManager(this));
        this.tabListManager = register(new TabListManager(this.roster, this.gamemanager, this.itemDifficultiesManager, this.randomEventManager, this.wanderingTraderManager));
        this.voteSkipManager = register(new VoteSkipManager(this.roster, this.gamemanager, this.itemDifficultiesManager));
        this.scoreboardManager = register(new ScoreboardManager(this));

        // Named dependencies rather than the plugin, so this one has to be built after all of them.
        // That ordering requirement is the cost of the explicitness, and it is why the managers
        // above still take the plugin: most are constructed before their collaborators exist.
        this.foundItemResolver = register(new FoundItemResolver(
                this.settings,
                this.gamemanager,
                this.scoreboardManager,
                this.backToBackManager,
                this.randomEventManager,
                this.timerManager,
                this.itemDifficultiesManager,
                this.fibService));

        // Construction above follows dependencies; this follows the lifecycle. They are no longer
        // the same list, which is what lets a manager take a sibling constructed before it.
        this.lifecycle = this.lifecycleOrder();
        this.verifyLifecycleCoversEveryManager(this.lifecycle);
        this.lifecycle.forEach(Manager::enable);

        this.initListeners();
        this.initCommands();

        // Paper initialises the plugin before the worlds, so a 0-delay task is what puts this
        // after they exist.
        Scheduler.runLaterSync(() -> Bukkit.getWorlds().forEach(world -> {
            boolean keepInventory = getSettings().isSettingEnabled(GameSetting.KEEP_INVENTORY);
            if (getSettings().isSettingEnabled(GameSetting.EVENT) && !keepInventory) {

                keepInventory = true;
                Scheduler.runLaterSync(
                        () -> world.setGameRule(GameRules.KEEP_INVENTORY, false),
                        20 * 60 * 5 // 5 minutes
                );
            }

            world.setGameRule(GameRules.KEEP_INVENTORY, keepInventory);
            getSettings().setSettingEnabled(GameSetting.FASTER_RANDOM_TICK, getSettings().isSettingEnabled(GameSetting.FASTER_RANDOM_TICK));

            world.setGameRule(GameRules.LOCATOR_BAR, false);
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false);
            world.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
            world.setGameRule(GameRules.SPAWN_PHANTOMS, false);

            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(world.getSpawnLocation());
            worldBorder.setSize(30);

            forceloadChunksAround(world.getSpawnLocation(), 2);
        }), 0L);
    }

    public void forceloadChunksAround(Location center, int radiusChunks) {
        World world = center.getWorld();
        int centerChunkX = center.getChunk().getX();
        int centerChunkZ = center.getChunk().getZ();

        for (int x = centerChunkX - radiusChunks; x <= centerChunkX + radiusChunks; x++) {
            for (int z = centerChunkZ - radiusChunks; z <= centerChunkZ + radiusChunks; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                chunk.setForceLoaded(true);
            }
        }
    }

    private void copyDatapack(String datapackName) {
        File world = new File(Bukkit.getWorldContainer(), "world");

        try {
            Path sourceDirectory = Paths.get(this.getDataFolder() + "/" + datapackName + ".zip");
            Path destinationDirectory = Paths.get(world + "/datapacks/" + datapackName + ".zip");

            Files.walk(sourceDirectory)
                    .forEach(source -> {
                        try {
                            Path destination = destinationDirectory.resolve(sourceDirectory.relativize(source));
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            System.out.println("Directory copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scheduleReset(Long seed) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writeLevelSeed(seed == null ? "" : Long.toString(seed));
            } catch (IOException e) {
                System.out.println("[FIB] Failed to set level-seed; resetting with existing seed.");
                e.printStackTrace();
            }

            try {
                File world = new File(Bukkit.getWorldContainer(), "world").toPath().normalize().toFile();
                if (world.exists()) {
                    FileUtils.deleteDirectory(world);
                    System.out.println("[FIB] World deleted successfully.");
                }

                world.mkdirs();
                new File(world, "datapacks").mkdirs();
                this.copyDatapack("FIB_Worldgen");
                System.out.println("[FIB] Datapack copied.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        Bukkit.restart();
    }

    private void writeLevelSeed(String value) throws IOException {
        File props = new File("server.properties");
        if (!props.isFile()) {
            System.out.println("[FIB] server.properties not found; cannot set level-seed.");
            return;
        }

        List<String> lines = Files.readAllLines(props.toPath(), StandardCharsets.UTF_8);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("level-seed=")) {
                lines.set(i, "level-seed=" + value);
                found = true;
                break;
            }
        }
        if (!found) {
            lines.add("level-seed=" + value);
        }

        Files.write(props.toPath(), lines, StandardCharsets.UTF_8);
        System.out.println("[FIB] level-seed set to '" + value + "'.");
    }

    private void initListeners() {
        registerListeners(
                new FoundItemListener(this.roster, this.foundItemResolver, this.gamemanager),
                new SettingsListener(this.settings),
                new RecipeListener(this.recipeManager),
                new PvPListener(this.roundPhase, this.settings),
                new ProtectionListener(this, this.roster, this.roundPhase, this.protectionManager),
                new ClickableItemsListener(this, this.roster, this.backpackManager, this.fibService, this.roundPhase, this.locatorManager, this.settings, this.timerManager),
                new ItemsListener(this.roster, this.roundPhase),
                new PortalListener(this.roster, this.antimatterPortalManager, this.fibService, this.roundPhase, this.settings),
                new AntimatterPortalListener(this.antimatterPortalManager, this.roundPhase),
                new AchievementListener(this.roster, this.achievementManager, this.backpackManager, this.roundPhase, this.settings),
                new PreGameLockListener(this.roundPhase),
                new ChatListener(this, this.roster, this.gamemanager, this.settings),
                new PlayerLifecycleListener(this.roster, this.fibService, this.roundPhase, this.gamemanager, this.scoreboardManager, this.settings, this.teamManager, this.timerManager),
                new TradeListener(this.wanderingTraderManager),
                new VillagerTradeListener(this),
                new GameRulesListener(this.roundPhase, this.settings),
                new GuiListener(this),
                new JournalListener(this)
        );

    }

    public void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void initCommands() {
        CommandsManager commands = this.commandsManager;

        commands.registerCommand(new CommandStart(this));
        commands.registerCommand(new CommandSettings(this));
        commands.registerCommand(new CommandSkip(this));
        commands.registerCommand(new CommandReset(this));
        commands.registerCommand(new CommandBp(this));
        commands.registerCommand(new CommandResult(this));
        commands.registerCommand(new CommandInfo(this));
        commands.registerCommand(new CommandItems(this));
        commands.registerCommand(new CommandStopTimer(this));
        commands.registerCommand(new CommandInfoWiki(this));
        commands.registerCommand(new CommandSpawn(this));
        commands.registerCommand(new CommandBed(this));
        commands.registerCommand(new CommandPause(this));
        commands.registerCommand(new CommandResume(this));
        commands.registerCommand(new CommandStats(this));
        commands.registerCommand(new CommandCollection(this));
        commands.registerCommand(new CommandLeaderboard(this));
        commands.registerCommand(new CommandPosition(this));
        commands.registerCommand(new CommandPing(this));
        commands.registerCommand(new CommandHelp(this));
        commands.registerCommand(new CommandTeams(this));
        commands.registerCommand(new CommandFixSkips(this));
        commands.registerCommand(new CommandAchievement(this));
        commands.registerCommand(new CommandSpectate(this));
        commands.registerCommand(new CommandShout(this));
        commands.registerCommand(new CommandForceTeam(this));
        commands.registerCommand(new CommandVote(this));
        commands.registerCommand(new CommandVoteSkip(this));
        commands.registerCommand(new CommandFixLocate(this));
        commands.registerCommand(new CommandForceItem(this));
        commands.registerCommand(new CommandRandomEvent(this));

        commands.warnAboutUnboundCommands();
    }

    @Override
    public void onDisable() {
        for (int i = this.lifecycle.size() - 1; i >= 0; i--) {
            this.lifecycle.get(i).disable();
        }
    }
}
