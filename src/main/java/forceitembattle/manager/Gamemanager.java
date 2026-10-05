package forceitembattle.manager;

import forceitembattle.achievements.AchievementManager;
import forceitembattle.ceremony.ResultStage;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.GameState;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Standings;
import forceitembattle.model.Team;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.MatchHistoryReporter;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Gamemanager implements Manager {

    private final GameSettings settings;
    private final RoundClock roundClock;
    private final ResultCeremony resultCeremony;
    private final ResultStage resultStage;
    private final ItemDifficultiesManager items;
    private final BackpackManager backpacks;
    private final RecipeManager recipeManager;
    private final PositionManager positionManager;
    private final ScoreboardManager scoreboardManager;
    private final TeamsManager teamManager;
    private final WanderingTraderManager traders;
    private final RandomEventManager randomEvents;
    private final AchievementManager achievementManager;
    private final FIBServiceClient fibService;
    private final Consumer<Location> spawnLocationSink;
    private final Plugin plugin;
    private final Roster roster;

    @Getter
    private final MatchHistoryReporter matchHistory;

    private final RoundPhase roundPhase;

    /** Kept past /start so a player reconnecting after the countdown can still be equipped. */
    @Getter
    @Setter
    private int jokerAmount;

    public Gamemanager(Plugin plugin, Roster roster, RoundPhase roundPhase, GameSettings settings,
                       RoundClock roundClock, ResultCeremony resultCeremony, ResultStage resultStage,
                       ItemDifficultiesManager items,
                       BackpackManager backpacks, RecipeManager recipeManager, PositionManager positionManager,
                       ScoreboardManager scoreboardManager, TeamsManager teamManager,
                       WanderingTraderManager traders, RandomEventManager randomEvents,
                       AchievementManager achievementManager, FIBServiceClient fibService,
                       Consumer<Location> spawnLocationSink) {
        this.plugin = plugin;
        this.settings = settings;
        this.roundClock = roundClock;
        this.resultCeremony = resultCeremony;
        this.resultStage = resultStage;
        this.items = items;
        this.backpacks = backpacks;
        this.recipeManager = recipeManager;
        this.positionManager = positionManager;
        this.scoreboardManager = scoreboardManager;
        this.teamManager = teamManager;
        this.traders = traders;
        this.randomEvents = randomEvents;
        this.achievementManager = achievementManager;
        this.fibService = fibService;
        this.spawnLocationSink = spawnLocationSink;
        this.roster = roster;
        this.roundPhase = roundPhase;


        this.matchHistory = new MatchHistoryReporter(fibService.matchHistory(), roster, settings,
                teamManager);
    }

    public void evaluateLead() {
        this.matchHistory.recordStandings(this.currentSoleLeader());
    }

    /** Null when the top score is shared. Spectators are skipped, or a spectator on 0 could create a phantom tie. */
    private ScoreOwner currentSoleLeader() {
        ScoreOwner best = null;
        boolean tied = false;
        for (ScoreOwner owner : this.roster.activeScoreOwners()) {
            if (best == null || owner.score() > best.score()) {
                best = owner;
                tied = false;
            } else if (owner.score() == best.score()) {
                tied = true;
            }
        }
        return tied ? null : best;
    }

    /** Without this, from round two on applyStartSetup no-ops for everyone and nobody leaves creative. */
    public void resetStartSetup() {
        this.roster.players().values().forEach(forceItemPlayer -> forceItemPlayer.setStartSetupApplied(false));
    }

    /** Idempotent, so someone who disconnected during the countdown can be set up on rejoin. */
    public void applyStartSetup(Player player) {
        ForceItemPlayer forceItemPlayer = this.roster.participant(player.getUniqueId()).orElse(null);

        if (forceItemPlayer == null) {
            PlayerOutfitter.toSpectator(player);
            return;
        }
        if (forceItemPlayer.isStartSetupApplied()) {
            return;
        }
        forceItemPlayer.setStartSetupApplied(true);

        GameContext context = GameContext.of(this.settings, forceItemPlayer);

        this.sendStartSummary(player, this.roundClock.totalSeconds() / 60, this.jokerAmount);

        // A solo player owns their own pool, so it is set here. A team's is set once for the whole
        // team by distributeTeamJokers, and setting it per member would just overwrite it.
        if (!forceItemPlayer.isInTeam()) {
            forceItemPlayer.scoreOwner().setJokers(this.jokerAmount);
        }

        PlayerOutfitter.toPlayer(player,
                RoundSetup.jokersOnHotbar(forceItemPlayer, context, this.jokerAmount, this.roundPhase.isStarting()));

        if (context.backpackEnabled()) {
            if (forceItemPlayer.isInTeam()) {
                this.backpacks.createTeamBackpack(forceItemPlayer.currentTeam(), forceItemPlayer);
            } else {
                this.backpacks.createBackpack(forceItemPlayer);
            }
        }

        if (this.settings.isSettingEnabled(GameSetting.STATS)) {
            this.fibService.statisticsWrites().recordGameStarted(forceItemPlayer);
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
            player.sendMessage(Text.of("  <dark_gray>● <gray>" + gameSettings.displayName() + " <dark_gray>» <green>" + (this.settings.isSettingEnabled(gameSettings) ? "<dark_green>✔" : "<dark_red>✘")));
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

    /** Everything that happens when /start's countdown hits zero. Keep orchestration out of /start. */
    public void startGame(int durationMinutes, int jokersAmount) {
        this.matchHistory.beginMatch(UUID.randomUUID());

        this.recipeManager.initRecipes();
        this.positionManager.clearPositions();
        this.items.configureUnlockSchedule(durationMinutes);

        World world = Objects.requireNonNull(Dimension.OVERWORLD.world());
        prepareSpawn(world.getSpawnLocation());

        Bukkit.getWorlds().forEach(w -> w.getWorldBorder().reset());
        world.setGameRule(GameRules.ADVANCE_TIME, true);
        world.setTime(0);

        // Only the players online at this instant. Anyone who disconnected during the countdown
        // keeps their roster spot and is set up by the same call when they rejoin.
        Bukkit.getOnlinePlayers().forEach(this::applyStartSetup);

        if (this.settings.isSettingEnabled(GameSetting.TEAM)) {
            distributeTeamJokers(jokersAmount);
        }

        this.traders.startTimer();
        this.randomEvents.startGame();
        this.achievementManager.resetProgress();
        this.items.resetUnlockAnnouncements();
        this.roundPhase.moveTo(GameState.MID_GAME);
        this.scoreboardManager.updateAllPlayers();
    }

    /** An offline member gets no stack here; applyStartSetup hands them the team's remaining pool on rejoin. */
    private void distributeTeamJokers(int jokersAmount) {
        this.teamManager.getTeams().forEach(team -> {
            team.setJokers(jokersAmount);

            List<ForceItemPlayer> members = team.members();
            int[] shares = RoundSetup.splitEvenly(jokersAmount, members.size());

            for (int member = 0; member < members.size(); member++) {
                PlayerOutfitter.giveJokerShare(members.get(member).player(), shares[member]);
            }
        });
    }

    /** Clears the two blocks at spawn so nobody starts the round inside terrain. */
    private void prepareSpawn(Location location) {
        this.spawnLocationSink.accept(location.clone());

        Block block = location.getBlock();
        block.setType(Material.AIR);
        block.getRelative(BlockFace.UP).setType(Material.AIR);
    }

    /** Not the stats standings: those keep spectators, leave ties unordered, and are skipped when STATS is off. */
    private List<ResultCeremony.Reveal> revealOrder() {
        if (this.settings.isSettingEnabled(GameSetting.TEAM)) {
            return ResultCeremony.orderFrom(
                    Standings.ofTeams(this.teamManager.getTeams()));
        }

        Map<UUID, ForceItemPlayer> contenders = this.roster.players().entrySet().stream()
                .filter(entry -> !entry.getValue().isSpectator())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                        LinkedHashMap::new));

        // A solo player is not a ScoreOwner; the SoloScore behind them is.
        Map<ScoreOwner, Integer> byOwner = new LinkedHashMap<>();
        Standings.ofPlayers(Standings.sortedByScore(contenders, false))
                .forEach((forceItemPlayer, place) -> byOwner.put(forceItemPlayer.scoreOwner(), place));

        return ResultCeremony.orderFrom(byOwner);
    }

    public void finishGame() {
        this.roundPhase.moveTo(GameState.END_GAME);
        this.achievementManager.checkGameEndAchievements();

        boolean statsEnabled = this.settings.isSettingEnabled(GameSetting.STATS);
        Map<ForceItemPlayer, Integer> placesMap = statsEnabled
                ? Standings.ofPlayers(this.roster.players())
                : null;
        Map<Team, Integer> teamPlaces = (statsEnabled
                && this.settings.isSettingEnabled(GameSetting.TEAM))
                ? Standings.ofTeams(this.teamManager.getTeams())
                : null;

        World overworld = Dimension.OVERWORLD.world();
        Location resultSpawn = overworld == null ? null : overworld.getSpawnLocation();

        Bukkit.getOnlinePlayers().forEach(player -> {
            try {
                PlayerOutfitter.toResultScreen(player, resultSpawn);

                if (player.isOp()) {
                    player.sendMessage(Text.of("<red>Use /result to see the results from every player"));
                }
            } catch (Exception exception) {
                this.plugin.getLogger().warning(
                        "Failed to finish round for " + player.getName() + ": " + exception.getMessage());
            }
        });

        // Every participant, not every online player: someone disconnected at the end still played
        // the round, and a team's win is only counted by its primary writer, who may be the one away.
        if (statsEnabled) {
            for (ForceItemPlayer forceItemPlayer : this.roster.players().values()) {
                if (Roster.isPlaying(forceItemPlayer)) {
                    this.recordRoundFinished(forceItemPlayer, placesMap, teamPlaces);
                }
            }
        }

        if (resultSpawn != null) {
            this.resultStage.open(resultSpawn);
        }

        // Its own ordering, not the stats one: those keep spectators and are null when STATS is off.
        this.resultCeremony.beginFor(
                this.matchHistory.getMatchId(), this.revealOrder());

        if (statsEnabled) {
            this.matchHistory.submit(placesMap, teamPlaces, this.roundClock.totalSeconds(),
                    this::evaluateCollectionAchievements);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                ForceItemPlayer forceItemPlayer = this.roster.get(onlinePlayer.getUniqueId());
                if (Roster.isPlaying(forceItemPlayer)) {
                    this.achievementManager.evaluateGlobalAchievements(onlinePlayer);
                }
            }
        }
    }

    private void recordRoundFinished(ForceItemPlayer forceItemPlayer, Map<ForceItemPlayer, Integer> placesMap,
                                     Map<Team, Integer> teamPlaces) {
        Player player = forceItemPlayer.player();
        try {
            Team currentTeam = forceItemPlayer.currentTeam();
            boolean won = currentTeam == null
                    ? Integer.valueOf(1).equals(placesMap.get(forceItemPlayer))
                    : (teamPlaces != null && Integer.valueOf(1).equals(teamPlaces.get(currentTeam)));

            this.fibService.statisticsWrites().recordRoundFinished(
                    forceItemPlayer,
                    player.getName(),
                    forceItemPlayer.activeScore(),
                    this.distanceOrZero(player),
                    won);
        } catch (Exception exception) {
            this.plugin.getLogger().warning(
                    "Failed to record the round for " + player.getName() + ": " + exception.getMessage());
        }
    }

    /** A distance that cannot be read must not cost the player their win and score as well. */
    private long distanceOrZero(Player player) {
        try {
            return this.calculateDistance(player);
        } catch (RuntimeException exception) {
            this.plugin.getLogger().warning(
                    "Could not read the distance travelled by " + player.getName() + ": " + exception.getMessage());
            return 0L;
        }
    }

    /** In blocks, from every distance statistic (cm). A player who left is read from the stats saved on quit. */
    private int calculateDistance(Player player) {
        OfflinePlayer source = player.isOnline() ? player : Bukkit.getOfflinePlayer(player.getUniqueId());
        int distance = Arrays.stream(Statistic.values())
                .filter(statistic -> statistic.name().contains("CM"))
                .mapToInt(source::getStatistic)
                .sum();

        return (int) Math.round((double) distance / 100);
    }

    /** Use this rather than setting the state, so the pause interval subtracted from item times is recorded. */
    public void pauseGame() {
        this.matchHistory.onPaused();
        this.roundPhase.moveTo(GameState.PAUSED_GAME);
        this.clearMobTargets();
        // Stops block entities, redstone, fluids and mobs; players keep ticking.
        Bukkit.getServerTickManager().setFrozen(true);
    }

    private void evaluateCollectionAchievements() {
        for (ForceItemPlayer participant : this.roster.players().values()) {
            if (participant.isSpectator()) {
                continue;
            }
            Player participantPlayer = participant.player();
            if (participantPlayer != null && participantPlayer.isOnline()) {
                this.achievementManager.evaluateCollectionAchievement(participantPlayer);
            }
        }
    }

    /** A mob already chasing a player keeps its target through the pause; the target listener only stops new ones. */
    private void clearMobTargets() {
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(Mob.class).forEach(mob -> {
                    if (mob.getTarget() instanceof Player) {
                        mob.setTarget(null);
                    }
                }));
    }

    /** The closed interval is subtracted from the seconds_taken of any item whose find straddled the pause. */
    public void resumeGame() {
        this.matchHistory.onResumed();
        Bukkit.getServerTickManager().setFrozen(false);
        this.roundPhase.moveTo(GameState.MID_GAME);
    }




}
