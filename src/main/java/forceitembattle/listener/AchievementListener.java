package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.manager.AchievementManager;
import forceitembattle.settings.achievements.*;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemObtain(FoundItemEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.OBTAIN_ITEM);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.OBTAIN_ITEM_IN_TIME);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.BACK_TO_BACK);
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.SKIP_ITEM);
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
    public void onTrade(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.TRADING);
    }

    @EventHandler
    public void onEatItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.EATING);
    }

    @EventHandler
    public void onAchievement(PlayerGrantAchievementEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.ACHIEVEMENT);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.LOOT);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.DYING);
    }



}
