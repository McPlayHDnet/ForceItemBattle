package forceitembattle.util;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Trading {

    private ForceItemPlayer trader;
    private ForceItemPlayer opposite;
    @Getter
    private List<ItemStack> traderItems;
    @Getter
    private List<ItemStack> oppositeItems;

    public Trading(ForceItemPlayer trader, ForceItemPlayer opposite, List<ItemStack> traderItems, List<ItemStack> oppositeItems) {
        this.trader = trader;
        this.opposite = opposite;
        this.traderItems = traderItems;
        this.oppositeItems = oppositeItems;
    }

    public ForceItemPlayer getTrader(Player trader) {
        return trader == this.trader.player() ? this.trader : this.opposite;
    }

    public ForceItemPlayer getOpposite(Player opposite) {
        return opposite == this.trader.player() ? this.opposite : this.trader;
    }


}
