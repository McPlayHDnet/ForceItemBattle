package forceitembattle.listener;

import forceitembattle.manager.RecipeManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class RecipeListener implements Listener {
    private final RecipeManager recipeManager;
    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (this.recipeManager.isShowingRecipe(event.getPlayer())) {
            this.recipeManager.handleRecipeClose(event.getPlayer());
        }
    }
}
