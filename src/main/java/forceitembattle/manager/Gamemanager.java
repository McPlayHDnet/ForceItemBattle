package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GamePreset;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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

    /**
     * True from the moment {@code /start} begins its countdown until the game
     * actually flips to MID_GAME. During this window teams and force items have
     * already been assigned, so anyone joining must be treated as a spectator
     * rather than a half-initialized participant.
     */
    @Getter
    @Setter
    private boolean starting;

    /**
     * Dev/testing override queue. When non-empty, {@link #generateMaterial()} and
     * {@link #generateSeededMaterial()} return the queued materials in order before
     * falling back to random generation. Populated by the /forceitem command and
     * cleared at the start of every game.
     */
    @Getter
    private final Deque<Material> forcedItemQueue = new ArrayDeque<>();

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
        Material forced = this.forcedItemQueue.poll();
        if (forced != null) {
            return forced;
        }
        return this.forceItemBattle.getItemDifficultiesManager().generateRandomMaterial();
    }

    public Material generateSeededMaterial() {
        Material forced = this.forcedItemQueue.poll();
        if (forced != null) {
            return forced;
        }
        return this.forceItemBattle.getItemDifficultiesManager().generateSeededRandomMaterial();
    }

    private MaterialPair nextMaterials(boolean runMode) {
        if (runMode) {
            return new MaterialPair(this.generateSeededMaterial(), this.generateSeededMaterial());
        }
        return new MaterialPair(this.generateMaterial(), this.generateMaterial());
    }

    private record MaterialPair(Material current, Material next) {
    }

    public String getMaterialName(Material material) {
        return CustomMaterials.nameOf(material);
    }

    public String formatMaterialName(String material) {
        String materialName = WordUtils.capitalizeFully(material.replace("_", " "));
        String[] wordsToIgnore = {"and", "with", "of", "on", "a", "the"};
        for (String word : wordsToIgnore) {
            materialName = materialName.replace(WordUtils.capitalize(word), word.toLowerCase());
        }
        return materialName.replace(" ", "_");
    }

    public void initializeMaterials() {
        this.forcedItemQueue.clear();
        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);
        boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);
        long now = System.currentTimeMillis();

        // In run mode everyone shares one seeded pair; otherwise each player gets their own.
        MaterialPair shared = runMode ? this.nextMaterials(true) : null;

        if (teamMode) {
            this.forceItemPlayerMap.forEach((uuid, forceItemPlayer) -> {
                if (forceItemPlayer.isSpectator()) return;

                MaterialPair pair = runMode ? shared : this.nextMaterials(false);

                forceItemPlayer.currentTeam().setCurrentScore(0);
                forceItemPlayer.currentTeam().setCurrentMaterial(pair.current());
                forceItemPlayer.currentTeam().setNextMaterial(pair.next());
                forceItemPlayer.currentTeam().setLastItemAssignedAt(now);
            });
        } else {
            Bukkit.getOnlinePlayers().forEach(player -> {
                ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
                if (forceItemPlayer.isSpectator()) return;

                MaterialPair pair = runMode ? shared : this.nextMaterials(false);

                forceItemPlayer.setCurrentScore(0);
                forceItemPlayer.setCurrentMaterial(pair.current());
                forceItemPlayer.setNextMaterial(pair.next());
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

        MaterialPair pair = this.nextMaterials(runMode);

        ForceItemPlayer gamePlayer = getForceItemPlayer(player.getUniqueId());
        if (teamMode) {
            forceItemPlayerMap().values().forEach(p -> {
                if (!adminCommand)
                    gamePlayer.currentTeam().setRemainingJokers(gamePlayer.currentTeam().getRemainingJokers() - 1);
                p.currentTeam().setCurrentMaterial(pair.current());
                p.currentTeam().setNextMaterial(pair.next());
            });
        } else {
            forceItemPlayerMap().values().forEach(p -> {
                if (!adminCommand) gamePlayer.setRemainingJokers(gamePlayer.remainingJokers() - 1);
                p.setCurrentMaterial(pair.current());
                p.setNextMaterial(pair.next());
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
        Map<Team, Integer> teamPlaces = (statsEnabled
                && this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM))
                ? this.calculatePlaces(this.forceItemBattle.getTeamManager().getTeams())
                : null;

        Bukkit.getOnlinePlayers().forEach(player -> {
            try {
                ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
                player.setHealth(20);
                player.setSaturation(20);
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.teleport(Dimension.OVERWORLD.world().getSpawnLocation());
                player.setGameMode(GameMode.CREATIVE);
                player.getPassengers().forEach(Entity::remove);
                player.setPlayerListName(player.getName());

                this.giveSpectatorItems(player);

                if (player.isOp()) {
                    player.sendMessage(ChatColor.RED + "Use /result to see the results from every player");
                }
                
                if (statsEnabled && forceItemPlayer != null && !forceItemPlayer.isSpectator()) {
                    FibStatisticsClient helper = this.forceItemBattle.getFibService().statistics();
                    long distance = (long) this.calculateDistance(forceItemPlayer.player());

                    if (forceItemPlayer.currentTeam() == null) {
                        // ---- Solo game: everything on solo stats ----
                        var soloUpdate = FIBServiceClient.soloUpdate()
                                .blocksTravelledAdd(distance)
                                .highestScore((long) forceItemPlayer.currentScore());

                        if (placesMap.get(forceItemPlayer) == 1) {
                            soloUpdate.gamesWonAdd(1);
                        }

                        helper.updateSoloStatisticsAsync(player.getUniqueId(), soloUpdate);
                    } else {
                        // ---- Team game: keep everything on team/member stats, never solo ----
                        Team currentTeam = forceItemPlayer.currentTeam();
                        boolean teamWon = teamPlaces != null && Integer.valueOf(1).equals(teamPlaces.get(currentTeam));

                        for (ForceItemPlayer teamPlayer : currentTeam.getPlayers()) {
                            if (!teamPlayer.equals(forceItemPlayer)) {
                                UUID teammateUuid = teamPlayer.player().getUniqueId();

                                // This player's own travel → their own member contribution.
                                helper.updateMemberStatisticsAsync(
                                        player.getUniqueId(),
                                        teammateUuid,
                                        player.getUniqueId(),
                                        FIBServiceClient.memberUpdate().blocksTravelledAdd(distance)
                                );

                                // Shared team stats. highestScore is a max-set (safe from both
                                // sides); gamesWon must count once, so only the lower-UUID side sends.
                                var teamUpdate = FIBServiceClient.teamUpdate()
                                        .highestScore((long) currentTeam.getCurrentScore());
                                boolean lowerSide = player.getUniqueId().toString()
                                        .compareTo(teammateUuid.toString()) < 0;
                                if (lowerSide && teamWon) {
                                    teamUpdate.gamesWonAdd(1);
                                }

                                helper.updateTeamStatisticsAsync(player.getUniqueId(), teammateUuid, teamUpdate);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                this.forceItemBattle.getLogger().warning(
                        "Failed to finish round for " + player.getName() + ": " + exception.getMessage());
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
