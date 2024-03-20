package forceitembattle;

import forceitembattle.commands.CommandsManager;
import forceitembattle.commands.admin.*;
import forceitembattle.commands.player.*;
import forceitembattle.listener.Listeners;
import forceitembattle.listener.PvPListener;
import forceitembattle.listener.RecipeListener;
import forceitembattle.manager.*;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Backpack;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.Timer;
import forceitembattle.util.color.ColorManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private ColorManager colorManager;
    @Getter
    private StatsManager statsManager;
    @Getter
    private PositionManager positionManager;
    @Getter
    @Setter
    private CommandsManager commandsManager;
    @Getter
    @Setter
    private Location spawnLocation;

    @Getter
    private GameSettings settings;

    public ForceItemBattle() {
        instance = this;
    }

    @Override
    public void onLoad() {
        saveConfig();

        this.settings = new GameSettings(this);

        saveConfig();
        if (!getConfig().contains("isReset")){
            getConfig().set("isReset" , false);
            saveConfig();
            return;
        }
        if (getConfig().getBoolean("isReset")){

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
            } catch (IOException e) {
                e.printStackTrace();
            }

            getConfig().set("isReset" , false);
            saveConfig();

        }
    }

    @Override
    public void onEnable() {
        this.gamemanager = new Gamemanager(this);
        this.timer = new Timer(this);
        this.backpack = new Backpack(this);
        this.itemDifficultiesManager = new ItemDifficultiesManager(this);
        //testing something, not needed in final code
        //this.itemDifficultiesManager.createList();
        this.recipeManager = new RecipeManager(this);
        this.colorManager = new ColorManager();
        this.statsManager = new StatsManager(this);
        this.positionManager = new PositionManager(this);

        this.initListeners();
        this.initCommands();

        Bukkit.getWorlds().forEach(world -> {
            // Apply settings.
            world.setGameRule(GameRule.KEEP_INVENTORY, getSettings().isSettingEnabled(GameSetting.KEEP_INVENTORY));
            getSettings().setSettingEnabled(GameSetting.FASTER_RANDOM_TICK, getSettings().isSettingEnabled(GameSetting.FASTER_RANDOM_TICK));

            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        });

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

    private void initListeners() {
        new Listeners(this);
        new RecipeListener(this);
        new PvPListener(this);
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
        new CommandShow();
        new CommandHelp();
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
