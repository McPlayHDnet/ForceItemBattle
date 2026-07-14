package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.model.ActiveTrader;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.TraderKind;
import forceitembattle.util.Scheduler;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantInventory;

@RequiredArgsConstructor
public class TradeListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler(ignoreCancelled = true)
    public void onInteractTrader(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof WanderingTrader entity)) return;

        WanderingTraderManager manager = this.plugin.getWanderingTraderManager();
        ActiveTrader trader = manager.getTrader(entity.getUniqueId());
        if (trader == null) return;

        // Suppress vanilla's single-occupancy merchant and hand out a private one instead.
        event.setCancelled(true);

        Player player = event.getPlayer();
        player.openMerchant(manager.createMerchantFor(player, trader), true);
    }

    @EventHandler
    public void onCloseTrader(InventoryCloseEvent event) {
        this.plugin.getWanderingTraderManager().stopTrading(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPurchase(PlayerPurchaseEvent event) {
        Player player = event.getPlayer();
        ActiveTrader trader = this.plugin.getWanderingTraderManager().traderOf(player.getUniqueId());

        if (trader == null) return;

        // The wandering trader's wheel offer has unlimited uses, so it keeps its own one-per-spawn gate.
        if (trader.getKind() == TraderKind.WANDERING
                && CustomMaterials.WHEEL_OF_FORTUNE.matches(event.getTrade().getResult())) {

            Boolean canBuy = trader.getCanBuyWheel().get(player.getUniqueId());
            if (canBuy != null && !canBuy) {
                event.setCancelled(true);
                return;
            }

            trader.getCanBuyWheel().put(player.getUniqueId(), Boolean.FALSE);
            Scheduler.runLaterSync(player::closeInventory, 1L);
        }

        int recipeIndex = this.selectedRecipeIndex(player);
        if (recipeIndex >= 0) {
            trader.recordUse(player.getUniqueId(), recipeIndex);
        }
    }

    private int selectedRecipeIndex(Player player) {
        if (player.getOpenInventory().getTopInventory() instanceof MerchantInventory merchantInventory) {
            return merchantInventory.getSelectedRecipeIndex();
        }
        return -1;
    }
}
