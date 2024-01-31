package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
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
    private boolean running;
    private int time;
    private Map<UUID, BossBar> bossBar = new HashMap<UUID, BossBar>();

    public Timer() {
        this.running = false;

        if (ForceItemBattle.getInstance().getConfig().contains("timer.time")) {
            this.time = ForceItemBattle.getInstance().getConfig().getInt("timer.time");
        } else {
            this.time = 0;
        }

        run();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
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

            if (!isRunning()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED +
                        "Timer paused"));
                continue;
            }

            if (ForceItemBattle.getGamemanager().isPlayerInMaps(player)) {

                if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                    player.setPlayerListName(player.getName() + " §7[§6" + WordUtils.capitalize(ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(player).toString().replace("_", " ").toLowerCase()) + "§7]");

                    /////////////////////////////////////// TEAMS ///////////////////////////////////////
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                            ChatColor.BOLD + formatSeconds(getTime()) + " §8| §aTeam score: §f" + ForceItemBattle.getGamemanager().getTeamScoreFromPlayer(player)));

                    //String material = ForceItemBattle.getColorManager().rgbGradient(ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(player).toString().replace("_", " ").toLowerCase(), new java.awt.Color(34, 0, 241), new java.awt.Color(138, 2, 40), ForceItemBattle.getColorManager()::linear);
                    String material = ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(player).toString().replace("_", " ").toLowerCase();

                    try {
                        if (!bossBar.get(player.getUniqueId()).getTitle().equalsIgnoreCase(WordUtils.capitalize(material))) {
                            bossBar.get(player.getUniqueId()).setTitle(WordUtils.capitalize(material));
                            bossBar.get(player.getUniqueId()).addPlayer(player);
                        }
                    } catch (NullPointerException e) {
                        BossBar bar = Bukkit.createBossBar(WordUtils.capitalize(material), BarColor.WHITE, BarStyle.SOLID);
                        bossBar.put(player.getUniqueId(), bar);
                        bossBar.get(player.getUniqueId()).addPlayer(player);
                    }
                } else {
                    player.setPlayerListName(player.getName() + " §7[§6" + ForceItemBattle.getGamemanager().getCurrentMaterialName(player) + "§7]");

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                            ChatColor.BOLD + formatSeconds(getTime()) + " §8| §aYour score: §f" + ForceItemBattle.getGamemanager().getScore(player)));

                    //String material = ForceItemBattle.getColorManager().rgbGradient(ForceItemBattle.getGamemanager().getCurrentMaterial(player).toString().replace("_", " ").toLowerCase(), new java.awt.Color(34, 0, 241), new java.awt.Color(138, 2, 40), ForceItemBattle.getColorManager()::linear);
                    String material = ForceItemBattle.getGamemanager().getCurrentMaterialName(player);
                    String bossBarTitle = "§a§l" + material;

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
                }
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD.toString() +
                        ChatColor.BOLD + formatSeconds(getTime()) + " | SPEC"));
            }
        }
    }

    public void save() {
        ForceItemBattle.getInstance().getConfig().set("timer.time", time);
    }

    private void run() {
        new BukkitRunnable() {
            @Override
            public void run() {

                sendActionBar();
                if (!isRunning() || getTime() <= 0) {
                    return;
                }
                setTime(getTime() - 1);

                ForceItemBattle.getGamemanager().decreaseDelay();

                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (ForceItemBattle.getGamemanager().isPlayerInMaps(player)) {
                        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                            /////////////////////////////////////// TEAMS ///////////////////////////////////////
                            if (player.getInventory().contains(ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(player))) {
                                ForceItemBattle.getGamemanager().checkItem(player, ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(player), false);
                            }
                        } else {
                            if (player.getInventory().contains(ForceItemBattle.getGamemanager().getCurrentMaterial(player))) {
                                //ForceItemBattle.getGamemanager().checkItem(player, ForceItemBattle.getGamemanager().getCurrentMaterial(player), false);
                            }
                        }
                    }
                });

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
                    ForceItemBattle.getGamemanager().finishGame();
                    ForceItemBattle.getInstance().logToFile("<< Force Item Battle is over >>");
                    cancel();
                }
            }
        }.runTaskTimer(ForceItemBattle.getInstance(), 20, 20);
    }

    public Map<UUID, BossBar> getBossBar() {
        return bossBar;
    }
}
