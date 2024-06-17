package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class RecipeListener implements Listener {

    private final ForceItemBattle forceItemBattle;
    
    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (this.forceItemBattle.getRecipeManager().isShowingRecipe(event.getPlayer())) {
            this.forceItemBattle.getRecipeManager().handleRecipeClose(event.getPlayer());
        }
    }
}
