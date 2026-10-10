package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.handlers.CountingAchievementHandler.Occurrence;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/** "Finish the game without doing this." Never completes mid-game; unlocked at game end while the tally is zero. */
public final class TallyAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final Trigger trigger;
    private final Occurrence occurrence;
    private final boolean teamEligible;
    private final boolean requiresWin;

    private TallyAchievementHandler(Trigger trigger, boolean teamEligible, Occurrence occurrence) {
        this(trigger, teamEligible, false, occurrence);
    }

    private TallyAchievementHandler(Trigger trigger, boolean teamEligible, boolean requiresWin, Occurrence occurrence) {
        this.trigger = trigger;
        this.teamEligible = teamEligible;
        this.requiresWin = requiresWin;
        this.occurrence = occurrence;
    }

    /** The same tally, unlocked only by a player who also won. */
    public TallyAchievementHandler requiringWin() {
        return new TallyAchievementHandler(this.trigger, this.teamEligible, true, this.occurrence);
    }

    public boolean requiresWin() {
        return this.requiresWin;
    }

    public static TallyAchievementHandler noBackToBacks() {
        return new TallyAchievementHandler(Trigger.BACK_TO_BACK, Trigger.BACK_TO_BACK.isAchieveableInTeams(),
                (event, player, world) -> event instanceof FoundItemEvent found && found.isBackToBack());
    }

    public static TallyAchievementHandler noAntimatterTeleporter() {
        return new TallyAchievementHandler(Trigger.ANTIMATTER_TELEPORTER, true,
                (event, player, world) -> event instanceof AntimatterTeleporterUseEvent);
    }

    public static TallyAchievementHandler noDeaths() {
        return new TallyAchievementHandler(Trigger.DYING, Trigger.DYING.isAchieveableInTeams(),
                (event, player, world) -> event instanceof PlayerDeathEvent);
    }

    public static TallyAchievementHandler noOverworldExit() {
        return new TallyAchievementHandler(Trigger.VISIT, true,
                (event, player, world) -> event instanceof PlayerChangedWorldEvent change
                        && !Dimension.isOverworld(change.getPlayer()));
    }

    @Override
    public Trigger getTrigger() {
        return this.trigger;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (this.occurrence.test(event, forceItemPlayer, world)) {
            progress.count++;
        }
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }

    @Override
    public boolean isTeamEligible() {
        return this.teamEligible;
    }
}
