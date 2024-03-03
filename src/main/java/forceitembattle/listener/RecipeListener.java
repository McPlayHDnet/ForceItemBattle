package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class RecipeListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public RecipeListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getServer().getPluginManager().registerEvents(this, this.forceItemBattle);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (this.forceItemBattle.getRecipeManager().isShowingRecipe(event.getPlayer())) {
            this.forceItemBattle.getRecipeManager().handleRecipeClose(event.getPlayer());
        }
    }
}
