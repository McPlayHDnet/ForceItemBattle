package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

public class TradingManager implements Manager {

    public static final String PREFIX = "<dark_gray>» <green>Trade <dark_gray>┃ ";
    private final ForceItemBattle forceItemBattle;
    private final Map<ForceItemPlayer, ForceItemPlayer> pendingTradeRequests;
    @Getter
    private final Map<ForceItemPlayer, ForceItemPlayer> tradingPlayers;

    public TradingManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.pendingTradeRequests = new ConcurrentHashMap<>();
        this.tradingPlayers = new HashMap<>();
    }

    public boolean hasInvite(ForceItemPlayer player) {
        return this.pendingTradeRequests.containsKey(player);
    }

    public void sendTradeRequest(ForceItemPlayer player, ForceItemPlayer target) {
        if (this.hasInvite(target)) {
            //already got an invite
            return;
        }


    }
}
