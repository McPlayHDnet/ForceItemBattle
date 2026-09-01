package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.PlayerCounter;
import forceitembattle.manager.PlayerOutfitter;
import forceitembattle.model.Admission;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.util.Text;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class PlayerLifecycleListener implements Listener {
    private final Roster roster;
    private final FIBServiceClient fibService;
    private final RoundPhase roundPhase;
    private final Gamemanager gamemanager;
    private final ScoreboardManager scoreboardManager;
    private final GameSettings settings;
    private final TeamsManager teamManager;
    private final TimerManager timerManager;
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean onRoster = this.roster.contains(player.getUniqueId());
        Admission admission = Roster.admit(onRoster, this.roundPhase.state());

        ForceItemPlayer existing = this.roster.get(player.getUniqueId());
        if (existing != null) {
            existing.setPlayer(player);
        } else if (admission.joinsRoster()) {
            ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, null, 0, 0);
            forceItemPlayer.setSpectator(admission.isSpectating());
            this.roster.add(player.getUniqueId(), forceItemPlayer);
        }

        switch (admission) {
            case RETURNING_PARTICIPANT -> this.restoreParticipant(player);
            case RESULT_SCREEN -> this.showResultScreen(player);
            case LATE_SPECTATOR, COUNTDOWN_SPECTATOR -> PlayerOutfitter.toSpectator(player);
            case LOBBY -> PlayerOutfitter.toLobby(player, this.roundPhase.state());
            // Nothing to write, and deliberately so. Reattaching the player object above was the
            // whole outcome: quitting in PRE_GAME releases your roster spot, so the only way to
            // reach this arm is to quit during the countdown and return before it ends -- and
            // applyStartSetup will outfit them when it does. Writing a state here would be undone
            // seconds later, and toLobby would put lobby buttons in the hands of someone who is
            // about to be a participant.
            case RECONNECTING_BEFORE_START -> { }
        }

        this.scoreboardManager.setupForPlayer(player);
        this.scoreboardManager.updateAllPlayers();

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
        this.gamemanager.applyStartSetup(player);

        // Only players the timer has already ticked for have a bar. Someone who was offline for the
        // whole countdown has none yet, and showBossBar(null) would throw here and abort the rest
        // of the join.
        BossBar bossBar = this.timerManager.getBossBar().get(player.getUniqueId());
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * They missed the result screen {@code finishGame()} handed out; give them the same one rather
     * than resetting them to a lobby player and losing the score they are ranked on.
     *
     * <p>"The same one" is now literally the same body. This used to be a thinner copy — clear,
     * creative, buttons — and the four things it left out were exactly the four a rejoiner still
     * carries: {@code RESULT_SCREEN} is only reachable by quitting <em>during</em> the round and
     * returning after it ended, so {@code finishGame} never touched them and they arrive on the
     * result screen with the health, mount, level and coloured tab name they disconnected with.
     *
     * <p>The destination is resolved here rather than inside the outfitter, which takes decided
     * values only. It is the world spawn, matching {@code finishGame} — not
     * {@code ForceItemBattle.getSpawnLocation()}, which is a different, configured place, and
     * sending a rejoiner there would leave them somewhere the rest of the room is not.
     */
    private void showResultScreen(Player player) {
        World overworld = Dimension.OVERWORLD.world();
        PlayerOutfitter.toResultScreen(player, overworld == null ? null : overworld.getSpawnLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.quitMessage(Text.of("<red>« <yellow>" + playerQuitEvent.getPlayer().getName() + " <red>ragequit"));

        if (Roster.releasesSpotOnQuit(this.roundPhase.state())) {
            ForceItemPlayer fibPlayer = this.roster.get(playerQuitEvent.getPlayer().getUniqueId());
            if (fibPlayer != null && fibPlayer.isInTeam()) {
                this.teamManager.leave(fibPlayer);
            }

            this.roster.remove(playerQuitEvent.getPlayer().getUniqueId());
        }

        if (this.roundPhase.roundRunning()) {
            playerQuitEvent.getPlayer().getPassengers().forEach(Entity::remove);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ForceItemPlayer gamePlayer = this.roster.get(player.getUniqueId());
        String plainDeathMessage = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(event.deathMessage()));
        String plainPlayerName = PlainTextComponentSerializer.plainText().serialize(player.name());

        event.deathMessage(Text.of("<dark_gray>[<red>\uD83D\uDC80<dark_gray>] " + plainDeathMessage.replace(plainPlayerName, "<gold>" + player.getName() + "<gray>")));
        if (!event.getKeepInventory()) {
            event.getDrops().removeIf(Gamemanager::isJoker);
            event.getDrops().removeIf(Gamemanager::isBackpack);
        }

        if (this.roundPhase.roundRunning() && this.settings.isSettingEnabled(GameSetting.STATS)) {
            this.fibService.statistics()
                    .recordPlayerCounter(player.getUniqueId(), gamePlayer, PlayerCounter.DEATHS, 1);
        }

        // Skip the death screen entirely; nobody sits out a round here.
        Scheduler.runLaterSync(() -> event.getEntity().spigot().respawn(), 1);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!this.roundPhase.roundRunning()) {
            return;
        }

        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        Boolean keepInventory = player.getWorld().getGameRuleValue(GameRules.KEEP_INVENTORY);
        if (keepInventory == null || !keepInventory) {
            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));

            player.performCommand("fixskips -silent");
        }

        // A mid-round joiner holds no roster entry and no backpack, so there is nothing to put
        // in slot 8 for them.
        if (forceItemPlayer != null) {
            player.getInventory().setItem(8, Gamemanager.createBackpack(forceItemPlayer, forceItemPlayer.isInTeam()));
        }

    }
}
