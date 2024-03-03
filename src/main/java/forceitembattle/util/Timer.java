package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Timer {

    private final ForceItemBattle forceItemBattle;
    @Getter
    private final Map<UUID, BossBar> bossBar = new HashMap<>();
    @Getter
    @Setter
    private int time;

    public Timer(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;

        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.time = this.forceItemBattle.getConfig().getInt("timer.time");
        } else {
            this.time = 0;
        }

        run();
    }

    public String formatSeconds(int inputSeconds) {
        int seconds = inputSeconds % 60;
        int minutes = (inputSeconds / 60) % 60;
        int hours = inputSeconds / 60 / 60;

        String time = "";
        if (hours != 0) {
            time += hours + "h ";
        }
        if (minutes != 0) {
            time += minutes + "m ";
        }
        if (seconds != 0) {
            time += seconds + "s";
        }

        //Perhaps replace with just a simple: LocalTime.MIN.plusSeconds(inputSeconds).toString();
        return time;
    }

    public void sendActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!this.forceItemBattle.getGamemanager().isMidGame()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED +
                        "Timer paused"));
                continue;
            }

            if (this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());

                player.setPlayerListName(player.getName() + " §7[§6" + this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer) + "§7]");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                        ChatColor.BOLD + formatSeconds(getTime()) + " §8| §aYour score: §f" + forceItemPlayer.getCurrentScore()));

                String material = this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer);
                String bossBarTitle = "§a§l" + material;

                BossBar bar = bossBar.get(player.getUniqueId());
                if (bar == null) {
                    bar = Bukkit.createBossBar(bossBarTitle, BarColor.WHITE, BarStyle.SOLID);
                    bar.addPlayer(player);
                    bossBar.put(player.getUniqueId(), bar);
                }

                if (!bar.getTitle().equalsIgnoreCase(bossBarTitle)) {
                    bar.removePlayer(player);
                    bar.setTitle(bossBarTitle);
                    bar.addPlayer(player);
                }

            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                        ChatColor.BOLD + formatSeconds(getTime()) + " | SPEC"));
            }
        }
    }

    public void save() {
        this.forceItemBattle.getConfig().set("timer.time", time);
    }

    private void run() {
        new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBar();

                if (!forceItemBattle.getGamemanager().isMidGame()) {
                    return;
                }

                setTime(getTime() - 1);

                switch (getTime()) {
                    case 300, 60 -> {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< " + (getTime() / 60) + " minutes left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                    }
                    case 30, 10, 5, 4, 3, 2, 1 -> {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< " + getTime() + " seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                    }
                }

                if (getTime() <= 0) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "<< Force Item Battle is over >>");
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1));
                    forceItemBattle.getGamemanager().finishGame();
                    this.cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }
}
