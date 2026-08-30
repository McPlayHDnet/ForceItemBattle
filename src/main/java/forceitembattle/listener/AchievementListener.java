package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Scheduler;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

@RequiredArgsConstructor
public class AchievementListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Warm the cache from the service ahead of any in-game achievement checks.
        UUID joiningUuid = event.getPlayer().getUniqueId();
        this.plugin.getAchievementManager().getAchievementStorage()
                .loadPlayer(joiningUuid, () -> this.plugin.getAchievementManager()
                        .evaluateGlobalAchievements(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Free memory once a player leaves, but only outside a running game so
        // team-completion checks still see their data mid-round.
        if (!this.plugin.getGamemanager().roundRunning()) {
            this.plugin.getAchievementManager().getAchievementStorage()
                    .unloadPlayer(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Changed to MONITOR so it runs AFTER Listeners.updateMaterials()
    public void onItemObtain(FoundItemEvent event) {
        Player player = event.getPlayer();

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
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.BEEHIVE_HARVEST);
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.LOOT);
    }

    @EventHandler
    public void onPlayerTrade(PlayerPurchaseEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAchievementManager().handleEvent(player, event, Trigger.TRADING);
    }

    @EventHandler
    public void onAchievementGrant(PlayerGrantAchievementEvent event) {
        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Achievements achievement = event.getAchievement();

        if (forceItemPlayer == null || !forceItemPlayer.isSpectator()) {
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 1);
            Bukkit.getOnlinePlayers().forEach(players -> {
                players.sendMessage(Component.empty());
                players.sendMessage(Text.of("<dark_gray>[<yellow>❋<dark_gray>] <gold>" + player.getName() + " <gray>has made the achievement <hover:show_text:'<dark_aqua>" + achievement.getTitle() + "<newline><gray>" + achievement.getDescription() + "'><dark_aqua>[" + achievement.getTitle() + "]</hover>"));
                players.sendMessage(Component.empty());
            });
        }
    }

    @EventHandler
    public void onWheelOfFortuneWin(WheelOfFortuneWinEvent event) {
        this.plugin.getAchievementManager().handleEvent(event.getPlayer(), event, Trigger.WHEEL_OF_FORTUNE);
    }

    @EventHandler
    public void onAntimatterTeleporterUse(AntimatterTeleporterUseEvent event) {
        this.plugin.getAchievementManager().handleEvent(event.getPlayer(), event, Trigger.ANTIMATTER_TELEPORTER);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Credit the player who killed the mob (null when it died to the
        // environment or another mob — no one to award in that case).
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            this.plugin.getAchievementManager().handleEvent(killer, event, Trigger.MOB_DEATH);
        }
    }

    @EventHandler
    public void onLootGenerate(org.bukkit.event.world.LootGenerateEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.plugin.getAchievementManager().handleEvent(player, event, Trigger.LOOT);
        }
    }

    @EventHandler
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        this.plugin.getAchievementManager().handleEvent(event.getPlayer(), event, Trigger.INVENTORY_FULL);
    }

    @EventHandler
    public void onBackpackClick(InventoryClickEvent event) {
        recheckBackpackFull(event.getWhoClicked(), event.getView().getTopInventory(), event);
    }

    @EventHandler
    public void onBackpackDrag(InventoryDragEvent event) {
        recheckBackpackFull(event.getWhoClicked(), event.getView().getTopInventory(), event);
    }

    private void recheckBackpackFull(HumanEntity who, Inventory top, org.bukkit.event.Event event) {
        if (!(who instanceof Player player)) {
            return;
        }
        // Reference-equality against the stored backpack instance; ignore other GUIs.
        if (top != this.plugin.getBackpackManager().getBackpackForPlayer(player)) {
            return;
        }
        // Click/drag fire before the slot settles, so re-check on the next tick.
        Scheduler.runSync(() ->
                this.plugin.getAchievementManager().handleEvent(player, event, Trigger.INVENTORY_FULL));
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT)) {
            event.message(null);
            return;
        }

        Advancement advancement = event.getAdvancement();

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(event.getPlayer().getUniqueId());
        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            event.message(null);
            return;
        }

        if (advancement.key().namespace().equals("fib")) {
            String plainAdvancement = PlainTextComponentSerializer.plainText().serialize(advancement.displayName());
            String plainAdvancementDescription = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(advancement.getDisplay()).description());

            String advancementType = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "has completed the challenge" : "has made the advancement";
            String advancementTypeColor = advancement.getDisplay().frame() == AdvancementDisplay.Frame.CHALLENGE ? "<dark_purple>" : "<green>";

            event.message(Text.of("<dark_gray>[<yellow>⭐<dark_gray>] <gold>" + event.getPlayer().getName() + " <gray>" + advancementType + " <hover:show_text:'" + advancementTypeColor + plainAdvancement + "<newline>" + advancementTypeColor + plainAdvancementDescription + "'>" + advancementTypeColor + plainAdvancement + "</hover>"));
        } else {
            event.message(null);
        }
    }
}
