package forceitembattle;

import forceitembattle.commands.CommandsManager;
import forceitembattle.commands.admin.*;
import forceitembattle.commands.player.*;
import forceitembattle.commands.player.trade.CommandAskTrade;
import forceitembattle.commands.player.trade.CommandTrade;
import forceitembattle.listener.*;
import forceitembattle.manager.*;
import forceitembattle.manager.stats.StatsManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ForceItemBattle extends JavaPlugin {

    @Getter
    private static ForceItemBattle instance;

    @Getter
    private Gamemanager gamemanager;
    @Getter
    private Timer timer;
    @Getter
    private Backpack backpack;
    @Getter
    private ItemDifficultiesManager itemDifficultiesManager;
    @Getter
    private RecipeManager recipeManager;
    @Getter
    private StatsManager statsManager;
    @Getter
    private PositionManager positionManager;
    @Getter
    private WanderingTraderTimer wanderingTraderTimer;
    @Getter
    @Setter
    private CommandsManager commandsManager;
    @Getter
    @Setter
    private TeamsManager teamManager;
    @Getter
    @Setter
    private TradingManager tradingManager;
    @Getter
    private AntimatterLocator antimatterLocator;
    @Getter
    private AchievementManager achievementManager;
    @Getter
    private AchievementListener achievementListener;
    @Getter
    private LocatorManager locatorManager;
    @Getter
    private ProtectionManager protectionManager;
    @Getter
    @Setter
    private Location spawnLocation;

    @Getter
    private GameSettings settings;

    public final File resetFile = new File(this.getDataFolder() + "/reset");

    public ForceItemBattle() {
        instance = this;
    }

    @Override
    public void onLoad() {
        ConsoleLogger.startCapturing();
        saveConfig();

        this.settings = new GameSettings(this);

        saveConfig();
        if (resetFile.exists()) {
            this.resetWorld();
            resetFile.delete();
        }
    }

    @Override
    public void onEnable() {
        this.gamemanager = new Gamemanager(this);
        this.timer = new Timer(this);
        this.backpack = new Backpack(this);
        this.itemDifficultiesManager = new ItemDifficultiesManager(this);
        this.recipeManager = new RecipeManager(this);
        this.statsManager = new StatsManager();
        this.positionManager = new PositionManager(this);
        this.teamManager = new TeamsManager(this);
        this.tradingManager = new TradingManager(this);
        this.commandsManager = new CommandsManager(this);
        this.achievementManager = new AchievementManager(this);
        this.achievementListener = new AchievementListener(this);
        this.locatorManager = new LocatorManager();
        this.protectionManager = new ProtectionManager(this);
        this.wanderingTraderTimer = new WanderingTraderTimer();
        this.antimatterLocator = new AntimatterLocator();

        this.initListeners();
        this.initCommands();

        //this 0 delay scheduler is needed in paper, because at that time this code gets initialized and the worlds after that, so we'll wait
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.getWorlds().forEach(world -> {
            boolean keepInventory = getSettings().isSettingEnabled(GameSetting.KEEP_INVENTORY);
            if (getSettings().isSettingEnabled(GameSetting.EVENT) && !keepInventory) {

                keepInventory = true;
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this,
                        () -> world.setGameRule(GameRule.KEEP_INVENTORY, false),
                        20 * 60 * 5 // 5 minutes
                );
            }

            // Apply settings.
            world.setGameRule(GameRule.KEEP_INVENTORY, keepInventory);
            getSettings().setSettingEnabled(GameSetting.FASTER_RANDOM_TICK, getSettings().isSettingEnabled(GameSetting.FASTER_RANDOM_TICK));

            //world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
            world.setGameRule(GameRule.DO_INSOMNIA, false);
        }), 0L);

        if(this.getConfig().isConfigurationSection("descriptions")) {
            ConfigurationSection configurationSection = this.getConfig().getConfigurationSection("descriptions");
            Set<String> materialKeys;
            if (configurationSection != null) {
                materialKeys = configurationSection.getKeys(false);

                materialKeys.forEach(keys -> {
                    List<String> descriptions = configurationSection.getStringList(keys);
                    keys = keys.toUpperCase();
                    getItemDifficultiesManager().getDescriptionItems().put(Material.valueOf(keys), new DescriptionItem(Material.valueOf(keys), descriptions));
                });
            } else {
                throw new NullPointerException("'descriptions' does not exist in the config.yml");
            }
        }
    }

    private void copyDatapack(String datapackName) {
        File world = new File(Bukkit.getWorldContainer() , "world");
        File nether = new File(Bukkit.getWorldContainer() , "world_nether");
        File end = new File(Bukkit.getWorldContainer() , "world_the_end");

        try {
            // Create Path objects for source and destination directories
            Path sourceDirectory = Paths.get(this.getDataFolder() + "/" + datapackName + ".zip");
            Path destinationDirectory = Paths.get(world + "/datapacks/" + datapackName + ".zip");

            // Copy the directory and its contents recursively
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

    private void resetWorld() {
        try {
            //////////////////////////////////////////////////////////////////////////////
            //Files.deleteIfExists(getDataFolder().toPath());
            //////////////////////////////////////////////////////////////////////////////

            File world = new File(Bukkit.getWorldContainer() , "world");
            File nether = new File(Bukkit.getWorldContainer() , "world_nether");
            File end = new File(Bukkit.getWorldContainer() , "world_the_end");

            Files.walk(world.toPath())
                    .sorted(Comparator.reverseOrder())
                    . map(Path::toFile)
                    . forEach(File::delete);
            Files.walk(nether.toPath())
                    .sorted(Comparator.reverseOrder())
                    . map(Path::toFile)
                    . forEach(File::delete);
            Files.walk(end.toPath())
                    .sorted(Comparator.reverseOrder())
                    . map(Path::toFile)
                    . forEach(File::delete);

            //////////////////////////////////////////////////////////////////////////////

            world.mkdirs();
            nether.mkdirs();
            end.mkdirs();

            new File(world , "data").mkdirs();
            new File(world , "datapacks").mkdirs();
            new File(world , "playerdata").mkdirs();
            new File(world , "poi").mkdirs();
            new File(world , "region").mkdirs();

            new File(nether , "data").mkdirs();
            new File(nether , "datapacks").mkdirs();
            new File(nether , "playerdata").mkdirs();
            new File(nether , "poi").mkdirs();
            new File(nether , "region").mkdirs();

            new File(end , "data").mkdirs();
            new File(end , "datapacks").mkdirs();
            new File(end , "playerdata").mkdirs();
            new File(end , "poi").mkdirs();
            new File(end , "region").mkdirs();

            this.copyDatapack("FIB_Worldgen");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initListeners() {
        registerListeners(
                new Listeners(this),
                new SettingsListener(this),
                new RecipeListener(this),
                new PvPListener(this),
                new ProtectionListener(this),
                new ClickableItemsListener(this),
                new ItemsListener(this),
                new PortalListener(this),
                new AchievementListener(this)
        );

    }

    public void initRecipes() {
        final boolean easyRecipes = !this.settings.isSettingEnabled(GameSetting.HARDER_TRACKERS);

        NamespacedKey antimatterKey = new NamespacedKey("fib", "antimatter_locator");
        ShapedRecipe antimatterRecipe = new ShapedRecipe(antimatterKey, new ItemBuilder(Material.KNOWLEDGE_BOOK).setDisplayName("<dark_gray>» <dark_purple>Antimatter Locator").getItemStack());
        if (easyRecipes) {
            antimatterRecipe.shape(
                    " N ",
                    "GQG",
                    " N "
            );
            antimatterRecipe.setIngredient('N', Material.NETHER_BRICK);
            antimatterRecipe.setIngredient('G', Material.GLOWSTONE_DUST);
            antimatterRecipe.setIngredient('Q', Material.QUARTZ);
        } else {
            antimatterRecipe.shape(
                    "BGB",
                    "QEQ",
                    "BGB"
            );
            antimatterRecipe.setIngredient('B', Material.NETHER_BRICK);
            antimatterRecipe.setIngredient('E', Material.ENDER_EYE);
            antimatterRecipe.setIngredient('G', Material.GLOWSTONE_DUST);
            antimatterRecipe.setIngredient('Q', Material.QUARTZ);
        }

        NamespacedKey chambersKey = new NamespacedKey("fib", "chambers_locator");
        ShapedRecipe chambersRecipe = new ShapedRecipe(chambersKey, new ItemBuilder(Material.WITHER_ROSE).setDisplayName("<dark_gray>» <gold>Trial Locator").getItemStack());
        if (easyRecipes) {
            chambersRecipe.shape(
                    "BGB",
                    "GCG",
                    "AAA"
            );
            chambersRecipe.setIngredient('B', Material.CUT_COPPER);
            chambersRecipe.setIngredient('G', Material.GLASS);
            chambersRecipe.setIngredient('C', Material.COMPASS);
            chambersRecipe.setIngredient('A', Material.GOLD_INGOT);
        } else {
            chambersRecipe.shape(
                    "OKO",
                    "GCI",
                    "ODO"
            );
            chambersRecipe.setIngredient('O', Material.OBSIDIAN);
            chambersRecipe.setIngredient('C', Material.COMPASS);
            chambersRecipe.setIngredient('K', Material.COPPER_INGOT);
            chambersRecipe.setIngredient('I', Material.IRON_INGOT);
            chambersRecipe.setIngredient('G', Material.GOLD_INGOT);
            chambersRecipe.setIngredient('D', Material.DIAMOND);
        }

        Bukkit.addRecipe(antimatterRecipe);
        Bukkit.addRecipe(chambersRecipe);
    }

    public void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void initCommands() {
        new CommandStart();
        new CommandSettings();
        new CommandSkip();
        new CommandReset();
        new CommandBp();
        new CommandResult();
        new CommandInfo();
        new CommandItems();
        new CommandStopTimer();
        new CommandInfoWiki();
        new CommandSpawn();
        new CommandBed();
        new CommandPause();
        new CommandResume();
        new CommandStats();
        new CommandLeaderboard();
        new CommandPosition();
        new CommandPing();
        new CommandHelp();
        new CommandTeams();
        new CommandAskTrade();
        new CommandTrade();
        new CommandFixSkips();
        new CommandAchievement();
        new CommandSpectate();
        new CommandShout();
        new CommandForceTeam();
        new CommandLogConsole();
    }

    @Override
    public void onDisable() {
        reloadConfig();
        if (getConfig().getBoolean("isReset")) {
            getConfig().set("timer.time", 0);
        } else {
            timer.save();
        }
        saveConfig();
    }

    public void logToFile(String message) {
        try {
            File dataFolder = getDataFolder();
            if(!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File saveTo = new File(getDataFolder(), "logs_plugin.txt");
            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("[" + getTime() + "] | " + message);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTime(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(cal.getTime());
    }

}
