package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.achievements.AchievementMode;
import forceitembattle.achievements.AchievementStorage;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.handlers.BackToBackProgress;
import forceitembattle.achievements.handlers.CollectionHandler;
import forceitembattle.achievements.handlers.CollectionProgress;
import forceitembattle.achievements.handlers.ConsecutiveStoneHandler;
import forceitembattle.achievements.handlers.CounterProgress;
import forceitembattle.achievements.handlers.ProgressTracker;
import forceitembattle.achievements.handlers.SimpleProgress;
import forceitembattle.achievements.handlers.SkipProgress;
import forceitembattle.achievements.handlers.TimeProgress;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.*;

public class AchievementManager {

    private final ForceItemBattle plugin;
    private final Map<UUID, Map<Achievements, ProgressTracker>> playerProgress = new HashMap<>();
    private final Map<Team, Map<Achievements, ProgressTracker>> teamProgress = new HashMap<>();
    private final AchievementStorage storage;

    // OPTIMIZATION: Pre-built map of achievements by trigger
    private final Map<Trigger, List<Achievements>> achievementsByTrigger;

    public AchievementManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.storage = new AchievementStorage(plugin);
        this.achievementsByTrigger = buildTriggerMap();
    }

    private Map<Trigger, List<Achievements>> buildTriggerMap() {
        Map<Trigger, List<Achievements>> map = new EnumMap<>(Trigger.class);

        for (Trigger trigger : Trigger.values()) {
            map.put(trigger, new ArrayList<>());
        }

        for (Achievements achievement : Achievements.values()) {
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

        ForceItemPlayer forceItemPlayer = plugin.getGamemanager().getForceItemPlayer(uuid);
        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            return;
        }

        // Initialize progress
        playerProgress.putIfAbsent(uuid, new HashMap<>());
        Map<Achievements, ProgressTracker> progress = playerProgress.get(uuid);

        // Handle team progress
        Map<Achievements, ProgressTracker> teamProgressMap = null;
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
            if (storage.hasAchievement(uuid, achievement)) {
                continue;
            }

            // For team achievements, skip if all have it
            if (useTeamProgress && team != null) {
                boolean allHaveIt = team.getPlayers().stream()
                        .allMatch(p -> storage.hasAchievement(p.player().getUniqueId(), achievement));
                if (allHaveIt) {
                    continue;
                }
            }

            // Get or create progress
            Map<Achievements, ProgressTracker> progressMap = useTeamProgress ? teamProgressMap : progress;
            progressMap.putIfAbsent(achievement, handler.createProgress());
            ProgressTracker tracker = progressMap.get(achievement);

            // Type-safe check
            @SuppressWarnings("unchecked")
            AchievementHandler<ProgressTracker> typedHandler =
                    (AchievementHandler<ProgressTracker>) handler;

            // Check if completed
            if (typedHandler.check(event, tracker, forceItemPlayer)) {
                grantAchievement(player, achievement, useTeamProgress, forceItemPlayer);
            }
        }
    }

    private void grantAchievement(Player player, Achievements achievement,
                                  boolean isTeamAchievement, ForceItemPlayer forceItemPlayer) {
        Team team = forceItemPlayer.currentTeam();
        boolean teamGame = plugin.getSettings().isSettingEnabled(GameSetting.TEAM) && team != null;

        // Grant to the triggering player.
        writeUnlock(player.getUniqueId(), player, achievement, team, teamGame);

        // Team-eligible achievements are also granted to the rest of the team.
        // (Which achievements are team-eligible vs player-only is decided by the
        // handler flags — that scoping is unchanged here.)
        if (isTeamAchievement && team != null) {
            for (ForceItemPlayer teamMember : team.getPlayers()) {
                UUID memberUuid = teamMember.player().getUniqueId();
                if (!memberUuid.equals(player.getUniqueId())) {
                    writeUnlock(memberUuid, teamMember.player(), achievement, team, teamGame);
                }
            }
        }
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

        boolean teamGameEnabled = plugin.getSettings().isSettingEnabled(GameSetting.TEAM);

        // Check all players
        for (UUID uuid : plugin.getGamemanager().forceItemPlayerMap().keySet()) {
            ForceItemPlayer fip = plugin.getGamemanager().getForceItemPlayer(uuid);
            if (fip == null || fip.isSpectator()) {
                continue;
            }

            // Skip if already has Chicot
            if (storage.hasAchievement(uuid, Achievements.CHICOT)) {
                continue;
            }

            // Initialize progress if not exists
            playerProgress.putIfAbsent(uuid, new HashMap<>());
            Map<Achievements, ProgressTracker> progress = playerProgress.get(uuid);

            // Get or create progress tracker for Chicot
            progress.putIfAbsent(Achievements.CHICOT, Achievements.CHICOT.getHandler().createProgress());
            ProgressTracker tracker = progress.get(Achievements.CHICOT);

            // Check death count
            if (tracker instanceof SimpleProgress simpleProgress) {
                if (simpleProgress.deathCount == 0) {
                    Team team = fip.currentTeam();
                    boolean teamGame = teamGameEnabled && team != null;
                    // writeUnlock persists with the right mode/teammate and fires
                    // the event only if the player is online (so Completionist can
                    // still chain); offline players are simply persisted.
                    writeUnlock(uuid, fip.player(), Achievements.CHICOT, team, teamGame);
                }
            }
        }
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
    public ProgressTracker getProgress(UUID uuid, Achievements achievement) {
        Map<Achievements, ProgressTracker> playerMap = playerProgress.get(uuid);
        if (playerMap != null && playerMap.get(achievement) != null) {
            return playerMap.get(achievement);
        }
        ForceItemPlayer fip = plugin.getGamemanager().getForceItemPlayer(uuid);
        if (fip != null && fip.currentTeam() != null) {
            Map<Achievements, ProgressTracker> teamMap = teamProgress.get(fip.currentTeam());
            if (teamMap != null) {
                return teamMap.get(achievement);
            }
        }
        return null;
    }

    /**
     * description of how far along a player's achievement is,
     * for debugging (e.g. a /achievements progress command). Progress is
     * in-memory and per-round, so this only reflects the current game.
     */
    public String describeProgress(UUID uuid, Achievements achievement) {
        ProgressTracker tracker = getProgress(uuid, achievement);
        if (tracker == null) {
            return "not started";
        }

        if (tracker instanceof CollectionProgress<?> collection) {
            AchievementHandler<?> handler = achievement.getHandler();
            if (handler instanceof CollectionHandler<?> collectionHandler) {
                Set<?> required = collectionHandler.getRequiredItems();
                Set<Object> missing = new HashSet<>(required);
                missing.removeAll(collection.collected);
                String base = collection.collected.size() + "/" + required.size() + " collected";
                return missing.isEmpty() ? base + " (complete)" : base + ", missing: " + missing;
            }
            return collection.collected.size() + " collected: " + collection.collected;
        }
        if (tracker instanceof CounterProgress counter) {
            return "count=" + counter.count + ", consecutive=" + counter.consecutiveCount;
        }
        if (tracker instanceof TimeProgress time) {
            return "count=" + time.count + ", hasSkipped=" + time.hasSkipped;
        }
        if (tracker instanceof SkipProgress skip) {
            return "skips=" + skip.skipCount;
        }
        if (tracker instanceof BackToBackProgress backToBack) {
            return "backToBack=" + backToBack.b2bCount;
        }
        if (tracker instanceof ConsecutiveStoneHandler.Progress stone) {
            return "consecutiveStone=" + stone.consecutiveCount;
        }
        if (tracker instanceof SimpleProgress simple) {
            return "count=" + simple.count + (simple.deathCount != 0 ? ", deaths=" + simple.deathCount : "");
        }
        return "in progress";
    }

    public AchievementStorage getAchievementStorage() {
        return storage;
    }

    public AchievementStorage getStorage() {
        return storage;
    }
}