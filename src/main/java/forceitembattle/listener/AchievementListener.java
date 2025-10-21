package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.achievements.Trigger;
import io.papermc.paper.event.player.PlayerTradeEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@RequiredArgsConstructor
public class AchievementListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler(priority = EventPriority.MONITOR) // Changed to MONITOR so it runs AFTER Listeners.updateMaterials()
    public void onItemObtain(FoundItemEvent event) {
        Player player = event.getPlayer();

        if (!event.isBackToBack() && !event.isSkipped()) {
            this.plugin.getAchievementManager().onNewItemReceived(player.getUniqueId());
        }

        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.OBTAIN_ITEM);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.OBTAIN_ITEM_IN_TIME);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.BACK_TO_BACK);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.SKIP_ITEM);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.INVENTORY_FULL);
    }

    @EventHandler
    public void onPlayerChangeDimension(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.VISIT);
    }

    @EventHandler
    public void onChangeBiome(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.VISIT);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.DYING);
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.EATING);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // Handle both loot chests and beehive harvesting
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.LOOT);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.BEEHIVE_HARVEST);
    }

    @EventHandler
    public void onPlayerTrade(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.TRADING);
    }

    @EventHandler
    public void onAchievementGrant(PlayerGrantAchievementEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.ACHIEVEMENT);
    }
}