package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.achievements.AchievementMode;
import forceitembattle.achievements.AchievementStorage;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.handlers.BackToBackAchievementProgress;
import forceitembattle.achievements.handlers.CollectionAchievementHandler;
import forceitembattle.achievements.handlers.CollectionAchievementProgress;
import forceitembattle.achievements.handlers.ConsecutiveStoneAchievementHandler;
import forceitembattle.achievements.handlers.CounterAchievementProgress;
import forceitembattle.achievements.handlers.ItemFrequencyAchievementProgress;
import forceitembattle.achievements.handlers.AchievementProgressTracker;
import forceitembattle.achievements.handlers.SimpleAchievementProgress;
import forceitembattle.achievements.handlers.SkipAchievementProgress;
import forceitembattle.achievements.handlers.TimeAchievementProgress;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.*;

public class AchievementManager implements Manager {

    private final ForceItemBattle plugin;
    private final Map<UUID, Map<Achievements, AchievementProgressTracker>> playerProgress = new HashMap<>();
    private final Map<Team, Map<Achievements, AchievementProgressTracker>> teamProgress = new HashMap<>();
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
        if (tracker instanceof ConsecutiveStoneAchievementHandler.AchievementProgress stone) {
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