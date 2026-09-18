package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.model.ForceItemPlayer;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import org.bukkit.event.Event;

public class TradingAchievementHandler extends CountingAchievementHandler {

    public TradingAchievementHandler(int targetAmount) {
        super(targetAmount);
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.TRADING;
    }

    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        return event instanceof PlayerPurchaseEvent purchaseEvent
                && world.isTrading(purchaseEvent.getPlayer().getUniqueId());
    }
}
