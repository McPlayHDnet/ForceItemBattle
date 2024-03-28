package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Timer {

    private ForceItemBattle forceItemBattle;
    private int time;
    private final Map<UUID, BossBar> bossBar = new HashMap<>();

    public Timer(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        if (this.forceItemBattle.getConfig().contains("timer.time")) {
            this.time = this.forceItemBattle.getConfig().getInt("timer.time");
        } else {
            this.time = 0;
        }

        run();
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String formatSeconds(int inputSeconds) {
        int seconds = inputSeconds % 60;
        int minutes = (inputSeconds / 60) % 60;
        int hours = inputSeconds / 60 / 60;

        String time = "";
        if(hours != 0) time += hours + "h ";
        if(minutes != 0) time += minutes + "m ";
        if(seconds != 0) time += seconds + "s";

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

                player.setPlayerListName(player.getName() + " §7[§6" + this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer) + " §r" + net.md_5.bungee.api.ChatColor.of(new Color(78, 92, 36)) + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(true, forceItemPlayer.currentMaterial()) + "§7]");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                        ChatColor.BOLD + formatSeconds(getTime()) + " §8| §aYour score: §f" + forceItemPlayer.currentScore()));

                String material = this.forceItemBattle.getGamemanager().getCurrentMaterialName(forceItemPlayer);

                String bossBarTitle = "§a§l" + material + " §r" + net.md_5.bungee.api.ChatColor.of(new Color(78, 92, 36)) + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(false, forceItemPlayer.currentMaterial());

                try {
                    BossBar bar = bossBar.get(player.getUniqueId());
                    if (!bar.getTitle().equalsIgnoreCase(bossBarTitle)) {
                        bar.removePlayer(player);
                        bar.setTitle(bossBarTitle);
                        bar.addPlayer(player);
                    }
                } catch (NullPointerException e) {
                    BossBar bar = Bukkit.createBossBar(bossBarTitle, BarColor.WHITE, BarStyle.SOLID);
                    bar.addPlayer(player);
                    bossBar.put(player.getUniqueId(), bar);
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
                    case 300: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 5 minutes left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 60: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 1 minute left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 30: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 30 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 10: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 10 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 5: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 5 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 4: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 4 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 3: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 3 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 2: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 2 seconds left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    case 1: {
                        Bukkit.broadcastMessage(ChatColor.RED + "<< 1 second left >>");
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        break;
                    }
                    default:
                        break;
                }
                if (getTime()<=0) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "<< Force Item Battle is over >>");
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1));
                    forceItemBattle.getGamemanager().finishGame();
                    forceItemBattle.logToFile("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(this.forceItemBattle, 20, 20);
    }

    public Map<UUID, BossBar> getBossBar() {
        return bossBar;
    }
}
