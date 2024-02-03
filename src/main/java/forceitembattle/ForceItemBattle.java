package forceitembattle;

import forceitembattle.commands.*;
import forceitembattle.listener.Listeners;
import forceitembattle.listener.RecipeListener;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Backpack;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.RecipeInventory;
import forceitembattle.util.Timer;
import forceitembattle.util.color.ColorManager;
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

    private Gamemanager gamemanager;
    private Timer timer;
    private Backpack backpack;
    private ItemDifficultiesManager itemDifficultiesManager;
    private RecipeInventory recipeInventory;
    private ColorManager colorManager;
    private Location spawnLocation;

    private GameSettings settings;

    @Override
    public void onLoad() {
        saveConfig();

        getConfig().addDefault("timer.time", 0);
        getConfig().addDefault("settings.isTeamGame", false);
        getConfig().addDefault("settings.keepinventory", false);
        getConfig().addDefault("settings.food", true);
        getConfig().addDefault("settings.backpack", true);
        getConfig().addDefault("settings.pvp", true);
        getConfig().addDefault("settings.nether", true);
        getConfig().addDefault("standard.countdown", 30);
        getConfig().addDefault("standard.jokers", 3);
        getConfig().addDefault("standard.backpackSize", 27);


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
        this.recipeInventory = new RecipeInventory(this);
        this.colorManager = new ColorManager();
        this.settings = new GameSettings(this);

        this.initListeners();
        this.initCommands();

        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRule.KEEP_INVENTORY, getSettings().isKeepInventoryEnabled());
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
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
    }

    private void initCommands() {
        new CommandStart(this);
        new CommandSettings(this);
        new CommandSkip(this);
        new CommandReset(this);
        new CommandBp(this);
        new CommandResult(this);
        new CommandInfo(this);
        new CommandItems(this);
        new CommandStopTimer(this);
        new CommandInfoWiki(this);
        new CommandSpawn(this);
    }

    @Override
    public void onDisable() {
        if (getConfig().getBoolean("isReset")) getConfig().set("timer.time", 0);
        else timer.save();
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

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Gamemanager getGamemanager() {
        return this.gamemanager;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public Backpack getBackpack() {
        return this.backpack;
    }

    public ItemDifficultiesManager getItemDifficultiesManager() {
        return this.itemDifficultiesManager;
    }

    public RecipeInventory getRecipeInventory() {
        return this.recipeInventory;
    }

    public ColorManager getColorManager() {
        return this.colorManager;
    }

    public GameSettings getSettings() {
        return this.settings;
    }

}
