package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;

/**
 * Handler for trading-based achievements
 */
public class TradingHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public TradingHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
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
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}