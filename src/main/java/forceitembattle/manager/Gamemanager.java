package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.Dimension;
import forceitembattle.model.GameContext;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.service.MatchHistoryReporter;
import forceitembattle.service.PlayerStatsWrite;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundSetup;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.model.Standings;
import forceitembattle.model.ScoreOwner;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    private final ForceItemBattle forceItemBattle;
    private final Roster roster;
    /** End-of-game result screens, keyed by player / by team. Paged: page → slot → stack. */
    private final Map<UUID, Map<Integer, Map<Integer, ItemStack>>> savedInventory = new HashMap<>();
    private final Map<Team, Map<Integer, Map<Integer, ItemStack>>> savedInventoryTeam = new HashMap<>();

    /**
     * Match telemetry. Owns everything that only exists to be reported: the match id, lead changes,
     * pause intervals, and the submit/link-broadcast flow.
     */
    @Getter
    private final MatchHistoryReporter matchHistory;

    private final RoundPhase roundPhase;
    @Getter
    @Setter
    private long gameStartTime;

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

    public Gamemanager(ForceItemBattle forceItemBattle, Roster roster, RoundPhase roundPhase) {
        this.forceItemBattle = forceItemBattle;
        this.roster = roster;
        this.roundPhase = roundPhase;


        this.matchHistory = new MatchHistoryReporter(forceItemBattle);
    }

    /**
     * Re-evaluates who is in front and records a lead change if the sole leader's identity moved.
     * Called from the one place a score can change.
     */
    public void evaluateLead() {
        this.matchHistory.recordStandings(this.currentSoleLeader());
    }

    /**
     * Identity of the unique highest scorer, or null when the top score is shared. Team mode keys
     * on team id, solo on player UUID; the two never mix within a game.
     *
     * <p>Spectators are skipped, matching who gets written as a participant at submit time --
     * otherwise a spectator sitting on 0 could create a phantom tie in a one-player game.
     *
     * <p><b>The one branch on the TEAM setting that is not a mistake</b>, and
     * {@code CONTEXT.md § Round Phase} says why. Left as it is deliberately.
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

        // One pass over the roster, one pair per score owner. Written as separate team and solo
        // halves once, and they had drifted: the solo half walked getOnlinePlayers() rather than
        // the roster, so a player who disconnected during the countdown was never dealt an item;
        // the team half drew a fresh pair per member and let the last one win. Both fixed by the
        // collapse -- keep it one pass.
        this.roster.activeScoreOwners().forEach(owner -> {
            MaterialPair pair = runMode ? shared : this.nextMaterials(false);
            owner.startRound(pair.current(), pair.next(), now);
        });
    }


    /**
     * Moves whoever this find belongs to onto their next item.
     *
     * <p>Run mode is the one distinction left here, and it is a real one: everybody races for the
     * same seeded item, so one find advances the whole server. Otherwise only the finder's own
     * owner moves. Team versus solo no longer appears — it is the owner's business, not this
     * method's.
     */
    public void advanceMaterials(ForceItemPlayer forceItemPlayer, GameContext context) {
        long now = System.currentTimeMillis();

        if (context.runMode()) {
            Material nextMaterial = this.generateSeededMaterial();
            // Once per owner. The team branch this replaces ran per member, which advanced a
            // two-player team twice: the queued item was skipped over and current and next both
            // ended up holding nextMaterial.
            this.roster.activeScoreOwners().forEach(owner -> owner.advance(nextMaterial, now));
            return;
        }

        forceItemPlayer.scoreOwner().advance(this.generateMaterial(), now);
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

    /**
     * Replaces the current item for the whole server with a freshly generated one — what /voteskip
     * does when the vote carries, and what /skip does as an admin override.
     *
     * Charging a joker is <em>not</em> part of this: the only caller that costs one (the vote)
     * spends it itself, on the initiator. Do not charge one inside the per-player loop below —
     * it runs once per non-spectator, so the initiator would pay a joker for each of them.
     */
    public void forceSkipItem(Player player) {
        if (!this.roster.contains(player.getUniqueId())) {
            return;
        }

        boolean runMode = this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.RUN);

        MaterialPair pair = this.nextMaterials(runMode);

        // Everyone gets the same replacement pair, so this is one pair handed to every owner.
        // Spectators are already out of activeScoreOwners(), which is what keeps a countdown
        // joiner -- who holds no team -- from NPE-ing every skip for the rest of the round.
        this.roster.activeScoreOwners().forEach(owner -> owner.assignMaterials(pair.current(), pair.next()));
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
     * <p>Here rather than inside /start because the countdown-end pass only walks the players
     * online at that instant. Someone who disconnected during the countdown is still a participant,
     * so the same setup has to run when they come back — otherwise they rejoin in ADVENTURE mode
     * with an empty inventory, unable to play the round they are scored in.
     */
    public void applyStartSetup(Player player) {
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());

        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
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
     * Everything that has to happen the instant /start's countdown reaches zero: the round's
     * identity, the world reset, per-player setup, and the flip to MID_GAME.
     *
     * Both halves of a round's lifecycle belong here: /start only parses arguments and runs the
     * countdown, then hands over. Keep orchestration out of the command.
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
     * Splits the round's joker pool across each team's members.
     *
     * The share is computed for the whole team either way, so the split stays stable; a member who
     * is offline right now simply gets no stack here and is handed the team's remaining pool by
     * {@link #applyStartSetup(Player)} when they rejoin.
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

        Bukkit.getOnlinePlayers().forEach(player -> {
            try {
                ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
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
                    Team currentTeam = forceItemPlayer.currentTeam();

                    // Who came first is this class's business; which rows that lands on is not.
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
        int distance = 0;

        for (Statistic statistics : Statistic.values()) {
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









    /**
     * Begin a pause: flip state and record when it started, so its duration can be subtracted from
     * item times on resume. Called by /pause instead of setting the state directly, so the timing
     * bookkeeping cannot be bypassed.
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
        this.matchHistory.onResumed();
        this.roundPhase.moveTo(GameState.MID_GAME);
    }




}
