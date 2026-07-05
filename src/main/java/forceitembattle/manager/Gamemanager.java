package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.stats.FIBServiceHelper;
import forceitembattle.util.*;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static forceitembattle.util.RecipeInventory.CUSTOM_MATERIALS;

public class Gamemanager implements Manager {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey("fib", "backpack");
    private static final Material JOKER_MATERIAL = Material.BARRIER;
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
    }

    private static <T> Map<T, Integer> calculatePlaces(List<T> entities, ToIntFunction<T> score) {
        List<T> sorted = entities.stream()
                .sorted(Comparator.comparingInt(score).reversed())
                .toList();

        Map<T, Integer> placesMap = new LinkedHashMap<>();

        int place = 0;
        Integer previousScore = null;
        for (T entity : sorted) {
            int currentScore = score.applyAsInt(entity);
            if (previousScore == null || currentScore != previousScore) {
                place++;
            }
            placesMap.put(entity, place);
            previousScore = currentScore;
        }
        return placesMap;
    }

    public static Material getJokerMaterial() {
        return JOKER_MATERIAL;
    }

    public static ItemStack getJokers(int amount) {
        return new ItemBuilder(JOKER_MATERIAL)
                .setAmount(amount)
                .setDisplayName("<dark_gray>» <dark_purple>Joker")
                .getItemStack();
    }

    public static ItemStack createBackpack(ForceItemPlayer forceItemPlayer, boolean isTeamMode) {
        Material bundle = Material.BUNDLE;
        if (isTeamMode) {
            bundle = Material.getMaterial(forceItemPlayer.currentTeam().getColor().name() + "_BUNDLE");
        }

        ItemStack itemStack = new ItemBuilder(bundle)
                .setDisplayName("<dark_gray>» <yellow>Backpack")
                .getItemStack();

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(BACKPACK_KEY, PersistentDataType.BOOLEAN, Boolean.TRUE);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private static boolean isJoker(Material material) {
        // TODO change to also use NBT maybe
        return material == JOKER_MATERIAL;
    }

    public static boolean isJoker(ItemStack itemStack) {
        return isJoker(itemStack.getType());
    }

    public static boolean isBackpack(ItemStack itemStack) {
        if (!itemStack.getType().name().contains("BUNDLE")) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!itemMeta.getPersistentDataContainer().has(BACKPACK_KEY)) {
            return false;
        }

        return Boolean.TRUE.equals(itemStack.getItemMeta().getPersistentDataContainer().get(BACKPACK_KEY, PersistentDataType.BOOLEAN));
    }

    public MiniMessage getMiniMessage() {
        return Text.mm();
    }

    public void addPlayer(Player player, ForceItemPlayer forceItemPlayer) {
        this.forceItemPlayerMap.put(player.getUniqueId(), forceItemPlayer);
    }

    public void removePlayer(Player player) {
        this.forceItemPlayerMap.remove(player.getUniqueId());
    }

    public Material generateMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().generateRandomMaterial();
    }

    public Material generateSeededMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().generateSeededRandomMaterial();
    }

    public String getMaterialName(Material material) {
        CustomMaterial customMaterial = CUSTOM_MATERIALS.get(material);
        if (customMaterial != null) {
            return customMaterial.containerName();
        }
        return WordUtils.capitalizeFully(material.name().replace("_", " "));
    }

    public String formatMaterialName(String material) {
        String materialName = WordUtils.capitalizeFully(material.replace("_", " "));
        String[] wordsToIgnore = {"and", "with", "of", "on", "a", "the"};
        for (String word : wordsToIgnore) {
            materialName = materialName.replace(WordUtils.capitalize(word), word.toLowerCase());
        }
        return materialName.replace(" ", "_");
    }

    public void initializeMats() {
        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);
        boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);
        long now = System.currentTimeMillis();

        Material globalCurrent = null;
        Material globalNext = null;

        if (runMode) {
            globalCurrent = this.generateSeededMaterial();
            globalNext = this.generateSeededMaterial();
        }

        if (teamMode) {
            Material finalGlobalCurrent = globalCurrent;
            Material finalGlobalNext = globalNext;
            this.forceItemPlayerMap.forEach((uuid, forceItemPlayer) -> {
                if (forceItemPlayer.isSpectator()) return;

                Material current = runMode ? finalGlobalCurrent : this.generateMaterial();
                Material next = runMode ? finalGlobalNext : this.generateMaterial();

                forceItemPlayer.currentTeam().setCurrentScore(0);
                forceItemPlayer.currentTeam().setCurrentMaterial(current);
                forceItemPlayer.currentTeam().setNextMaterial(next);
                forceItemPlayer.currentTeam().setLastItemAssignedAt(now);
            });
        } else {
            Material finalGlobalCurrent1 = globalCurrent;
            Material finalGlobalNext1 = globalNext;
            Bukkit.getOnlinePlayers().forEach(player -> {
                ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
                if (forceItemPlayer.isSpectator()) return;

                Material current = runMode ? finalGlobalCurrent1 : this.generateMaterial();
                Material next = runMode ? finalGlobalNext1 : this.generateMaterial();

                forceItemPlayer.setCurrentScore(0);
                forceItemPlayer.setCurrentMaterial(current);
                forceItemPlayer.setNextMaterial(next);
                forceItemPlayer.setLastItemAssignedAt(now);
            });
        }

    }

    public void forceSkipItem(Player player, boolean adminCommand) {
        if (!forceItemPlayerExist(player.getUniqueId())) {
            return;
        }

        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);
        boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);

        Material currentMaterial = runMode ? this.generateSeededMaterial() : this.generateMaterial();
        Material nextMaterial = runMode ? this.generateSeededMaterial() : this.generateMaterial();

        ForceItemPlayer gamePlayer = getForceItemPlayer(player.getUniqueId());
        if (teamMode) {
            forceItemPlayerMap().values().forEach(p -> {
                if (!adminCommand)
                    gamePlayer.currentTeam().setRemainingJokers(gamePlayer.currentTeam().getRemainingJokers() - 1);
                p.currentTeam().setCurrentMaterial(currentMaterial);
                p.currentTeam().setNextMaterial(nextMaterial);
            });
        } else {
            forceItemPlayerMap().values().forEach(p -> {
                if (!adminCommand) gamePlayer.setRemainingJokers(gamePlayer.remainingJokers() - 1);
                p.setCurrentMaterial(currentMaterial);
                p.setNextMaterial(nextMaterial);
            });
        }

    }

    public void giveSpectatorItems(Player player) {
        player.getInventory().setItem(1, new ItemBuilder(Material.LIME_DYE).setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(3, new ItemBuilder(Material.COMPASS).setDisplayName("<dark_gray>» <yellow>Teleporter").getItemStack());
        player.getInventory().setItem(5, new ItemBuilder(Material.GRASS_BLOCK).setDisplayName("<dark_gray>» <dark_green>Overworld").getItemStack());
        player.getInventory().setItem(6, new ItemBuilder(Material.NETHERRACK).setDisplayName("<dark_gray>» <red>Nether").getItemStack());
        player.getInventory().setItem(7, new ItemBuilder(Material.ENDER_EYE).setDisplayName("<dark_gray>» <dark_purple>End").getItemStack());
        player.getInventory().setItem(8, new ItemBuilder(Material.SPYGLASS).setDisplayName("<dark_gray>» <green>Spectate").getItemStack());
    }

    public void finishGame() {
        this.setCurrentGameState(GameState.END_GAME);
        this.forceItemBattle.getAchievementManager().checkGameEndAchievements();

        boolean statsEnabled = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS);
        Map<ForceItemPlayer, Integer> placesMap = statsEnabled
                ? this.calculatePlaces(this.sortByValue(this.forceItemPlayerMap(), false))
                : null;

        Bukkit.getOnlinePlayers().forEach(player -> {
            ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
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

            if (statsEnabled) {
                FIBServiceHelper helper = this.forceItemBattle.getFibServiceHelper();

                var soloUpdate = FIBServiceHelper.soloUpdate()
                        .blocksTravelledAdd((long) this.calculateDistance(forceItemPlayer.player()))
                        .highestScore((long) forceItemPlayer.currentScore());

                if (placesMap.get(forceItemPlayer) == 1) {
                    soloUpdate.gamesWonAdd(1);
                }

                helper.updateSoloStatisticsAsync(player.getUniqueId(), soloUpdate);

                if (forceItemPlayer.currentTeam() != null) {
                    Team currentTeam = forceItemPlayer.currentTeam();
                    for (ForceItemPlayer teamPlayer : currentTeam.getPlayers()) {
                        if (!teamPlayer.equals(forceItemPlayer)) {
                            helper.updateTeamStatisticsAsync(
                                    player.getUniqueId(),
                                    teamPlayer.player().getUniqueId(),
                                    FIBServiceHelper.teamUpdate().highestScore((long) forceItemPlayer.currentScore())
                            );
                            break;
                        }
                    }
                }
            }
        });
    }

    public String placeColor(int place) {
        String placeColor;
        switch (place) {
            case 3 -> placeColor = "<red>";
            case 2 -> placeColor = "<gray>";
            case 1 -> placeColor = "<gold>";
            default -> placeColor = "<white>";
        }
        return placeColor;
    }

    public int calculateDistance(Player player) {
        int distance = 0;

        for (Statistic statistics : Statistic.values()) {
            //check and get every statistic that has CM (distance based)
            if (statistics.name().contains("CM")) {
                distance += player.getStatistic(statistics);
            }
        }

        return (int) Math.round((double) distance / 100);
    }

    public Map<UUID, ForceItemPlayer> sortByValue(Map<UUID, ForceItemPlayer> unsortMap, final boolean order) {
        Comparator<Map.Entry<UUID, ForceItemPlayer>> comparator =
                Comparator.comparingInt((Map.Entry<UUID, ForceItemPlayer> e) -> e.getValue().currentScore())
                        .thenComparing(Map.Entry::getKey);
        if (!order) {
            comparator = comparator.reversed();
        }
        return unsortMap.entrySet().stream()
                .sorted(comparator)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public Map<ForceItemPlayer, Integer> calculatePlaces(Map<UUID, ForceItemPlayer> playerMap) {
        return calculatePlaces(new ArrayList<>(playerMap.values()), ForceItemPlayer::currentScore);
    }

    public Map<Team, Integer> calculatePlaces(List<Team> teams) {
        return calculatePlaces(teams, Team::getCurrentScore);
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
}