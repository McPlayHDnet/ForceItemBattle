package forceitembattle.settings.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class AchievementStorage {

    private final ForceItemBattle plugin;
    private final File achievementsFile;
    private final Gson gson;
    private Map<UUID, Set<String>> playerAchievements;

    public AchievementStorage(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.achievementsFile = new File(plugin.getDataFolder(), "achievements.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerAchievements = new HashMap<>();
        this.loadAchievements();
    }

    public void loadAchievements() {
        if (!achievementsFile.exists()) {
            try {
                achievementsFile.getParentFile().mkdirs();
                achievementsFile.createNewFile();
                this.playerAchievements = new HashMap<>();
                this.saveAchievements();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create achievements.json file!");
                e.printStackTrace();
            }
            return;
        }

        try (Reader reader = Files.newBufferedReader(achievementsFile.toPath())) {
            Type type = new TypeToken<Map<UUID, Set<String>>>(){}.getType();
            Map<UUID, Set<String>> loaded = gson.fromJson(reader, type);
            this.playerAchievements = loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load achievements.json file!");
            e.printStackTrace();
            this.playerAchievements = new HashMap<>();
        }
    }

    public void saveAchievements() {
        try (Writer writer = Files.newBufferedWriter(achievementsFile.toPath())) {
            gson.toJson(this.playerAchievements, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save achievements.json file!");
            e.printStackTrace();
        }
    }

    public void saveAchievementsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAchievements);
    }

    public void addAchievement(UUID playerUUID, Achievements achievement) {
        this.playerAchievements.putIfAbsent(playerUUID, new HashSet<>());
        this.playerAchievements.get(playerUUID).add(achievement.name());
        this.saveAchievementsAsync();
    }

    public void removeAchievement(UUID playerUUID, Achievements achievement) {
        if (this.playerAchievements.containsKey(playerUUID)) {
            this.playerAchievements.get(playerUUID).remove(achievement.name());
            this.saveAchievementsAsync();
        }
    }

    public boolean hasAchievement(UUID playerUUID, Achievements achievement) {
        return this.playerAchievements.containsKey(playerUUID) &&
                this.playerAchievements.get(playerUUID).contains(achievement.name());
    }

    public Set<String> getPlayerAchievements(UUID playerUUID) {
        return this.playerAchievements.getOrDefault(playerUUID, new HashSet<>());
    }

    public void resetPlayerAchievements(UUID playerUUID) {
        this.playerAchievements.remove(playerUUID);
        this.saveAchievementsAsync();
    }

    public void resetAllAchievements() {
        this.playerAchievements.clear();
        this.saveAchievementsAsync();
    }

    public Map<UUID, Set<String>> getAllAchievements() {
        return new HashMap<>(this.playerAchievements);
    }
}