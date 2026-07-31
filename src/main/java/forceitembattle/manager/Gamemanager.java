package forceitembattle.manager;

import de.threeseconds.openapi.fibservice.client.model.FibMatchItemSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchParticipantSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchTeamSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItem;
import forceitembattle.model.GameContext;
import forceitembattle.model.LeadTracker;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GamePreset;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Gamemanager implements Manager {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey("fib", "backpack");
    private static final Material JOKER_MATERIAL = Material.BARRIER;
    private static final String MATCH_STATS_URL = "https://forceitembattle.net/stats?view=match&id=";
    private static final long MATCH_STATS_LINK_DELAY_TICKS = 5L;
    private final ForceItemBattle forceItemBattle;
    private final Map<UUID, ForceItemPlayer> forceItemPlayerMap;
    /** End-of-game result screens, keyed by player / by team. Paged: page → slot → stack. */
    private final Map<UUID, Map<Integer, Map<Integer, ItemStack>>> savedInventory = new HashMap<>();
    private final Map<Team, Map<Integer, Map<Integer, ItemStack>>> savedInventoryTeam = new HashMap<>();
    private final LeadTracker leadTracker = new LeadTracker();
    @Setter
    @Getter
    public GameState currentGameState;
    @Setter
    private GamePreset currentGamePreset;
    @Getter
    @Setter
    private long gameStartTime;
    @Getter
    @Setter
    private UUID matchId;

    /** True once the match-history PUT landed; the stats link is held back until the result reveal. */
    private boolean matchStatsLinkReady;
    /** True once every result has been revealed via /result (the winner comes last). */
    private boolean matchResultsRevealed;
    /** Guard so the stats link is broadcast exactly once per match. */
    private boolean matchStatsLinkShared;

    /**
     * Wall-clock intervals during which the game was paused, as [start, end] millis pairs.
     *
     * Item times are wall-clock deltas between hand-ins, so a pause between two hand-ins would
     * otherwise be counted as time spent finding — a 22-minute pause turned one item into a 30-minute
     * "timesink" that never happened. Recording the pause intervals lets submit time subtract the
     * pause overlap from each item's window, so seconds_taken reflects play time, not wall time.
     *
     * A pause in progress is held in pauseStartedAt until it is resumed and the closed interval is
     * appended here. Cleared at the start of each game.
     */
    private final List<long[]> pauseIntervals = new ArrayList<>();

    /** Wall-clock millis when the current pause began, or 0 when the game is not paused. */
    private long pauseStartedAt;
    /**
     * Total game duration (seconds).
     */
    @Getter
    @Setter
    private int gameDuration;

    /**
     * Jokers configured for the current round.
     *
     * Kept here rather than only in /start's local scope because a player who reconnects after the
     * countdown ended still has to be equipped, and by then the command's frame is long gone.
     */
    @Getter
    @Setter
    private int jokerAmount;

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

    private void submitMatchHistory(Map<ForceItemPlayer, Integer> placesMap, Map<Team, Integer> teamPlaces) {
        GameSettings settings = this.forceItemBattle.getSettings();
        boolean teamMode = settings.isSettingEnabled(GameSetting.TEAM);

        // Settings snapshot: GameSetting constant name -> value as text. Integer-valued settings
        // (BACKPACKSIZE, QUICKIE, ...) read via getSettingValue; everything else is a toggle.
        Map<String, String> settingsSnapshot = new LinkedHashMap<>();
        for (GameSetting setting : GameSetting.values()) {
            String value = setting.defaultValue() instanceof Integer
                    ? String.valueOf(settings.getSettingValue(setting))
                    : String.valueOf(settings.isSettingEnabled(setting));
            settingsSnapshot.put(setting.name(), value);
        }

        List<FibMatchTeamSubmitDto> teams = new ArrayList<>();
        if (teamMode) {
            for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
                teams.add(new FibMatchTeamSubmitDto()
                        .teamIndex(team.getTeamId())
                        .teamName(team.getName())
                        .color(team.getColor() != null ? team.getColor().name() : null));
            }
        }

        List<FibMatchParticipantSubmitDto> participants = new ArrayList<>();
        for (ForceItemPlayer forceItemPlayer : this.forceItemPlayerMap.values()) {
            if (forceItemPlayer.isSpectator()) {
                continue;
            }
            Team team = forceItemPlayer.currentTeam();
            Integer placement = team == null
                    ? placesMap.get(forceItemPlayer)
                    : (teamPlaces != null ? teamPlaces.get(team) : null);
            long score = team == null ? forceItemPlayer.currentScore() : team.getCurrentScore();
            participants.add(new FibMatchParticipantSubmitDto()
                    .playerUuid(forceItemPlayer.player().getUniqueId())
                    .teamIndex(team == null ? null : team.getTeamId())
                    .finalScore(score)
                    .placement(placement != null ? placement : 0)
                    .won(placement != null && placement == 1));
        }

        List<FibMatchItemSubmitDto> items = new ArrayList<>();
        if (teamMode) {
            for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
                appendMatchItems(items, team.getFoundItems(), null, team.getTeamId(), this.gameStartTime);
            }
        } else {
            for (ForceItemPlayer forceItemPlayer : this.forceItemPlayerMap.values()) {
                if (forceItemPlayer.isSpectator()) {
                    continue;
                }
                appendMatchItems(items, forceItemPlayer.foundItems(),
                        forceItemPlayer.player().getUniqueId(), null, this.gameStartTime);
            }
        }

        FibMatchSubmitRequestDto request = new FibMatchSubmitRequestDto()
                .startedAt(Instant.ofEpochMilli(this.gameStartTime).atOffset(ZoneOffset.UTC))
                .endedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .durationSeconds(this.gameDuration)
                .mode(teamMode ? FibMatchSubmitRequestDto.ModeEnum.TEAM : FibMatchSubmitRequestDto.ModeEnum.SOLO)
                .leadChanges(this.leadTracker.leadChanges())
                .teams(teams)
                .participants(participants)
                .items(items)
                .settings(settingsSnapshot);

        this.forceItemBattle.getFibService().matchHistory().submitMatchAsync(this.matchId, request, () -> {
            // Match is persisted now, so each participant's found-set includes it. Evaluate the
            // collection achievement at conclusion (fresh read), not a game late.
            for (ForceItemPlayer participant : this.forceItemPlayerMap.values()) {
                if (participant.isSpectator()) {
                    continue;
                }
                Player participantPlayer = participant.player();
                if (participantPlayer != null && participantPlayer.isOnline()) {
                    this.forceItemBattle.getAchievementManager().evaluateCollectionAchievement(participantPlayer);
                }
            }

            this.matchStatsLinkReady = true;
            this.tryShareMatchStatsLink();
        });
    }

    /**
     * Marks the ceremonial /result reveal as finished (the winner is shown last). Called by
     * /result; the stats link goes out only once both this and the match PUT have completed.
     */
    public void markMatchResultsRevealed() {
        this.matchResultsRevealed = true;
        this.tryShareMatchStatsLink();
    }

    /**
     * Broadcasts the match stats link, but only once the match is persisted (so the page exists)
     * and the full /result reveal has finished — any earlier and the link would spoil the winner.
     */
    private void tryShareMatchStatsLink() {
        if (!this.matchStatsLinkReady || !this.matchResultsRevealed || this.matchStatsLinkShared) {
            return;
        }
        this.matchStatsLinkShared = true;

        String matchUrl = MATCH_STATS_URL + this.matchId;
        Bukkit.getScheduler().runTaskLater(this.forceItemBattle, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>The match has concluded — see the full breakdown:"));
            player.sendMessage(Text.of("<dark_gray>» <click:open_url:'" + matchUrl
                    + "'><hover:show_text:'<gray>Opens the match stats in your browser'>"
                    + "<dark_gray>[<aqua><b>View Match Stats</b><dark_gray>]</hover></click>"));
            player.sendMessage(" ");
        }), MATCH_STATS_LINK_DELAY_TICKS);
    }

    /**
     * Re-evaluates who is in front and records a lead change if the sole leader's identity moved.
     * Called from the one place a score can change.
     */
    public void evaluateLead() {
        this.leadTracker.onStandingsChanged(this.currentSoleLeader());
    }

    /**
     * Identity of the unique highest scorer, or null when the top score is shared. Team mode keys on
     * team id, solo on player UUID; the two never mix within a game, since the mode can't change
     * mid-match.
     *
     * Spectators are skipped, matching who gets written as a participant at submit time -- otherwise a
     * spectator sitting on 0 could create a phantom tie in a one-player game.
     */
    private Object currentSoleLeader() {
        Object best = null;
        int bestScore = Integer.MIN_VALUE;
        boolean tied = false;
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
                int score = team.getCurrentScore() == null ? 0 : team.getCurrentScore();
                if (best == null || score > bestScore) {
                    best = team.getTeamId();
                    bestScore = score;
                    tied = false;
                } else if (score == bestScore) {
                    tied = true;
                }
            }
        } else {
            for (ForceItemPlayer forceItemPlayer : this.forceItemPlayerMap.values()) {
                if (forceItemPlayer.isSpectator()) {
                    continue;
                }
                int score = forceItemPlayer.currentScore() == null ? 0 : forceItemPlayer.currentScore();
                if (best == null || score > bestScore) {
                    best = forceItemPlayer.player().getUniqueId();
                    bestScore = score;
                    tied = false;
                } else if (score == bestScore) {
                    tied = true;
                }
            }
        }
        return tied ? null : best;
    }

    private void appendMatchItems(List<FibMatchItemSubmitDto> out, List<ForceItem> found,
                                  UUID playerUuid, Integer teamIndex, long startedAtMillis) {
        // The first item is measured from the match start; every later one from the previous
        // hand-in by this same owner. Computed from timestamps rather than parsed out of
        // ForceItem.timeNeeded, which is a formatted display string.
        long previousMillis = startedAtMillis;
        for (int i = 0; i < found.size(); i++) {
            ForceItem forceItem = found.get(i);
            String b2bRarity = (forceItem.back2Back() != null && forceItem.back2Back().isActive()
                    && forceItem.back2Back().getRarityType() != null)
                    ? forceItem.back2Back().getRarityType().name()
                    : null;
            // Pause-aware: the raw wall-clock span for this item minus any time the game spent
            // paused inside that span, so a pause between two hand-ins is not counted as time spent
            // finding. Without this a single item that straddled a 22-minute pause reads as a
            // 22-minute find that never happened, and lands at the top of the "biggest timesinks".
            // Clamped: a clock adjustment mid-match must not write a negative duration.
            long rawMillis = forceItem.timeStamp() - previousMillis;
            long playMillis = rawMillis - pausedMillisWithin(previousMillis, forceItem.timeStamp());
            long secondsTaken = Math.max(0L, playMillis / 1000L);
            previousMillis = forceItem.timeStamp();
            out.add(new FibMatchItemSubmitDto()
                    .playerUuid(playerUuid)
                    .teamIndex(teamIndex)
                    .itemName(forceItem.material().getKey().asString())
                    .skipped(forceItem.usedSkip())
                    .b2bRarity(b2bRarity)
                    .orderIndex(i)
                    .secondsTaken((int) secondsTaken)
                    .collectedAt(Instant.ofEpochMilli(forceItem.timeStamp()).atOffset(ZoneOffset.UTC)));
        }
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
        this.leadTracker.reset();
        this.clearPauseIntervals();
        this.matchStatsLinkReady = false;
        this.matchResultsRevealed = false;
        this.matchStatsLinkShared = false;
        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);
        boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);
        long now = System.currentTimeMillis();

        // Everyone starts the round un-equipped; applyStartSetup flips this back per player.
        this.forceItemPlayerMap.values().forEach(forceItemPlayer -> forceItemPlayer.setStartSetupApplied(false));

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

    public void advanceMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.runMode()) {
            advanceSeededMaterials(context);
        } else {
            advanceRandomMaterials(forceItemPlayer, context);
        }
    }

    private void advanceSeededMaterials(GameContext context) {
        Material nextMaterial = this.generateSeededMaterial();
        long now = System.currentTimeMillis();

        if (context.teamGame()) {
            this.forceItemPlayerMap.values().forEach(player -> {
                // Spectators hold no team -- someone who joined during the countdown sits in the
                // roster with a null team, and used to take the whole skip down with an NPE.
                if (player.isSpectator()) return;

                Team team = player.currentTeam();
                team.setPreviousMaterial(team.getCurrentMaterial());
                team.setCurrentMaterial(team.getNextMaterial());
                team.setNextMaterial(nextMaterial);
                team.setLastItemAssignedAt(now);
            });
        } else {
            this.forceItemPlayerMap.values().forEach(player -> {
                if (player.isSpectator()) return;

                player.setPreviousMaterial(player.currentMaterial());
                player.setCurrentMaterial(player.getNextMaterial());
                player.setNextMaterial(nextMaterial);
                player.setLastItemAssignedAt(now);
            });
        }
    }

    private void advanceRandomMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        Material nextMaterial = this.generateMaterial();
        long now = System.currentTimeMillis();

        if (context.teamGame()) {
            Team team = forceItemPlayer.currentTeam();
            team.setPreviousMaterial(team.getCurrentMaterial());
            team.setCurrentMaterial(team.getNextMaterial());
            team.setNextMaterial(nextMaterial);
            team.setLastItemAssignedAt(now);
        } else {
            forceItemPlayer.setPreviousMaterial(forceItemPlayer.currentMaterial());
            forceItemPlayer.setCurrentMaterial(forceItemPlayer.getNextMaterial());
            forceItemPlayer.setNextMaterial(nextMaterial);
            forceItemPlayer.setLastItemAssignedAt(now);
        }
    }

    /** Stores the paged result screen for a finished player or team. */
    public void saveResultPages(@Nullable ForceItemPlayer forceItemPlayer, @Nullable Team team,
                                Map<Integer, Map<Integer, ItemStack>> pages) {
        if (team != null) {
            this.savedInventoryTeam.put(team, pages);
        } else if (forceItemPlayer != null) {
            this.savedInventory.put(forceItemPlayer.player().getUniqueId(), pages);
        }
    }

    /** The stored result screen, or null if that player/team never finished a game. */
    @Nullable
    public Map<Integer, Map<Integer, ItemStack>> getResultPages(@Nullable ForceItemPlayer forceItemPlayer,
                                                                @Nullable Team team) {
        if (team != null) {
            return this.savedInventoryTeam.get(team);
        }
        if (forceItemPlayer != null) {
            return this.savedInventory.get(forceItemPlayer.player().getUniqueId());
        }
        return null;
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
                // Spectators hold no team; skipping them here is what keeps a countdown joiner from
                // NPE-ing every skip for the rest of the round.
                if (p.isSpectator()) return;

                if (!adminCommand)
                    gamePlayer.currentTeam().setRemainingJokers(gamePlayer.currentTeam().getRemainingJokers() - 1);
                p.currentTeam().setCurrentMaterial(pair.current());
                p.currentTeam().setNextMaterial(pair.next());
            });
        } else {
            forceItemPlayerMap().values().forEach(p -> {
                if (p.isSpectator()) return;

                if (!adminCommand) gamePlayer.setRemainingJokers(gamePlayer.remainingJokers() - 1);
                p.setCurrentMaterial(pair.current());
                p.setNextMaterial(pair.next());
            });
        }

    }

    public void giveSpectatorItems(Player player) {
        player.getInventory().setItem(1, new ItemBuilder(Material.LIME_DYE).setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(2, new ItemBuilder(Material.WRITTEN_BOOK)
                .setDisplayName("<dark_gray>» <dark_aqua>Collection")
                .addItemFlags(ItemFlag.values())
                .getItemStack());
        player.getInventory().setItem(3, new ItemBuilder(Material.COMPASS).setDisplayName("<dark_gray>» <yellow>Teleporter").getItemStack());
        player.getInventory().setItem(5, new ItemBuilder(Material.GRASS_BLOCK).setDisplayName("<dark_gray>» <dark_green>Overworld").getItemStack());
        player.getInventory().setItem(6, new ItemBuilder(Material.NETHERRACK).setDisplayName("<dark_gray>» <red>Nether").getItemStack());
        player.getInventory().setItem(7, new ItemBuilder(Material.ENDER_EYE).setDisplayName("<dark_gray>» <dark_purple>End").getItemStack());
        player.getInventory().setItem(8, new ItemBuilder(Material.SPYGLASS).setDisplayName("<dark_gray>» <green>Spectate").getItemStack());
    }

    /**
     * Applies one player's round setup: gamemode, jokers, starting tools, backpack and the
     * gamesPlayed write. Spectators are put into spectator mode instead. Safe to call more than
     * once — {@link ForceItemPlayer#isStartSetupApplied()} makes every call after the first a no-op.
     *
     * This lives here rather than inside /start because the countdown-end pass only walks the
     * players who happen to be online at that instant. Someone who disconnected during the countdown
     * is still a participant — their team and force item were assigned before the countdown even
     * began — so the exact same setup has to run when they come back, or they rejoin in ADVENTURE
     * mode with an empty inventory and no jokers, unable to play the round they are scored in.
     */
    public void applyStartSetup(Player player) {
        ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());

        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            return;
        }
        if (forceItemPlayer.isStartSetupApplied()) {
            return;
        }
        forceItemPlayer.setStartSetupApplied(true);

        boolean teamMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM);

        this.sendStartSummary(player, this.gameDuration / 60, this.jokerAmount);

        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();

        if (!teamMode) {
            forceItemPlayer.setRemainingJokers(this.jokerAmount);
            if (!this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN)) {
                player.getInventory().setItem(4, getJokers(this.jokerAmount));
            }
        } else if (!this.isStarting() && forceItemPlayer.getRemainingJokers() > 0) {
            // At countdown end the whole roster is served by /start's distributeTeamJokers, which
            // splits the pool across both members. A player rejoining later missed that split, so
            // hand them what the team has left: the stack is only the button, the count that gates
            // a skip lives on the team.
            player.getInventory().setItem(4, getJokers(forceItemPlayer.getRemainingJokers()));
        }

        player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));

        player.setLevel(0);
        player.setExp(0);
        player.setWalkSpeed(0.2f);
        player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
        player.getPassengers().forEach(Entity::remove);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.setGameMode(GameMode.SURVIVAL);
        player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            if (teamMode) {
                this.forceItemBattle.getBackpackManager().createTeamBackpack(forceItemPlayer.currentTeam(), forceItemPlayer);
            } else {
                this.forceItemBattle.getBackpackManager().createBackpack(forceItemPlayer);
            }
        }

        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS)) {
            FibStatisticsClient helper = this.forceItemBattle.getFibService().statistics();
            if (!teamMode) {
                helper.updateSoloStatisticsAsync(player.getUniqueId(), FIBServiceClient.soloUpdate().gamesPlayedAdd(1));
                return;
            }
            if (forceItemPlayer.currentTeam() != null) {
                Team currentTeam = forceItemPlayer.currentTeam();

                for (ForceItemPlayer teamPlayer : currentTeam.getPlayers()) {
                    if (!teamPlayer.equals(forceItemPlayer)) {
                        // Both teammates write the same normalized team row, so only the
                        // lower-UUID side sends gamesPlayed — otherwise every game counts twice.
                        if (player.getUniqueId().toString()
                                .compareTo(teamPlayer.player().getUniqueId().toString()) < 0) {
                            helper.updateTeamStatisticsAsync(
                                    player.getUniqueId(),
                                    teamPlayer.player().getUniqueId(),
                                    FIBServiceClient.teamUpdate().gamesPlayedAdd(1)
                            );
                        }
                        break;
                    }
                }
            }
        }
    }

    private void sendStartSummary(Player player, int timeMinutes, int jokersAmount) {
        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>Force Item Battle</b> <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(Text.of("  <dark_gray>● <gray>Duration <dark_gray>» <green>" + timeMinutes + " minutes"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Jokers <dark_gray>» <green>" + jokersAmount));
        for (GameSetting gameSettings : GameSetting.values()) {
            if (gameSettings.defaultValue() instanceof Integer) continue;
            player.sendMessage(Text.of("  <dark_gray>● <gray>" + gameSettings.displayName() + " <dark_gray>» <green>" + (this.forceItemBattle.getSettings().isSettingEnabled(gameSettings) ? "<dark_green>✔" : "<dark_red>✘")));
        }
        player.sendMessage(" ");
        player.sendMessage(Text.of(" <dark_gray>● <gray>Useful Commands:"));
        player.sendMessage(Text.of("  <dark_gray>» <gold>/info"));
        player.sendMessage(Text.of("  <dark_gray>» <gold>/infowiki"));
        player.sendMessage(Text.of("  <dark_gray>» <gold>/spawn"));
        player.sendMessage(Text.of("  <dark_gray>» <gold>/bed"));
        player.sendMessage(Text.of("  <dark_gray>» <gold>/pos"));
        player.sendMessage("");
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
                    Team currentTeam = forceItemPlayer.currentTeam();

                    // Hoisted: the solo branch, the team branch and the win-streak write all need it.
                    boolean won = currentTeam == null
                            ? Integer.valueOf(1).equals(placesMap.get(forceItemPlayer))
                            : (teamPlaces != null && Integer.valueOf(1).equals(teamPlaces.get(currentTeam)));

                    if (currentTeam == null) {
                        // ---- Solo game: everything on solo stats ----
                        var soloUpdate = FIBServiceClient.soloUpdate()
                                .blocksTravelledAdd(distance)
                                .highestScore((long) forceItemPlayer.currentScore());

                        if (won) {
                            soloUpdate.gamesWonAdd(1);
                        }

                        helper.updateSoloStatisticsAsync(player.getUniqueId(), soloUpdate);
                    } else {
                        // ---- Team game: keep everything on team/member stats, never solo ----
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
                                if (lowerSide && won) {
                                    teamUpdate.gamesWonAdd(1);
                                }

                                helper.updateTeamStatisticsAsync(player.getUniqueId(), teammateUuid, teamUpdate);
                                break;
                            }
                        }
                    }

                    // Win streak. Player-scoped, so this is unlike every other write above:
                    //   - losers report too — a LOSS is what resets the streak
                    //   - no lower-UUID dedupe — both members of a winning team each own a streak,
                    //     so both send their own WIN
                    helper.recordGameOutcomeAsync(
                            player.getUniqueId(),
                            new FibPlayerStatsUpdateRequestDto()
                                    .outcome(won
                                            ? FibPlayerStatsUpdateRequestDto.OutcomeEnum.WIN
                                            : FibPlayerStatsUpdateRequestDto.OutcomeEnum.LOSS)
                                    .playerName(player.getName()));
                }
            } catch (Exception exception) {
                this.forceItemBattle.getLogger().warning(
                        "Failed to finish round for " + player.getName() + ": " + exception.getMessage());
            }
        });

        if (statsEnabled) {
            this.submitMatchHistory(placesMap, teamPlaces);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(onlinePlayer.getUniqueId());
                if (forceItemPlayer != null && !forceItemPlayer.isSpectator()) {
                    this.forceItemBattle.getAchievementManager().evaluateGlobalAchievements(onlinePlayer);
                }
            }
        }
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

    /**
     * True from the moment {@code /start} begins its countdown until the game actually flips to
     * MID_GAME. During this window teams and force items have already been assigned, so the roster
     * is frozen: anyone joining is a spectator for the round, and anyone leaving keeps their spot.
     */
    public boolean isStarting() {
        return this.getCurrentGameState() == GameState.STARTING;
    }

    public boolean isPausedGame() {
        return this.getCurrentGameState() == GameState.PAUSED_GAME;
    }

    /**
     * Begin a pause: flip state and record when it started, so its duration can be subtracted from
     * item times on resume. Called by /pause instead of setting the state directly, so the timing
     * bookkeeping cannot be bypassed.
     */
    public void pauseGame() {
        this.pauseStartedAt = System.currentTimeMillis();
        this.setCurrentGameState(GameState.PAUSED_GAME);
        this.clearMobTargets();
    }

    /**
     * Drop every mob's aggro at the moment of pause.
     *
     * Cancelling target-acquisition (in the entity-target listener) stops mobs locking on DURING the
     * pause, but a mob already chasing a player when /pause runs keeps its target and keeps pathing —
     * and lands its hit the instant the game resumes. Clearing targets here makes the pause actually
     * calm: mobs already tracking a player let go, and the listener keeps them from re-acquiring
     * until resume. Together they give "no aggro while paused, no free hit on unpause".
     */
    private void clearMobTargets() {
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(Mob.class).forEach(mob -> {
                    if (mob.getTarget() instanceof Player) {
                        mob.setTarget(null);
                    }
                }));
    }

    /**
     * End a pause: close the open interval and flip state back. The closed [start, end] is kept so
     * that any item whose find-window straddled this pause has the paused span removed from its
     * seconds_taken at submit time.
     */
    public void resumeGame() {
        if (this.pauseStartedAt > 0L) {
            this.pauseIntervals.add(new long[]{this.pauseStartedAt, System.currentTimeMillis()});
            this.pauseStartedAt = 0L;
        }
        this.setCurrentGameState(GameState.MID_GAME);
    }

    /** Clears recorded pauses. Called when a new game starts so intervals never carry across games. */
    public void clearPauseIntervals() {
        this.pauseIntervals.clear();
        this.pauseStartedAt = 0L;
    }

    /**
     * The milliseconds of pause that overlap the window [fromMillis, toMillis].
     *
     * Summed over every recorded interval by clamping each to the window and taking the positive
     * width — so a pause fully inside the window counts whole, a pause partly overlapping counts
     * only its overlap, and a pause outside counts zero. Correct regardless of how many pauses fall
     * in one item's window, or whether the item began mid-pause.
     */
    private long pausedMillisWithin(long fromMillis, long toMillis) {
        long paused = 0L;
        for (long[] interval : this.pauseIntervals) {
            long overlapStart = Math.max(fromMillis, interval[0]);
            long overlapEnd = Math.min(toMillis, interval[1]);
            if (overlapEnd > overlapStart) {
                paused += overlapEnd - overlapStart;
            }
        }
        return paused;
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
