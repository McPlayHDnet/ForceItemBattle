package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.AchievementStorage;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.settings.achievements.handlers.AchievementHandler;
import forceitembattle.settings.achievements.handlers.ProgressTracker;
import forceitembattle.settings.achievements.handlers.SimpleProgress;
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

    /**
     * Pre-builds a map of achievements grouped by trigger for faster lookups
     */
    private Map<Trigger, List<Achievements>> buildTriggerMap() {
        Map<Trigger, List<Achievements>> map = new EnumMap<>(Trigger.class);

        // Initialize empty lists for all triggers
        for (Trigger trigger : Trigger.values()) {
            map.put(trigger, new ArrayList<>());
        }

        // Group achievements by their trigger
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
        storage.addAchievement(player.getUniqueId(), achievement);
        Bukkit.getPluginManager().callEvent(new PlayerGrantAchievementEvent(player, achievement));

        if (isTeamAchievement && forceItemPlayer.currentTeam() != null) {
            for (ForceItemPlayer teamMember : forceItemPlayer.currentTeam().getPlayers()) {
                if (!teamMember.player().getUniqueId().equals(forceItemPlayer.player().getUniqueId())) {
                    storage.addAchievement(teamMember.player().getUniqueId(), achievement);
                    Bukkit.getPluginManager().callEvent(
                            new PlayerGrantAchievementEvent(teamMember.player(), achievement)
                    );
                }
            }
        }
    }

    /**
     * Check game-end achievements like Chicot (no deaths)
     */
    public void checkGameEndAchievements() {
        if (!plugin.getGamemanager().isEndGame()) {
            return;
        }

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
                    Player player = fip.player();
                    if (player != null && player.isOnline()) {
                        Bukkit.getPluginManager().callEvent(
                                new PlayerGrantAchievementEvent(player, Achievements.CHICOT)
                        );
                    } else {
                        storage.addAchievement(uuid, Achievements.CHICOT);
                    }
                }
            }
        }
    }

    public void resetProgress() {
        playerProgress.clear();
        teamProgress.clear();
    }

    public AchievementStorage getAchievementStorage() {
        return storage;
    }

    public AchievementStorage getStorage() {
        return storage;
    }
}