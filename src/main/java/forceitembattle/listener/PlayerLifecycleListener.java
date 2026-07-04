package forceitembattle.listener;

import forceitembattle.util.Text;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.stats.FIBServiceHelper;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ItemBuilder;
import lombok.RequiredArgsConstructor;
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
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@RequiredArgsConstructor
public class PlayerLifecycleListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ForceItemPlayer forceItemPlayer = new ForceItemPlayer(player, null, 0, 0);
        if (this.plugin.getGamemanager().isMidGame() || this.plugin.getGamemanager().isPausedGame()) {
            if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.setGameMode(GameMode.SPECTATOR);

            } else {
                forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                forceItemPlayer.setPlayer(player);
                player.showBossBar(this.plugin.getTimer().getBossBar().get(event.getPlayer().getUniqueId()));
            }
        } else {

            this.plugin.getGamemanager().addPlayer(player, forceItemPlayer);

            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setGameMode(GameMode.ADVENTURE);

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
            FIBServiceHelper helper = plugin.getFibServiceHelper();
            if (gamePlayer != null && gamePlayer.currentTeam() != null) {
                gamePlayer.currentTeam().getPlayers().stream()
                        .filter(teammate -> !teammate.equals(gamePlayer))
                        .forEach(teammate -> helper.updateMemberStatisticsAsync(
                                player.getUniqueId(),
                                teammate.player().getUniqueId(),
                                player.getUniqueId(),
                                FIBServiceHelper.memberUpdate().deathsAdd(1L)
                        ));
            } else {
                helper.updateSoloStatisticsAsync(player.getUniqueId(),
                        FIBServiceHelper.soloUpdate().deathsAdd(1L));
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