package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.util.Scheduler;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

@RequiredArgsConstructor
public class TradeListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler(ignoreCancelled = true)
    public void onInteractTrader(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof WanderingTrader trader)) return;

        WanderingTraderManager manager = this.plugin.getWanderingTraderManager();
        if (!trader.getUniqueId().equals(manager.getTraderUuid())) return;

        // Suppress vanilla's single-occupancy merchant and hand out a private one instead.
        event.setCancelled(true);

        Player player = event.getPlayer();
        player.openMerchant(manager.createMerchantFor(player), true);
    }

    @EventHandler
    public void onCloseTrader(InventoryCloseEvent event) {
        this.plugin.getWanderingTraderManager().getTradingPlayers()
                .remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPurchase(PlayerPurchaseEvent event) {
        Player player = event.getPlayer();
        WanderingTraderManager manager = this.plugin.getWanderingTraderManager();

        if (!manager.getTradingPlayers().contains(player.getUniqueId())) return;
        if (event.getTrade().getResult().getType() != Material.NETHER_STAR) return;

        Boolean canBuy = manager.getCanBuyWheel().get(player.getUniqueId());
        if (canBuy == null || canBuy) {
            manager.getCanBuyWheel().put(player.getUniqueId(), Boolean.FALSE);
            Scheduler.runLaterSync(player::closeInventory, 1L);
        } else {
            event.setCancelled(true);
        }
    }
}
