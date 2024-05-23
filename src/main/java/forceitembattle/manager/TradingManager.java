package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Teams;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingManager {

    private final ForceItemBattle forceItemBattle;

    private final Map<ForceItemPlayer, ForceItemPlayer> pendingTradeRequests;
    @Getter
    private final Map<ForceItemPlayer, ForceItemPlayer> tradingPlayers;

    public static final String PREFIX = "<dark_gray>» <green>Trade <dark_gray>┃ ";

    public TradingManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.pendingTradeRequests = new ConcurrentHashMap<>();
        this.tradingPlayers = new HashMap<>();
    }

    public boolean hasInvite(ForceItemPlayer player) {
        return this.pendingTradeRequests.containsKey(player);
    }

    public void sendTradeRequest(ForceItemPlayer player, ForceItemPlayer target) {
        if(this.hasInvite(target)) {
            //already got an invite
            return;
        }


    }
}
