package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;

/**
 * Handler for trading-based achievements
 */
public class TradingAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;

    public TradingAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.TRADING;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof PlayerTradeEvent tradeEvent)) {
            return false;
        }

        if (tradeEvent.getVillager() instanceof WanderingTrader) {
            progress.count++;
            return progress.count >= targetAmount;
        }
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}