package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.model.ForceItemPlayer;
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

        // An existing roster entry always wins over the defaults below. Someone who disconnected
        // during the countdown still owns their team, their force item and their score, so they come
        // back as the participant they were — never as a freshly created spectator.
        if (gamemanager.forceItemPlayerExist(player.getUniqueId())) {
            ForceItemPlayer forceItemPlayer = gamemanager.getForceItemPlayer(player.getUniqueId());
            forceItemPlayer.setPlayer(player);

            if (gamemanager.isMidGame() || gamemanager.isPausedGame()) {
                // No-op for anyone who was online when the countdown ended; the full round setup
                // for anyone who was not.
                gamemanager.applyStartSetup(player);

                // Only players the timer has already ticked for have a bar. Someone who was offline
                // for the whole countdown has none yet, and showBossBar(null) would throw here and
                // abort the rest of the join.
                BossBar bossBar = this.plugin.getTimerManager().getBossBar().get(player.getUniqueId());
                if (bossBar != null) {
                    player.showBossBar(bossBar);
                }
            } else if (gamemanager.isEndGame()) {
                // Missed the result screen finishGame() handed out; give them the same one rather
                // than resetting them to a lobby player and losing the score they are ranked on.
                player.getInventory().clear();
                player.setGameMode(GameMode.CREATIVE);
                gamemanager.giveSpectatorItems(player);
            }
        } else if (gamemanager.isMidGame() || gamemanager.isPausedGame()) {
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setGameMode(GameMode.SPECTATOR);

        } else if (gamemanager.isStarting()) {
            ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, null, 0, 0);
            forceItemPlayer.setSpectator(true);
            gamemanager.addPlayer(player, forceItemPlayer);

            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setGameMode(GameMode.SPECTATOR);
        } else {

            gamemanager.addPlayer(player, new ForceItemPlayer(player, null, 0, 0));

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

        plugin.getScoreboardManager().setupForPlayer(player);
        plugin.getScoreboardManager().updateAllPlayers();

        player.sendPlayerListHeader(Text.of("<!shadow>\n\n\n\ue000\ue003\ue001\ue003\ue002\n"));
        event.joinMessage(Text.of("<green>» <yellow>" + player.getName() + " <green>joined"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.quitMessage(Text.of("<red>« <yellow>" + playerQuitEvent.getPlayer().getName() + " <red>ragequit"));

        // Deliberately not during STARTING: once the countdown runs, teams and force items are
        // already assigned, and dropping the player here would tear their team apart and cost them
        // the round. They keep their spot and are restored on rejoin.
        if (this.plugin.getGamemanager().isPreGame() || this.plugin.getGamemanager().isEndGame()) {
            if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                ForceItemPlayer fibPlayer = this.plugin.getGamemanager().getForceItemPlayer(playerQuitEvent.getPlayer().getUniqueId());
                if (fibPlayer != null && fibPlayer.currentTeam() != null) {
                    this.plugin.getTeamManager().leave(fibPlayer);
                }
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
            FibStatisticsClient helper = plugin.getFibService().statistics();
            if (gamePlayer != null && gamePlayer.currentTeam() != null) {
                gamePlayer.currentTeam().getPlayers().stream()
                        .filter(teammate -> !teammate.equals(gamePlayer))
                        .forEach(teammate -> helper.updateMemberStatisticsAsync(
                                player.getUniqueId(),
                                teammate.player().getUniqueId(),
                                player.getUniqueId(),
                                FIBServiceClient.memberUpdate().deathsAdd(1L)
                        ));
            } else {
                helper.updateSoloStatisticsAsync(player.getUniqueId(),
                        FIBServiceClient.soloUpdate().deathsAdd(1L));
            }
        }

        // Automatically respawn player.
        Bukkit.getScheduler().runTaskLater(
                this.plugin,
                () -> event.getEntity().spigot().respawn(),
                1
        );
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

        player.getInventory().setItem(8, Gamemanager.createBackpack(forceItemPlayer, this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)));

    }
}
