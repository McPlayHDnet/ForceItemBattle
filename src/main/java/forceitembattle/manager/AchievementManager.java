package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementMode;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.achievements.AchievementStorage;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.global.FoundItemsCache;
import forceitembattle.achievements.global.FoundItemsLoader;
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.achievements.global.GlobalStatsLoader;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.progress.AchievementProgressTracker;
import forceitembattle.achievements.progress.BackToBackAchievementProgress;
import forceitembattle.achievements.handlers.CollectionAchievementHandler;
import forceitembattle.achievements.progress.CollectionAchievementProgress;
import forceitembattle.achievements.handlers.ConsecutiveStoneAchievementHandler;
import forceitembattle.achievements.progress.ConsecutiveStoneAchievementProgress;
import forceitembattle.achievements.progress.CounterAchievementProgress;
import forceitembattle.achievements.progress.ItemFrequencyAchievementProgress;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.achievements.progress.SkipAchievementProgress;
import forceitembattle.achievements.progress.TimeAchievementProgress;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.gui.CollectionCategory;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class AchievementManager implements Manager {

    private final ForceItemBattle plugin;
    private final Map<UUID, Map<Achievements, AchievementProgressTracker>> playerProgress = new HashMap<>();
    private final Map<Team, Map<Achievements, AchievementProgressTracker>> teamProgress = new HashMap<>();
    private final AchievementStorage storage;
    @Getter
    private final GlobalStatsCache globalStatsCache;
    @Getter
    private final GlobalStatsLoader globalStatsLoader;
    @Getter
    private final FoundItemsCache foundItemsCache;
    @Getter
    private final FoundItemsLoader foundItemsLoader;

    // OPTIMIZATION: Pre-built map of achievements by trigger
    private final Map<Trigger, List<Achievements>> achievementsByTrigger;

    public AchievementManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.storage = new AchievementStorage(plugin);
        this.globalStatsCache = new GlobalStatsCache();
        this.globalStatsLoader = new GlobalStatsLoader(plugin, this.globalStatsCache);
        this.foundItemsCache = new FoundItemsCache();
        this.foundItemsLoader = new FoundItemsLoader(plugin, this.foundItemsCache);
        this.achievementsByTrigger = buildTriggerMap();
    }

    private Map<Trigger, List<Achievements>> buildTriggerMap() {
        Map<Trigger, List<Achievements>> map = new EnumMap<>(Trigger.class);

        for (Trigger trigger : Trigger.values()) {
            map.put(trigger, new ArrayList<>());
        }

        for (Achievements achievement : Achievements.values()) {
            // Only ROUND achievements are handler-driven. GLOBAL is evaluated against persisted
            // stats; META is evaluated after every unlock. Neither has a trigger.
            if (achievement.getScope() != AchievementScope.ROUND) {
                continue;
            }
            Trigger trigger = achievement.getHandler().getTrigger();
            map.get(trigger).add(achievement);
        }

        return map;
    }

    /**
     * Main event handler with trigger-based filtering
     */
    public void handleEvent(Player player, Event event, Trigger trigger) {
        UUID uuid = player.getUniqueId();

        if (!plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (!plugin.getSettings().isSettingEnabled(GameSetting.ACHIEVEMENTS)) {
            return;
        }

        ForceItemPlayer forceItemPlayer = plugin.getGamemanager().getForceItemPlayer(uuid);
        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            return;
        }

        // Initialize progress
        playerProgress.putIfAbsent(uuid, new HashMap<>());
        Map<Achievements, AchievementProgressTracker> progress = playerProgress.get(uuid);

        // Handle team progress
        Map<Achievements, AchievementProgressTracker> teamProgressMap = null;
        Team team = null;
        if (plugin.getSettings().isSettingEnabled(GameSetting.TEAM) && forceItemPlayer.currentTeam() != null) {
            team = forceItemPlayer.currentTeam();
            teamProgress.putIfAbsent(team, new HashMap<>());
            teamProgressMap = teamProgress.get(team);
        }

        // OPTIMIZATION: Only check achievements for this trigger
        List<Achievements> relevantAchievements = achievementsByTrigger.get(trigger);
        for (Achievements achievement : relevantAchievements) {
            AchievementHandler<?> handler = achievement.getHandler();

            // Determine team vs player progress
            boolean useTeamProgress = handler.isTeamEligible() &&
                    !handler.isPlayerBased() &&
                    teamProgressMap != null;

            // Skip if already has achievement
            if (storage.hasAchievement(uuid, achievement) && !useTeamProgress) {
                continue;
            }

            // For team achievements, once every teammate has it there's nothing left to do.
            if (useTeamProgress && team != null) {
                boolean allHaveIt = team.getPlayers().stream()
                        .allMatch(p -> storage.hasAchievement(p.player().getUniqueId(), achievement));
                if (allHaveIt) {
                    continue;
                }
            }

            // Get or create progress
            Map<Achievements, AchievementProgressTracker> progressMap = useTeamProgress ? teamProgressMap : progress;
            progressMap.putIfAbsent(achievement, handler.createProgress());
            AchievementProgressTracker tracker = progressMap.get(achievement);

            // Type-safe check
            @SuppressWarnings("unchecked")
            AchievementHandler<AchievementProgressTracker> typedHandler =
                    (AchievementHandler<AchievementProgressTracker>) handler;

            // Check if completed
            if (typedHandler.check(event, tracker, forceItemPlayer, plugin)) {
                grantAchievement(player, achievement, useTeamProgress, forceItemPlayer);
            }
        }
    }

    private void grantAchievement(Player player, Achievements achievement,
                                  boolean isTeamAchievement, ForceItemPlayer forceItemPlayer) {
        Team team = forceItemPlayer.currentTeam();
        boolean teamGame = plugin.getSettings().isSettingEnabled(GameSetting.TEAM) && team != null;

        // Grant to the triggering player.
        if (!storage.hasAchievement(player.getUniqueId(), achievement)) {
            writeUnlock(player.getUniqueId(), player, achievement, team, teamGame);
            checkMetaTiers(player.getUniqueId(), player, team, teamGame);
        }

        // Team-eligible achievements are also granted to the rest of the team.
        // (Which achievements are team-eligible vs player-only is decided by the
        // handler flags — that scoping is unchanged here.)
        if (isTeamAchievement && team != null) {
            for (ForceItemPlayer teamMember : team.getPlayers()) {
                UUID memberUuid = teamMember.player().getUniqueId();
                if (memberUuid.equals(player.getUniqueId())) {
                    continue;
                }
                // Don't re-grant (and therefore re-announce) to a teammate who already has it.
                if (storage.hasAchievement(memberUuid, achievement)) {
                    continue;
                }
                writeUnlock(memberUuid, teamMember.player(), achievement, team, teamGame);
                checkMetaTiers(memberUuid, teamMember.player(), team, teamGame);
            }
        }
    }

    /** Convenience for unlocks with no game context (e.g. a GLOBAL unlock at join). */
    public void checkMetaTiers(UUID playerUuid, Player player) {
        checkMetaTiers(playerUuid, player, null, false);
    }

    public void checkMetaTiers(UUID playerUuid, Player player, Team team, boolean teamGame) {
        boolean grantedAny = true;

        while (grantedAny) {
            grantedAny = false;

            for (Achievements achievement : Achievements.values()) {
                if (achievement.getScope() != AchievementScope.META) {
                    continue;
                }
                if (storage.hasAchievement(playerUuid, achievement)) {
                    continue;
                }
                if (!achievement.getCompletionistRule().isMet(plugin, playerUuid)) {
                    continue;
                }

                writeUnlock(playerUuid, player, achievement, team, teamGame);
                grantedAny = true;
            }
        }
    }

    public void evaluateGlobalAchievements(Player player) {
        if (!plugin.getSettings().isSettingEnabled(GameSetting.ACHIEVEMENTS)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // The cache is what de-dups unlocks; without it we'd re-grant (and re-announce) every time.
        if (!storage.isLoaded(uuid)) {
            return;
        }

        boolean anyOutstanding = false;
        for (Achievements achievement : Achievements.values()) {
            if (achievement.isGlobal() && !storage.hasAchievement(uuid, achievement)) {
                anyOutstanding = true;
                break;
            }
        }
        if (!anyOutstanding) {
            return;
        }

        globalStatsLoader.load(uuid, stats -> {
            if (!player.isOnline()) {
                return;
            }

            for (Achievements achievement : Achievements.values()) {
                if (!achievement.isGlobal() || storage.hasAchievement(uuid, achievement)) {
                    continue;
                }
                if (!stats.isMet(achievement.getGlobalRule())) {
                    continue;
                }
                // Always recorded SOLO with no teammate. A GLOBAL stat spans both modes, so the
                // mode of the unlock is meaningless — and recording it as TEAM at game-end but
                // SOLO at join would make the same achievement's mode depend on where it fired.
                writeUnlock(uuid, player, achievement, null, false);
            }

            checkMetaTiers(uuid, player);
        });
    }

    /**
     * Evaluates the COLLECTION achievement(s) for one player after a match has been persisted.
     * Called from the match submit's success callback, so the just-finished game is already in the
     * DB -- the achievement fills at conclusion rather than a game late.
     */
    public void evaluateCollectionAchievement(Player player) {
        UUID uuid = player.getUniqueId();

        boolean anyOutstanding = false;
        for (Achievements achievement : Achievements.values()) {
            if (achievement.getScope() == AchievementScope.COLLECTION && !storage.hasAchievement(uuid, achievement)) {
                anyOutstanding = true;
                break;
            }
        }
        if (!anyOutstanding) {
            return;
        }

        // The submit already invalidated the found-set; invalidate again right before the read to
        // close the window where a concurrent load could have re-cached the pre-match set.
        foundItemsCache.invalidate(uuid);
        foundItemsLoader.load(uuid, found -> {
            if (!player.isOnline()) {
                return;
            }
            Set<String> catalogue = getCollectionCatalogue();
            for (Achievements achievement : Achievements.values()) {
                if (achievement.getScope() != AchievementScope.COLLECTION || storage.hasAchievement(uuid, achievement)) {
                    continue;
                }
                if (!achievement.getCollectionRule().isMet(found.keySet(), catalogue)) {
                    continue;
                }
                // Recorded SOLO with no teammate, like the GLOBAL unlocks: a lifetime collection
                // spans both modes, so the unlock's mode is meaningless.
                writeUnlock(uuid, player, achievement, null, false);
            }
            checkMetaTiers(uuid, player);
        });
    }

    private Set<String> collectionCatalogue;

    public Set<String> getCollectionCatalogue() {
        if (this.collectionCatalogue == null) {
            this.collectionCatalogue = this.plugin.getItemDifficultiesManager().getCollectableItems().stream()
                    .map(material -> material.getKey().asString())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return this.collectionCatalogue;
    }

    // Catalogue split into display categories, sorted within each. Built once (session-static),
    // shared by the collection book and every category page so nothing re-buckets per open.
    private Map<CollectionCategory, List<Material>> collectionBuckets;

    public Map<CollectionCategory, List<Material>> getCollectionBuckets() {
        if (this.collectionBuckets == null) {
            Map<CollectionCategory, List<Material>> buckets = new EnumMap<>(CollectionCategory.class);
            for (CollectionCategory category : CollectionCategory.values()) {
                buckets.put(category, new ArrayList<>());
            }
            for (Material material : this.plugin.getItemDifficultiesManager().getCollectableItems()) {
                buckets.get(CollectionCategory.categoryOf(material)).add(material);
            }
            buckets.values().forEach(list -> list.sort(Comparator.comparing(material -> material.getKey().asString())));
            this.collectionBuckets = buckets;
        }
        return this.collectionBuckets;
    }

    /**
     * Persists one unlock (with the correct mode + teammate) and, if the player
     * is online, fires the grant event so downstream listeners (announcements,
     * Completionist) run. A player in a team game with no resolvable teammate
     * (e.g. a solo remnant of a team) is recorded as SOLO to keep service data
     * valid.
     */
    private void writeUnlock(UUID memberUuid, Player memberPlayer, Achievements achievement,
                             Team team, boolean teamGame) {
        UUID teammate = teamGame ? teammateOf(memberUuid, team) : null;
        AchievementMode mode = teammate != null ? AchievementMode.TEAM : AchievementMode.SOLO;

        storage.addAchievement(memberUuid, achievement, mode, teammate);

        if (memberPlayer != null && memberPlayer.isOnline()) {
            Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(memberPlayer, achievement));
        }
    }

    /**
     * The other member of a (two-player) team, or null if none can be resolved.
     */
    private UUID teammateOf(UUID memberUuid, Team team) {
        if (team == null) {
            return null;
        }
        for (ForceItemPlayer p : team.getPlayers()) {
            UUID uuid = p.player().getUniqueId();
            if (!uuid.equals(memberUuid)) {
                return uuid;
            }
        }
        return null;
    }

    /**
     * Check game-end achievements like Chicot (no deaths)
     */
    public void checkGameEndAchievements() {
        if (!plugin.getGamemanager().isEndGame()) {
            return;
        }

        if (!plugin.getSettings().isSettingEnabled(GameSetting.ACHIEVEMENTS)) {
            return;
        }

        boolean teamGameEnabled = plugin.getSettings().isSettingEnabled(GameSetting.TEAM);

        // Check all players
        for (UUID uuid : plugin.getGamemanager().forceItemPlayerMap().keySet()) {
            ForceItemPlayer fip = plugin.getGamemanager().getForceItemPlayer(uuid);
            if (fip == null || fip.isSpectator()) {
                continue;
            }

            Team team = fip.currentTeam();
            boolean teamGame = teamGameEnabled && team != null;

            // CHICOT — finish with no deaths.
            if (!storage.hasAchievement(uuid, Achievements.CHICOT)) {
                playerProgress.putIfAbsent(uuid, new HashMap<>());
                Map<Achievements, AchievementProgressTracker> progress = playerProgress.get(uuid);
                progress.putIfAbsent(Achievements.CHICOT, Achievements.CHICOT.getHandler().createProgress());
                if (progress.get(Achievements.CHICOT) instanceof SimpleAchievementProgress simpleProgress
                        && simpleProgress.deathCount == 0) {
                    // writeUnlock persists with the right mode/teammate and fires the
                    // event only if the player is online (so Completionist can chain).
                    writeUnlock(uuid, fip.player(), Achievements.CHICOT, team, teamGame);
                }
            }

            // THE_HARD_WAY — finish the game without a single back-to-back.
            if (!storage.hasAchievement(uuid, Achievements.THE_HARD_WAY)
                    && !hadOccurrence(uuid, Achievements.THE_HARD_WAY)) {
                writeUnlock(uuid, fip.player(), Achievements.THE_HARD_WAY, team, teamGame);
            }

            // NO_HANDOUTS — win the game without a single back-to-back.
            if (!storage.hasAchievement(uuid, Achievements.NO_HANDOUTS)
                    && !hadOccurrence(uuid, Achievements.NO_HANDOUTS)
                    && didWin(fip, teamGame)) {
                writeUnlock(uuid, fip.player(), Achievements.NO_HANDOUTS, team, teamGame);
            }

            // NO_SHORTCUTS — finish without entering the Antimatter Teleporter.
            if (!storage.hasAchievement(uuid, Achievements.NO_SHORTCUTS)
                    && !hadOccurrence(uuid, Achievements.NO_SHORTCUTS)) {
                writeUnlock(uuid, fip.player(), Achievements.NO_SHORTCUTS, team, teamGame);
            }

            // OVERWORLD_PURIST — finish without leaving the Overworld.
            if (!storage.hasAchievement(uuid, Achievements.IT_IS_BEAUTIFUL)
                    && !hadOccurrence(uuid, Achievements.IT_IS_BEAUTIFUL)) {
                writeUnlock(uuid, fip.player(), Achievements.IT_IS_BEAUTIFUL, team, teamGame);
            }
        }
    }

    /**
     * True if this achievement's tracker recorded at least one occurrence this
     * round. The tracker is the shared team tracker for team-eligible achievements,
     * so it reflects whether either teammate triggered it. A missing tracker means
     * it never fired.
     */
    private boolean hadOccurrence(UUID uuid, Achievements achievement) {
        return getProgress(uuid, achievement) instanceof SimpleAchievementProgress simpleProgress
                && simpleProgress.count > 0;
    }

    /**
     * True if the player (solo) or their team (teams) finished in 1st place.
     * Ties for 1st count as a win.
     */
    private boolean didWin(ForceItemPlayer fip, boolean teamGame) {
        var gamemanager = plugin.getGamemanager();
        if (teamGame && fip.currentTeam() != null) {
            List<Team> teams = gamemanager.forceItemPlayerMap().values().stream()
                    .map(ForceItemPlayer::currentTeam)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Integer place = gamemanager.calculatePlaces(teams).get(fip.currentTeam());
            return place != null && place == 1;
        }
        Integer place = gamemanager.calculatePlaces(gamemanager.forceItemPlayerMap()).get(fip);
        return place != null && place == 1;
    }

    public void resetProgress() {
        playerProgress.clear();
        teamProgress.clear();
    }

    /**
     * The live progress tracker for a player+achievement in the current round,
     * or null if none exists yet. Checks the player's own progress and, for
     * team-shared achievements, the team's progress.
     */
    public AchievementProgressTracker getProgress(UUID uuid, Achievements achievement) {
        Map<Achievements, AchievementProgressTracker> playerMap = playerProgress.get(uuid);
        if (playerMap != null && playerMap.get(achievement) != null) {
            return playerMap.get(achievement);
        }
        ForceItemPlayer fip = plugin.getGamemanager().getForceItemPlayer(uuid);
        if (fip != null && fip.currentTeam() != null) {
            Map<Achievements, AchievementProgressTracker> teamMap = teamProgress.get(fip.currentTeam());
            if (teamMap != null) {
                return teamMap.get(achievement);
            }
        }
        return null;
    }

    /**
     * Human-readable description of how far along a player's achievement is,
     * for debugging (e.g. a /achievements progress command). Progress is
     * in-memory and per-round, so this only reflects the current game.
     */
    public String describeProgress(UUID uuid, Achievements achievement) {
        AchievementProgressTracker tracker = getProgress(uuid, achievement);
        if (tracker == null) {
            return "not started";
        }

        if (tracker instanceof CollectionAchievementProgress<?> collection) {
            AchievementHandler<?> handler = achievement.getHandler();
            if (handler instanceof CollectionAchievementHandler<?> collectionAchievementHandler) {
                Set<?> required = collectionAchievementHandler.getRequiredItems();
                Set<Object> missing = new HashSet<>(required);
                missing.removeAll(collection.collected);
                String base = collection.collected.size() + "/" + required.size() + " collected";
                return missing.isEmpty() ? base + " (complete)" : base + ", missing: " + missing;
            }
            return collection.collected.size() + " collected: " + collection.collected;
        }
        if (tracker instanceof CounterAchievementProgress counter) {
            return "count=" + counter.count + ", consecutive=" + counter.consecutiveCount;
        }
        if (tracker instanceof ItemFrequencyAchievementProgress frequency) {
            int highest = frequency.counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            return "highest same-item count=" + highest;
        }
        if (tracker instanceof TimeAchievementProgress time) {
            return "count=" + time.count + ", hasSkipped=" + time.hasSkipped;
        }
        if (tracker instanceof SkipAchievementProgress skip) {
            return "skips=" + skip.skipCount;
        }
        if (tracker instanceof BackToBackAchievementProgress backToBack) {
            return "backToBack=" + backToBack.b2bCount;
        }
        if (tracker instanceof ConsecutiveStoneAchievementProgress stone) {
            return "consecutiveStone=" + stone.consecutiveCount;
        }
        if (tracker instanceof SimpleAchievementProgress simple) {
            return "count=" + simple.count + (simple.deathCount != 0 ? ", deaths=" + simple.deathCount : "");
        }
        return "in progress";
    }

    public AchievementStorage getAchievementStorage() {
        return storage;
    }
}
