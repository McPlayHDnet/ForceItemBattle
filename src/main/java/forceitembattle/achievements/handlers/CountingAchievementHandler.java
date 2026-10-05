package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.CustomItemSpec;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/** "Do this N times." Counting before comparing is what makes the last occurrence the one that unlocks. */
public final class CountingAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final Trigger trigger;
    private final int targetAmount;
    private final Occurrence occurrence;
    private final boolean playerBased;

    private CountingAchievementHandler(Trigger trigger, int targetAmount, boolean playerBased, Occurrence occurrence) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.trigger = trigger;
        this.targetAmount = targetAmount;
        this.playerBased = playerBased;
        this.occurrence = occurrence;
    }

    public static CountingAchievementHandler backToBacks(int targetAmount) {
        return new CountingAchievementHandler(Trigger.BACK_TO_BACK, targetAmount, false,
                (event, player, world) -> event instanceof FoundItemEvent found && found.isBackToBack());
    }

    public static CountingAchievementHandler wheelsOfFortune(int targetAmount) {
        return new CountingAchievementHandler(Trigger.WHEEL_OF_FORTUNE, targetAmount, false,
                (event, player, world) -> event instanceof WheelOfFortuneWinEvent);
    }

    public static CountingAchievementHandler newAntimatterTeleporters(int targetAmount) {
        return new CountingAchievementHandler(Trigger.ANTIMATTER_TELEPORTER, targetAmount, false,
                (event, player, world) -> event instanceof AntimatterTeleporterUseEvent use && use.isNewTeleporter());
    }

    public static CountingAchievementHandler trades(int targetAmount) {
        return new CountingAchievementHandler(Trigger.TRADING, targetAmount, false,
                (event, player, world) -> event instanceof PlayerPurchaseEvent purchase
                        && world.isTrading(purchase.getPlayer().getUniqueId()));
    }

    /** @param requiredItem null means any consumable counts */
    public static CountingAchievementHandler eats(int targetAmount, CustomItemSpec requiredItem) {
        return new CountingAchievementHandler(Trigger.EATING, targetAmount, false,
                (event, player, world) -> event instanceof PlayerItemConsumeEvent consume
                        && (requiredItem == null || requiredItem.matches(consume.getItem())));
    }

    public static CountingAchievementHandler rareMobDrops(int targetAmount) {
        return new CountingAchievementHandler(Trigger.MOB_DEATH, targetAmount, true,
                (event, player, world) -> {
                    if (!(event instanceof EntityDeathEvent death)) {
                        return false;
                    }
                    Material rareDrop = switch (death.getEntityType()) {
                        case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
                        case DROWNED -> Material.TRIDENT;
                        default -> null;
                    };
                    // The mob must have actually rolled the rare drop this death.
                    return rareDrop != null && death.getDrops().stream().anyMatch(stack -> stack.getType() == rareDrop);
                });
    }

    @Override
    public Trigger getTrigger() {
        return this.trigger;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!this.occurrence.test(event, forceItemPlayer, world)) {
            return false;
        }
        progress.count++;
        return progress.count >= this.targetAmount;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }

    @Override
    public boolean isPlayerBased() {
        return this.playerBased;
    }

    @FunctionalInterface
    public interface Occurrence {
        boolean test(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world);
    }
}
