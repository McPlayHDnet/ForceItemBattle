package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import io.papermc.paper.event.player.PlayerTradeEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class TradeListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onTrade(PlayerTradeEvent playerTradeEvent) {
        Player player = playerTradeEvent.getPlayer();
        if (playerTradeEvent.getVillager() instanceof WanderingTrader wanderingTrader) {
            if (playerTradeEvent.getTrade().getResult().getType() != Material.NETHER_STAR) return;

            Boolean canBuy = this.plugin.getWanderingTraderManager().getCanBuyWheel().get(player.getUniqueId());
            if (canBuy == null || canBuy) {
                this.plugin.getWanderingTraderManager().getCanBuyWheel().put(player.getUniqueId(), Boolean.FALSE);
                player.closeInventory();
            } else {
                playerTradeEvent.setCancelled(true);
            }
        }
    }
}
