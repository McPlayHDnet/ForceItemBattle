package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.AchievementProgress;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.settings.achievements.PlayerProgress;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.*;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class Gamemanager {

    private final ForceItemBattle forceItemBattle;

    private final Map<UUID, ForceItemPlayer> forceItemPlayerMap;

    public Map<UUID, Map<Integer, Map<Integer, ItemStack>>> savedInventory = new HashMap<>();
    public Map<Team, Map<Integer, Map<Integer, ItemStack>>> savedInventoryTeam = new HashMap<>();

    @Setter
    @Getter
    public GameState currentGameState;
    @Setter
    private GamePreset currentGamePreset;

    @Getter
    private MiniMessage miniMessage;

    @Getter
    @Setter
    private long gameStartTime;

    /**
     * Total game duration (seconds).
     */
    @Getter
    @Setter
    private int gameDuration;

    public Gamemanager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.currentGameState = GameState.PRE_GAME;
        this.currentGamePreset = null;

        this.forceItemPlayerMap = new HashMap<>();

        this.miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color())
                        .resolver(StandardTags.gradient())
                        .resolver(StandardTags.reset())
                        .resolver(StandardTags.newline())
                        .resolver(StandardTags.rainbow())
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.clickEvent())
                        .resolver(StandardTags.hoverEvent())
                        .resolver(StandardTags.translatable())
                        .build()
                )
                .build();
    }

    public void addPlayer(Player player, ForceItemPlayer forceItemPlayer) {
        this.forceItemPlayerMap.put(player.getUniqueId(), forceItemPlayer);
        this.forceItemBattle.getStatsManager().createPlayerStats(forceItemPlayer);
    }

    public void removePlayer(Player player) {
        this.forceItemPlayerMap.remove(player.getUniqueId());
    }

    public Material generateMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().generateRandomMaterial();
    }

    public String getMaterialName(Material material) {
        return WordUtils.capitalizeFully(material.name().replace("_", " "));
    }

    public String formatMaterialName(String material) {
        String materialName = WordUtils.capitalizeFully(material.replace("_", " "));
        String[] wordsToIgnore = {"and", "with", "of", "on", "a", "the"};
        for(String word : wordsToIgnore) {
            materialName = materialName.replace(WordUtils.capitalize(word), word.toLowerCase());
        }
        return materialName.replace(" ", "_");
    }

    public void initializeMats() {
        if(this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            this.forceItemPlayerMap.forEach((uuid, forceItemPlayer) -> {
                if(forceItemPlayer.isSpectator()) return;
                forceItemPlayer.currentTeam().setCurrentScore(0);
                forceItemPlayer.currentTeam().setCurrentMaterial(this.generateMaterial());
                forceItemPlayer.currentTeam().setNextMaterial(this.generateMaterial());
            });
        } else {
            Bukkit.getOnlinePlayers().forEach(player -> {
                ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
                if(forceItemPlayer.isSpectator()) return;
                forceItemPlayer.setCurrentScore(0);
                forceItemPlayer.setCurrentMaterial(this.generateMaterial());
                forceItemPlayer.setNextMaterial(this.generateMaterial());
            });
        }

    }

    public void forceSkipItem(Player player) {
        if (!forceItemPlayerExist(player.getUniqueId())) {
            return;
        }

        ForceItemPlayer gamePlayer = getForceItemPlayer(player.getUniqueId());
        if(this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            gamePlayer.currentTeam().setCurrentMaterial(this.generateMaterial());
        } else {
            gamePlayer.setCurrentMaterial(this.generateMaterial());
        }

        if (!this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.HARD)) {
            gamePlayer.updateItemDisplay();
        }

        this.forceItemBattle.getTimer().sendActionBar();
    }

    public void giveSpectatorItems(Player player) {
        player.getInventory().setItem(1, new ItemBuilder(Material.LIME_DYE).setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(3, new ItemBuilder(Material.COMPASS).setDisplayName("<dark_gray>» <yellow>Teleporter").getItemStack());
        player.getInventory().setItem(5, new ItemBuilder(Material.GRASS_BLOCK).setDisplayName("<dark_gray>» <dark_green>Overworld").getItemStack());
        player.getInventory().setItem(6, new ItemBuilder(Material.NETHERRACK).setDisplayName("<dark_gray>» <red>Nether").getItemStack());
        player.getInventory().setItem(7, new ItemBuilder(Material.ENDER_EYE).setDisplayName("<dark_gray>» <dark_purple>End").getItemStack());
    }

    public void finishGame() {
        this.setCurrentGameState(GameState.END_GAME);

        Bukkit.getOnlinePlayers().forEach(player -> {
            ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
            ForceItemPlayerStats playerStats = this.forceItemBattle.getStatsManager().playerStats(player.getName());

            player.setHealth(20);
            player.setSaturation(20);
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.teleport(Bukkit.getWorld("world").getSpawnLocation());
            player.setGameMode(GameMode.CREATIVE);
            player.getPassengers().forEach(Entity::remove);
            player.setPlayerListName(player.getName());

            this.giveSpectatorItems(player);

            if (player.isOp()) {
                player.sendMessage(ChatColor.RED + "Use /result to see the results from every player");
            }

            if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS)) {
                Map<UUID, ForceItemPlayer> sortedMapDesc = this.sortByValue(this.forceItemPlayerMap(), false);
                Map<ForceItemPlayer, Integer> placesMap = this.calculatePlaces(sortedMapDesc);

                ForceItemPlayerStats forceItemPlayerStats = this.forceItemBattle.getStatsManager().playerStats(forceItemPlayer.player().getName());
                this.forceItemBattle.getStatsManager().addToStats(PlayerStat.TRAVELLED, forceItemPlayerStats, this.forceItemBattle.getStatsManager().calculateDistance(forceItemPlayer.player()));

                if (forceItemPlayerStats.highestScore() < forceItemPlayer.currentScore()) {
                    this.forceItemBattle.getStatsManager().addToStats(PlayerStat.HIGHEST_SCORE, forceItemPlayerStats, forceItemPlayer.currentScore());
                }

                if (placesMap.get(forceItemPlayer) == 1) {
                    this.forceItemBattle.getStatsManager().addToStats(PlayerStat.GAMES_WON, forceItemPlayerStats, 1);

                    this.forceItemBattle.getStatsManager().addToStats(PlayerStat.WIN_STREAK, forceItemPlayerStats, forceItemPlayerStats.winStreak() + 1);
                } else {
                    if(forceItemPlayerStats.winStreak() != 0) {
                        this.forceItemBattle.getStatsManager().addToStats(PlayerStat.WIN_STREAK, forceItemPlayerStats, 0);
                    }
                }
            }

            Achievements achievement = Achievements.CHICOT;
            if(playerStats.achievementsDone().contains(achievement.getTitle())) {
                return;
            }
            PlayerProgress playerProgress = this.forceItemBattle.getAchievementManager().playerProgressMap.get(player.getUniqueId());
            if(playerProgress.getProgress(achievement).getDeathCounter() == achievement.getCondition().getAmount()) {
                PlayerGrantAchievementEvent grantAchievementEvent = new PlayerGrantAchievementEvent(player, achievement);
                Bukkit.getPluginManager().callEvent(grantAchievementEvent);
            }
        });
    }

    public Map<UUID, ForceItemPlayer> sortByValue(Map<UUID, ForceItemPlayer> unsortMap, final boolean order) {
        List<Map.Entry<UUID, ForceItemPlayer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().currentScore().compareTo(o2.getValue().currentScore()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().currentScore().compareTo(o2.getValue().currentScore()) : o2.getValue().currentScore().compareTo(o1.getValue().currentScore()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().currentScore().compareTo(o1.getValue().currentScore()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    public Map<ForceItemPlayer, Integer> calculatePlaces(Map<UUID, ForceItemPlayer> playerMap) {
        List<ForceItemPlayer> sortedPlayers = playerMap.values().stream()
                .sorted(Comparator.comparingInt(ForceItemPlayer::currentScore).reversed())
                .toList();

        Map<ForceItemPlayer, Integer> placesMap = new LinkedHashMap<>();

        int place = 1;
        for(int i = 0; i < sortedPlayers.size(); i++) {
            ForceItemPlayer currentPlayer = sortedPlayers.get(i);

            if(i > 0 && currentPlayer.currentScore() == sortedPlayers.get(i - 1).currentScore()) {
                placesMap.put(currentPlayer, placesMap.get(sortedPlayers.get(i - 1)));
            } else {

                placesMap.put(currentPlayer, place);
                place++;
            }
        }
        return placesMap;
    }

    public Map<Team, Integer> calculatePlaces(List<Team> teams) {
        List<Team> sortedTeams = teams.stream()
                .sorted(Comparator.comparingInt(Team::getCurrentScore).reversed())
                .toList();

        Map<Team, Integer> placesMap = new LinkedHashMap<>();

        int place = 1;
        for(int i = 0; i < sortedTeams.size(); i++) {
            Team currentTeam = sortedTeams.get(i);

            if(i > 0 && currentTeam.getCurrentScore() == sortedTeams.get(i - 1).getCurrentScore()) {
                placesMap.put(currentTeam, placesMap.get(sortedTeams.get(i - 1)));
            } else {

                placesMap.put(currentTeam, place);
                place++;
            }
        }
        return placesMap;
    }

    public String colorCodeToName(String colorCode) {
        String name = "";

        switch(colorCode) {
            case "&0" -> name = "<black>";
            case "&1" -> name = "<dark_blue>";
            case "&2" -> name = "<dark_green>";
            case "&3" -> name = "<dark_aqua>";
            case "&4" -> name = "<dark_red>";
            case "&5" -> name = "<dark_purple>";
            case "&6" -> name = "<gold>";
            case "&7" -> name = "<gray>";
            case "&8" -> name = "<dark_gray>";
            case "&9" -> name = "<blue>";
            case "&a" -> name = "<green>";
            case "&b" -> name = "<aqua>";
            case "&c" -> name = "<red>";
            case "&d" -> name = "<pink>";
            case "&e" -> name = "<yellow>";
            case "&f" -> name = "<white>";
            default -> name = colorCode;
        }

        return name;
    }

    public boolean forceItemPlayerExist(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid) != null;
    }

    public ForceItemPlayer getForceItemPlayer(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid);
    }

    public Map<UUID, ForceItemPlayer> forceItemPlayerMap() {
        return this.forceItemPlayerMap;
    }

    public boolean isPreGame() {
        return this.getCurrentGameState() == GameState.PRE_GAME;
    }

    public boolean isPausedGame() {
        return this.getCurrentGameState() == GameState.PAUSED_GAME;
    }

    public boolean isMidGame() {
        return this.getCurrentGameState() == GameState.MID_GAME;
    }

    public boolean isEndGame() {
        return this.getCurrentGameState() == GameState.END_GAME;
    }

    public GamePreset currentGamePreset() {
        return currentGamePreset;
    }

    private static final Material JOKER_MATERIAL = Material.BARRIER;

    public static Material getJokerMaterial() {
        return JOKER_MATERIAL;
    }

    public static ItemStack getJokers(int amount) {
        return new ItemBuilder(JOKER_MATERIAL)
                .setAmount(amount)
                .setDisplayName("<dark_gray>» <dark_purple>Joker")
                .getItemStack();
    }

    public static boolean isJoker(Material material) {
        return material == JOKER_MATERIAL;
    }

    public static boolean isJoker(ItemStack itemStack) {
        return isJoker(itemStack.getType());
    }

    public static boolean isBackpack(Material material) {
        return material == Material.BUNDLE;
    }

    public static boolean isBackpack(ItemStack itemStack) {
        return isBackpack(itemStack.getType());
    }
}
