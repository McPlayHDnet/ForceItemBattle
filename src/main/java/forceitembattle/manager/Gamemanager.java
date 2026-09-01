package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.Dimension;
import forceitembattle.model.GameContext;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.MatchHistoryReporter;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundSetup;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.model.Standings;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.ScoreOwner;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Gamemanager implements Manager {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey("fib", "backpack");
    private static final Material JOKER_MATERIAL = Material.BARRIER;
    private final ForceItemBattle forceItemBattle;
    private final Roster roster;

    @Getter
    private final MatchHistoryReporter matchHistory;

    private final RoundPhase roundPhase;
    @Getter
    @Setter
    private long gameStartTime;

    /**
     * Jokers configured for the current round. Kept here rather than in /start's local scope because
     * a player who reconnects after the countdown ended still has to be equipped.
     */
    @Getter
    @Setter
    private int jokerAmount;

    /**
     * Dev/testing override queue: when non-empty, material generation returns these in order before
     * falling back to random. Populated by /forceitem, cleared at the start of every game.
     */
    @Getter
    private final Deque<Material> forcedItemQueue = new ArrayDeque<>();

    public Gamemanager(ForceItemBattle forceItemBattle, Roster roster, RoundPhase roundPhase) {
        this.forceItemBattle = forceItemBattle;
        this.roster = roster;
        this.roundPhase = roundPhase;


        this.matchHistory = new MatchHistoryReporter(forceItemBattle);
    }

    public void evaluateLead() {
        this.matchHistory.recordStandings(this.currentSoleLeader());
    }

    /**
     * Identity of the unique highest scorer, or null when the top score is shared. Team mode keys on
     * team id, solo on player UUID; the two never mix within a game.
     *
     * <p>Spectators are skipped, matching who gets written as a participant at submit time —
     * otherwise a spectator sitting on 0 could create a phantom tie in a one-player game.
     */
    private Object currentSoleLeader() {
        Object best = null;
        int bestScore = Integer.MIN_VALUE;
        boolean tied = false;
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
                int score = team.getCurrentScore();
                if (best == null || score > bestScore) {
                    best = team.getTeamId();
                    bestScore = score;
                    tied = false;
                } else if (score == bestScore) {
                    tied = true;
                }
            }
        } else {
            for (ForceItemPlayer forceItemPlayer : this.roster.players().values()) {
                if (forceItemPlayer.isSpectator()) {
                    continue;
                }
                int score = forceItemPlayer.activeScore();
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

    public void initializeMaterials() {
        this.forcedItemQueue.clear();
        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);
        long now = System.currentTimeMillis();

        // Everyone starts the round un-equipped; applyStartSetup flips this back per player.
        this.roster.players().values().forEach(forceItemPlayer -> forceItemPlayer.setStartSetupApplied(false));

        // In run mode everyone shares one seeded pair; otherwise each owner gets their own.
        MaterialPair shared = runMode ? this.nextMaterials(true) : null;

        // One pass over the roster, one pair per score owner — keep it that way. Walking online
        // players instead misses anyone who disconnected during the countdown, and drawing per team
        // member rather than per owner lets the last member's pair win.
        this.roster.activeScoreOwners().forEach(owner -> {
            MaterialPair pair = runMode ? shared : this.nextMaterials(false);
            owner.startRound(pair.current(), pair.next(), now);
        });
    }


    /**
     * Moves whoever this find belongs to onto their next item. In run mode everybody races for the
     * same seeded item, so one find advances the whole server; otherwise only the finder's owner
     * moves. Team versus solo is the owner's business, not this method's.
     */
    public void advanceMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        long now = System.currentTimeMillis();

        if (context.runMode()) {
            Material nextMaterial = this.generateSeededMaterial();
            // Once per owner, not per member: advancing a two-player team twice skips the queued item
            // and leaves current and next both holding nextMaterial.
            this.roster.activeScoreOwners().forEach(owner -> owner.advance(nextMaterial, now));
            return;
        }

        forceItemPlayer.scoreOwner().advance(this.generateMaterial(), now);
    }

    /**
     * Replaces the current item for the whole server with a freshly generated one — what /voteskip
     * does when the vote carries, and what /skip does as an admin override.
     *
     * <p>Charging a joker is <em>not</em> part of this: the only caller that costs one spends it
     * itself, on the initiator. Do not charge one inside the loop below — it runs once per
     * non-spectator, so the initiator would pay a joker for each of them.
     */
    public void forceSkipItem(Player player) {
        if (!this.roster.contains(player.getUniqueId())) {
            return;
        }

        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);

        MaterialPair pair = this.nextMaterials(runMode);

        // Spectators are already out of activeScoreOwners(), which is what keeps a countdown joiner
        // — who holds no team — from NPE-ing every skip for the rest of the round.
        this.roster.activeScoreOwners().forEach(owner -> owner.assignMaterials(pair.current(), pair.next()));
    }

    /**
     * Applies one player's round setup. Spectators are put into spectator mode instead. Safe to call
     * more than once — {@link ForceItemPlayer#isStartSetupApplied()} makes every call after the first
     * a no-op, which is what lets someone who disconnected during the countdown be set up on rejoin
     * rather than landing in ADVENTURE mode with an empty inventory.
     */
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

        GameContext context = GameContext.of(this.forceItemBattle.getSettings(), forceItemPlayer);

        this.sendStartSummary(player, this.forceItemBattle.getRoundClock().totalSeconds() / 60, this.jokerAmount);

        // A solo player owns their own pool, so it is set here. A team's is set once for the whole
        // team by distributeTeamJokers, and setting it per member would just overwrite it.
        if (!forceItemPlayer.isInTeam()) {
            forceItemPlayer.scoreOwner().setJokers(this.jokerAmount);
        }

        PlayerOutfitter.toPlayer(player,
                RoundSetup.jokersOnHotbar(forceItemPlayer, context, this.jokerAmount, this.roundPhase.isStarting()));

        if (context.backpackEnabled()) {
            if (forceItemPlayer.isInTeam()) {
                this.forceItemBattle.getBackpackManager().createTeamBackpack(forceItemPlayer.currentTeam(), forceItemPlayer);
            } else {
                this.forceItemBattle.getBackpackManager().createBackpack(forceItemPlayer);
            }
        }

        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS)) {
            this.forceItemBattle.getFibService().statistics().recordGameStarted(forceItemPlayer);
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

    /**
     * Everything that has to happen the instant /start's countdown reaches zero. /start only parses
     * arguments and runs the countdown, then hands over — keep orchestration out of the command.
     */
    public void startGame(int durationMinutes, int jokersAmount) {
        this.setGameStartTime(System.currentTimeMillis());
        this.matchHistory.beginMatch(UUID.randomUUID());

        this.forceItemBattle.getRecipeManager().initRecipes();
        this.forceItemBattle.getPositionManager().clearPositions();
        this.forceItemBattle.getItemDifficultiesManager().configureUnlockSchedule(durationMinutes);

        World world = Objects.requireNonNull(Dimension.OVERWORLD.world());
        prepareSpawn(world.getSpawnLocation());

        Bukkit.getWorlds().forEach(w -> w.getWorldBorder().reset());
        world.setGameRule(GameRules.ADVANCE_TIME, true);
        world.setTime(0);

        // Only the players online at this instant. Anyone who disconnected during the countdown
        // keeps their roster spot and is set up by the same call when they rejoin.
        Bukkit.getOnlinePlayers().forEach(this::applyStartSetup);

        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            distributeTeamJokers(jokersAmount);
        }

        this.forceItemBattle.getWanderingTraderManager().startTimer();
        this.forceItemBattle.getRandomEventManager().startGame();
        this.forceItemBattle.getAchievementManager().resetProgress();
        this.forceItemBattle.getItemDifficultiesManager().resetUnlockAnnouncements();
        this.roundPhase.moveTo(GameState.MID_GAME);
        this.forceItemBattle.getScoreboardManager().updateAllPlayers();
    }

    /**
     * Splits the round's joker pool across each team's members. The share is computed for the whole
     * team either way, so the split stays stable; an offline member gets no stack here and is handed
     * the team's remaining pool by {@link #applyStartSetup(Player)} on rejoin.
     */
    private void distributeTeamJokers(int jokersAmount) {
        this.forceItemBattle.getTeamManager().getTeams().forEach(team -> {
            team.setJokers(jokersAmount);

            List<ForceItemPlayer> members = team.members();
            int[] shares = RoundSetup.splitJokers(jokersAmount, members.size());

            for (int member = 0; member < members.size(); member++) {
                PlayerOutfitter.giveJokerShare(members.get(member).player(), shares[member]);
            }
        });
    }

    /** Clears the two blocks at spawn so nobody starts the round inside terrain. */
    private void prepareSpawn(Location location) {
        this.forceItemBattle.setSpawnLocation(location.clone());

        Block block = location.getBlock();
        block.setType(Material.AIR);
        block.getRelative(BlockFace.UP).setType(Material.AIR);
    }

    /**
     * The reveal order, worst-placed first. Deliberately not a reuse of the stats standings: that one
     * keeps spectators, leaves ties in map order, and is not computed at all when STATS is off.
     */
    private List<ResultCeremony.Reveal> revealOrder() {
        if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return ResultCeremony.orderFrom(
                    Standings.ofTeams(this.forceItemBattle.getTeamManager().getTeams()));
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
        this.forceItemBattle.getAchievementManager().checkGameEndAchievements();

        boolean statsEnabled = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS);
        Map<ForceItemPlayer, Integer> placesMap = statsEnabled
                ? Standings.ofPlayers(this.roster.players())
                : null;
        Map<Team, Integer> teamPlaces = (statsEnabled
                && this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM))
                ? Standings.ofTeams(this.forceItemBattle.getTeamManager().getTeams())
                : null;

        World overworld = Dimension.OVERWORLD.world();
        Location resultSpawn = overworld == null ? null : overworld.getSpawnLocation();

        Bukkit.getOnlinePlayers().forEach(player -> {
            try {
                ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());

                PlayerOutfitter.toResultScreen(player, resultSpawn);

                if (player.isOp()) {
                    player.sendMessage(Text.of("<red>Use /result to see the results from every player"));
                }

                if (statsEnabled && forceItemPlayer != null && !forceItemPlayer.isSpectator()) {
                    Team currentTeam = forceItemPlayer.currentTeam();

                    boolean won = currentTeam == null
                            ? Integer.valueOf(1).equals(placesMap.get(forceItemPlayer))
                            : (teamPlaces != null && Integer.valueOf(1).equals(teamPlaces.get(currentTeam)));

                    this.forceItemBattle.getFibService().statistics().recordRoundFinished(
                            forceItemPlayer,
                            player.getName(),
                            forceItemPlayer.activeScore(),
                            (long) this.calculateDistance(forceItemPlayer.player()),
                            won);
                }
            } catch (Exception exception) {
                this.forceItemBattle.getLogger().warning(
                        "Failed to finish round for " + player.getName() + ": " + exception.getMessage());
            }
        });

        // Its own ordering, not the stats one: spectators excluded, ties broken on UUID so they are
        // dealt out the same way every time. The stats maps do neither and are null when STATS is off.
        this.forceItemBattle.getResultCeremony().beginFor(
                this.matchHistory.getMatchId(), this.revealOrder());

        if (statsEnabled) {
            this.matchHistory.submit(placesMap, teamPlaces, this.forceItemBattle.getRoundClock().totalSeconds(),
                    this::evaluateCollectionAchievements);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                ForceItemPlayer forceItemPlayer = this.roster.get(onlinePlayer.getUniqueId());
                if (forceItemPlayer != null && !forceItemPlayer.isSpectator()) {
                    this.forceItemBattle.getAchievementManager().evaluateGlobalAchievements(onlinePlayer);
                }
            }
        }
    }

    /** Blocks travelled this round, summed over every distance statistic Bukkit tracks (all in cm). */
    private int calculateDistance(Player player) {
        int distance = Arrays.stream(Statistic.values())
                .filter(statistic -> statistic.name().contains("CM"))
                .mapToInt(player::getStatistic)
                .sum();

        return (int) Math.round((double) distance / 100);
    }

    /**
     * Called by /pause instead of setting the state directly, so the interval bookkeeping that
     * {@link #resumeGame()} subtracts from item times cannot be bypassed.
     */
    public void pauseGame() {
        this.matchHistory.onPaused();
        this.roundPhase.moveTo(GameState.PAUSED_GAME);
        this.clearMobTargets();
    }

    /**
     * Re-checks the collection achievement for every participant once the match is persisted, so it
     * sees a found-set that includes the round just played rather than trailing it by a game.
     */
    private void evaluateCollectionAchievements() {
        for (ForceItemPlayer participant : this.roster.players().values()) {
            if (participant.isSpectator()) {
                continue;
            }
            Player participantPlayer = participant.player();
            if (participantPlayer != null && participantPlayer.isOnline()) {
                this.forceItemBattle.getAchievementManager().evaluateCollectionAchievement(participantPlayer);
            }
        }
    }

    /**
     * Drops every mob's aggro at the moment of pause. The entity-target listener stops mobs locking
     * on <em>during</em> the pause, but one already chasing a player keeps its target and lands its
     * hit the instant the game resumes. Both halves are needed.
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
     * Closes the open pause interval. The closed [start, end] is kept so an item whose find-window
     * straddled the pause has that span removed from its seconds_taken at submit time.
     */
    public void resumeGame() {
        this.matchHistory.onResumed();
        this.roundPhase.moveTo(GameState.MID_GAME);
    }




}
