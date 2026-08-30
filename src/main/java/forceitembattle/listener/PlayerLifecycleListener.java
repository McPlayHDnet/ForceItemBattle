package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Scheduler;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.PlayerCounter;
import forceitembattle.model.Admission;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.util.Text;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class PlayerLifecycleListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Gamemanager gamemanager = this.plugin.getGamemanager();

        boolean onRoster = gamemanager.forceItemPlayerExist(player.getUniqueId());
        Admission admission = Roster.admit(onRoster, gamemanager.getCurrentGameState());

        if (onRoster) {
            gamemanager.getForceItemPlayer(player.getUniqueId()).setPlayer(player);
        } else if (admission.joinsRoster()) {
            ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, null, 0, 0);
            forceItemPlayer.setSpectator(admission.isSpectating());
            gamemanager.addPlayer(player, forceItemPlayer);
        }

        switch (admission) {
            case RETURNING_PARTICIPANT -> this.restoreParticipant(player);
            case RESULT_SCREEN -> this.showResultScreen(player);
            case LATE_SPECTATOR, COUNTDOWN_SPECTATOR -> this.makeSpectator(player);
            case LOBBY -> this.outfitLobbyPlayer(player);
            // Nothing to write; reattaching the player object above was the whole outcome.
            case RECONNECTING_BEFORE_START -> { }
        }

        plugin.getScoreboardManager().setupForPlayer(player);
        plugin.getScoreboardManager().updateAllPlayers();

        player.sendPlayerListHeader(Text.of("<!shadow>\n\n\n\ue000\ue003\ue001\ue003\ue002\n"));
        event.joinMessage(Text.of("<green>» <yellow>" + player.getName() + " <green>joined"));
    }

    /**
     * A participant rejoining a running round.
     *
     * <p>{@code applyStartSetup} is a no-op for anyone who was online when the countdown ended and
     * the full round setup for anyone who was not — {@code startSetupApplied} is what tells them
     * apart, so this call is safe either way.
     */
    private void restoreParticipant(Player player) {
        this.plugin.getGamemanager().applyStartSetup(player);

        // Only players the timer has already ticked for have a bar. Someone who was offline for the
        // whole countdown has none yet, and showBossBar(null) would throw here and abort the rest
        // of the join.
        BossBar bossBar = this.plugin.getTimerManager().getBossBar().get(player.getUniqueId());
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * They missed the result screen {@code finishGame()} handed out; give them the same one rather
     * than resetting them to a lobby player and losing the score they are ranked on.
     */
    private void showResultScreen(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        this.plugin.getGamemanager().giveSpectatorItems(player);
    }

    private void makeSpectator(Player player) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void outfitLobbyPlayer(Player player) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().setItem(0, new ItemBuilder(Material.WRITTEN_BOOK)
                .setDisplayName("<dark_gray>» <dark_aqua>Collection")
                .addItemFlags(ItemFlag.values())
                .getItemStack());
        player.getInventory().setItem(4, new ItemBuilder(Material.LIME_DYE).setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_PEARL).setDisplayName("<dark_gray>» <gray>Spectate game").getItemStack());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.quitMessage(Text.of("<red>« <yellow>" + playerQuitEvent.getPlayer().getName() + " <red>ragequit"));

        if (Roster.releasesSpotOnQuit(this.plugin.getGamemanager().getCurrentGameState())) {
            ForceItemPlayer fibPlayer = this.plugin.getGamemanager().getForceItemPlayer(playerQuitEvent.getPlayer().getUniqueId());
            if (fibPlayer != null && fibPlayer.isInTeam()) {
                this.plugin.getTeamManager().leave(fibPlayer);
            }

            this.plugin.getGamemanager().removePlayer(playerQuitEvent.getPlayer());
        }

        if (this.plugin.getGamemanager().isMidGame()) {
            playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ForceItemPlayer gamePlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        String plainDeathMessage = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(event.deathMessage()));
        String plainPlayerName = PlainTextComponentSerializer.plainText().serialize(player.name());

        event.deathMessage(Text.of("<dark_gray>[<red>\uD83D\uDC80<dark_gray>] " + plainDeathMessage.replace(plainPlayerName, "<gold>" + player.getName() + "<gray>")));
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
            event.getDrops().removeIf(Gamemanager::isBackpack);
        }

        if (this.plugin.getGamemanager().isMidGame() && this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
            this.plugin.getFibService().statistics()
                    .recordPlayerCounter(player.getUniqueId(), gamePlayer, PlayerCounter.DEATHS, 1);
        }

        // Skip the death screen entirely; nobody sits out a round here.
        Scheduler.runLaterSync(() -> event.getEntity().spigot().respawn(), 1);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Boolean keepInventory = player.getWorld().getGameRuleValue(GameRules.KEEP_INVENTORY);
        if (keepInventory == null || !keepInventory) {
            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));

            player.performCommand("fixskips -silent");
        }

        player.getInventory().setItem(8, Gamemanager.createBackpack(forceItemPlayer, forceItemPlayer.isInTeam()));

    }
}
