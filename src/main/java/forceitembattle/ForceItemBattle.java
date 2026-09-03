package forceitembattle;

import forceitembattle.achievements.AchievementListener;
import forceitembattle.achievements.AchievementManager;
import forceitembattle.achievements.AchievementStorage;
import forceitembattle.achievements.PluginAchievementWorld;
import forceitembattle.achievements.ServiceAchievementSink;
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.achievements.global.GlobalStatsLoader;
import forceitembattle.collection.CollectionManager;
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
import forceitembattle.commands.player.CommandFixLocate;
import forceitembattle.commands.player.CommandFixSkips;
import forceitembattle.commands.player.CommandHelp;
import forceitembattle.commands.player.CommandInfo;
import forceitembattle.commands.player.CommandInfoWiki;
import forceitembattle.commands.player.CommandLeaderboard;
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
import forceitembattle.gui.GuiContext;
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
import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.manager.BackToBackManager;
import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.CustomItemManager;
import forceitembattle.manager.ForceItemAssignment;
import forceitembattle.manager.FoundItemResolver;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.LocatorManager;
import forceitembattle.manager.Manager;
import forceitembattle.manager.PositionManager;
import forceitembattle.manager.ProtectionManager;
import forceitembattle.manager.RecipeManager;
import forceitembattle.manager.ScatterDestinations;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TabListManager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.manager.VoteSkipManager;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.model.FindDetection;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.randomevents.EventContext;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.FileLogger;
import forceitembattle.util.Scheduler;
import forceitembattle.util.SeedPool;
import forceitembattle.util.WorldReset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
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
     * Who is in the round. Constructed before every manager and depending on none of them, which is
     * what keeps the manager graph acyclic. The three fields below are here for the same reason.
     */
    @Getter
    private final Roster roster = new Roster();

    @Getter
    private final RoundPhase roundPhase = new RoundPhase();

    /** Shared rather than owned by {@code TimerManager}, which merely drives it. */
    @Getter
    private final RoundClock roundClock = new RoundClock();

    @Getter
    private final ResultCeremony resultCeremony = new ResultCeremony();

    /**
     * Held here rather than inside {@code AchievementManager} so the service client can be built
     * before it — that ordering is what breaks the service/collection/achievement cycle.
     */
    private final GlobalStatsCache globalStatsCache = new GlobalStatsCache();

    /** Not a {@link Manager}: it owns no state and runs entirely inside a shutdown hook. */
    private WorldReset worldReset;

    /** The menu layer's whole surface — see {@link GuiContext}. Built after the managers it names. */
    private GuiContext guiContext;

    @Getter
    private Gamemanager gamemanager;
    /** Not a {@link Manager}: no lifecycle, just the rule for what item an owner holds. */
    private ForceItemAssignment forceItemAssignment;
    private FoundItemResolver foundItemResolver;
    private TimerManager timerManager;
    private BackpackManager backpackManager;
    private BackToBackManager backToBackManager;
    private ItemDifficultiesManager itemDifficultiesManager;
    private AntimatterPortalManager antimatterPortalManager;
    private CustomItemManager customItemManager;
    private RecipeManager recipeManager;
    private PositionManager positionManager;
    private WanderingTraderManager wanderingTraderManager;
    private RandomEventManager randomEventManager;
    private TabListManager tabListManager;
    private CommandsManager commandsManager;
    private TeamsManager teamManager;
    private AchievementManager achievementManager;
    private CollectionManager collectionManager;
    private AchievementListener achievementListener;
    private LocatorManager locatorManager;
    private ProtectionManager protectionManager;
    private VoteSkipManager voteSkipManager;
    @Getter
    private ScoreboardManager scoreboardManager;
    private FIBServiceClient fibService;
    @Getter
    @Setter
    private Location spawnLocation;
    @Getter
    private GameSettings settings;
    private SeedPool seedPool;

    @Override
    public void onLoad() {
        saveConfig();

        this.worldReset = new WorldReset(getDataFolder());
        this.settings = new GameSettings(this);

        saveConfig();
    }

    /** Records a manager. <b>Does not decide when it is enabled</b> — {@link #lifecycleOrder()} does. */
    private <T extends Manager> T register(T manager) {
        this.managers.add(manager);
        return manager;
    }

    /**
     * The order managers are enabled in, and — reversed — disabled in. Load-bearing:
     * {@code TimerManager} starting its task earlier or saving its config later are live behaviour
     * changes no test would catch, so this is edited deliberately rather than following construction.
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
     * A manager registered but left out of {@link #lifecycleOrder()} would never be enabled and
     * nothing else would say so, hence the boot failure.
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

        // Dependency order. Every manager takes what it needs by name, so this is a topological
        // sort of the graph rather than a free choice — the three exceptions are the Suppliers
        // below, which are the only genuine cycles left.
        this.itemDifficultiesManager = register(new ItemDifficultiesManager(this, this.roundClock, this.settings));
        this.customItemManager = register(new CustomItemManager(this));
        this.antimatterPortalManager = register(new AntimatterPortalManager(this));
        this.positionManager = register(new PositionManager());
        this.recipeManager = register(new RecipeManager(this, this.settings));
        this.backpackManager = register(new BackpackManager(this, this.roster));
        this.locatorManager = register(new LocatorManager(this.positionManager));

        // Who is hunting what. Depends on the roster and the pool and nothing else, so it is built
        // early and five of its callers stop needing the round orchestrator entirely.
        this.forceItemAssignment = new ForceItemAssignment(this.roster, this.itemDifficultiesManager);

        // The service client builds the match-history and catalogue clients, which read the
        // collection and achievement managers — both built out of this one. Late-bound, and only
        // ever dereferenced long after boot.
        this.fibService = register(new FIBServiceClient(this, this.globalStatsCache,
                () -> this.achievementManager, () -> this.collectionManager));
        this.collectionManager = register(
                new CollectionManager(this.itemDifficultiesManager, this.fibService));

        // The trader manager repaints the scoreboard; the scoreboard lists the traders.
        this.wanderingTraderManager = register(new WanderingTraderManager(this, this.roster, this.roundPhase,
                this.positionManager, this.locatorManager, () -> this.scoreboardManager));
        this.scoreboardManager = register(new ScoreboardManager(this.roster, this.settings,
                this.wanderingTraderManager, this.itemDifficultiesManager));
        this.teamManager = register(new TeamsManager(this, this.roster, () -> this.scoreboardManager));

        // The world's timer lookup is late-bound: the timer needs the game manager, which needs this.
        this.achievementManager = register(new AchievementManager(this.roster, this.roundPhase,
                this.settings, this.collectionManager,
                new AchievementStorage(new ServiceAchievementSink(this, this.fibService)),
                this.globalStatsCache,
                new GlobalStatsLoader(this.fibService, this.globalStatsCache),
                new PluginAchievementWorld(this.roster, this.roundClock, this.settings,
                        this.itemDifficultiesManager, this.backpackManager,
                        this.wanderingTraderManager)));

        this.randomEventManager = register(new RandomEventManager(
                new EventContext(this, this.roundPhase, this.settings,
                        this.itemDifficultiesManager, this.wanderingTraderManager),
                this.roster, this.roundClock, this.settings));
        this.backToBackManager = register(new BackToBackManager(this.settings, this.itemDifficultiesManager,
                this.backpackManager, this.fibService));

        this.gamemanager = register(new Gamemanager(this, this.roster, this.roundPhase, this.settings,
                this.roundClock, this.resultCeremony, this.itemDifficultiesManager, this.backpackManager,
                this.recipeManager, this.positionManager, this.scoreboardManager, this.teamManager,
                this.wanderingTraderManager, this.randomEventManager, this.achievementManager,
                this.fibService, this::setSpawnLocation));

        this.protectionManager = register(new ProtectionManager(this.roster, this.gamemanager));
        this.tabListManager = register(new TabListManager(this.roster, this.gamemanager, this.itemDifficultiesManager, this.randomEventManager, this.wanderingTraderManager));
        this.timerManager = register(new TimerManager(this, this.roundClock, this.roster, this.roundPhase,
                this.settings, this.gamemanager, this.itemDifficultiesManager, this.randomEventManager,
                this.tabListManager));
        this.voteSkipManager = register(new VoteSkipManager(this.roster, this.forceItemAssignment, this.settings, this.itemDifficultiesManager));
        this.commandsManager = register(new CommandsManager(this, this.roundPhase, this.settings, this.roster));

        this.guiContext = new GuiContext(this, this.achievementManager, this.collectionManager,
                this.itemDifficultiesManager, this.fibService);

        this.foundItemResolver = register(new FoundItemResolver(
                this.settings,
                this.gamemanager,
                this.forceItemAssignment,
                this.scoreboardManager,
                this.backToBackManager,
                this.randomEventManager,
                this.roundClock,
                this.itemDifficultiesManager,
                this.fibService));

        // Construction above follows dependencies; this follows the lifecycle. Keeping them separate
        // is what lets a manager take a sibling constructed before it.
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

    private static void forceloadChunksAround(Location center, int radiusChunks) {
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

    /**
     * Delegates to {@link WorldReset}; kept here because every command reaches its collaborators
     * through {@code this.plugin}. Drop it once {@code CommandReset} takes a {@code WorldReset}.
     */
    public void scheduleReset(Long seed) {
        this.worldReset.scheduleReset(seed);
    }

    private void initListeners() {
        registerListeners(
                new FoundItemListener(this.roster, this.foundItemResolver, this.gamemanager),
                new SettingsListener(this.settings),
                new RecipeListener(this.recipeManager),
                new PvPListener(this.roundPhase, this.settings),
                new ProtectionListener(this.roster, this.roundPhase, this.protectionManager),
                new ClickableItemsListener(this::getSpawnLocation, this.guiContext,
                        this.itemDifficultiesManager, this.roster, this.backpackManager, this.fibService, this.roundPhase, this.locatorManager, this.settings, this.timerManager),
                new ItemsListener(new FindDetection(this.roster, this.roundPhase)),
                new PortalListener(this.roster, this.antimatterPortalManager, this.fibService,
                        this.roundPhase, this.settings, new ScatterDestinations()),
                new AntimatterPortalListener(this.antimatterPortalManager, this.roundPhase),
                new AchievementListener(this.roster, this.achievementManager, this.backpackManager, this.roundPhase, this.settings),
                new PreGameLockListener(this.roundPhase),
                new ChatListener(this.roster, this.gamemanager, this.settings),
                new PlayerLifecycleListener(this.roster, this.fibService, this.roundPhase, this.gamemanager, this.scoreboardManager, this.settings, this.teamManager, this.timerManager),
                new TradeListener(this.wanderingTraderManager),
                new VillagerTradeListener(this),
                new GameRulesListener(this.roundPhase, this.settings),
                new GuiListener(),
                new JournalListener()
        );

    }

    public void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void initCommands() {
        CommandsManager commands = this.commandsManager;

        commands.registerCommand(new CommandStart(this.gamemanager, this.forceItemAssignment, this.roster, this.roundPhase,
                this.roundClock, this.settings, this.teamManager));
        commands.registerCommand(new CommandSettings(this.roster, this.settings));
        commands.registerCommand(new CommandSkip(this.forceItemAssignment, this.roster, this.settings));
        commands.registerCommand(new CommandReset(this.seedPool, this.worldReset));
        commands.registerCommand(new CommandBp(this.backpackManager));
        commands.registerCommand(new CommandResult(this.gamemanager, this.roundPhase, this.roster, this.settings,
                this.teamManager, this.resultCeremony));
        commands.registerCommand(new CommandInfo(this.roster, this.roundPhase, this.itemDifficultiesManager, this.recipeManager));
        commands.registerCommand(new CommandItems(this.itemDifficultiesManager));
        commands.registerCommand(new CommandStopTimer(this.timerManager));
        commands.registerCommand(new CommandInfoWiki(this.roster, this.roundPhase));
        commands.registerCommand(new CommandSpawn(this::getSpawnLocation));
        commands.registerCommand(new CommandBed());
        commands.registerCommand(new CommandPause(this.gamemanager));
        commands.registerCommand(new CommandResume(this.gamemanager));
        commands.registerCommand(new CommandStats(this.itemDifficultiesManager, this.fibService));
        commands.registerCommand(new CommandCollection(this.guiContext));
        commands.registerCommand(new CommandLeaderboard(this.fibService));
        commands.registerCommand(new CommandPosition(this.roster, this.positionManager));
        commands.registerCommand(new CommandPing());
        commands.registerCommand(new CommandHelp(this.commandsManager));
        commands.registerCommand(new CommandTeams(this.roster, this.teamManager));
        commands.registerCommand(new CommandFixSkips(this.roster, this.backpackManager));
        commands.registerCommand(new CommandAchievement(this.achievementManager, this.guiContext));
        commands.registerCommand(new CommandSpectate(this.roundPhase));
        commands.registerCommand(new CommandShout());
        commands.registerCommand(new CommandForceTeam(this.roster, this.teamManager));
        commands.registerCommand(new CommandVote(this.voteSkipManager));
        commands.registerCommand(new CommandVoteSkip(this.roster, this.voteSkipManager));
        commands.registerCommand(new CommandFixLocate(this.locatorManager));
        commands.registerCommand(new CommandForceItem(this.forceItemAssignment, this.settings, this.timerManager, this.roster, this.scoreboardManager));
        commands.registerCommand(new CommandRandomEvent(this.randomEventManager));

        commands.warnAboutUnboundCommands();
    }

    @Override
    public void onDisable() {
        for (int i = this.lifecycle.size() - 1; i >= 0; i--) {
            this.lifecycle.get(i).disable();
        }
    }
}
