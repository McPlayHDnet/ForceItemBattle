package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandStart implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            try {
                ForceItemBattle.getTimer().setTime(Integer.parseInt(args[0]) * 60);
                ForceItemBattle.getGamemanager().initializeMaps();

                if (Integer.parseInt(args[1]) > 64) {
                    sender.sendMessage(ChatColor.RED + "The maximum amount of jokers is 64.");
                    return false;
                }


                new BukkitRunnable() {

                    int seconds = 11;
                    @Override
                    public void run() {
                        seconds--;
                        if(seconds == 0) {
                            cancel();
                            startGame(Integer.parseInt(args[1]));
                            return;
                        }
                        if(seconds < 6) Bukkit.getOnlinePlayers().forEach(players -> players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1));

                        String finalSubTitle = getString();
                        Bukkit.getOnlinePlayers().forEach(players -> players.sendTitle("§a" + seconds, finalSubTitle, 0, 20, 10));
                    }

                    private String getString() {
                        String subTitle = "";

                        switch(seconds) {
                            case 9, 8 -> subTitle = "§f» §6" + (ForceItemBattle.getTimer().getTime() / 60) + " minutes §f«";
                            case 7, 6 -> subTitle = "§f» §6" + args[1] + " Joker §f«";
                            case 5, 4 -> subTitle = "§f» §6/info & /infowiki §f«";
                            case 3, 2 -> subTitle = "§f» §6Collect as many items as possible §f«";
                            case 1 -> subTitle = "§f» §6Have fun! §f«";
                        }

                        return subTitle;
                    }
                }.runTaskTimer(ForceItemBattle.getInstance(), 0L, 20L);


            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
                sender.sendMessage(ChatColor.RED + "<time> and <jokers> have to be numbers");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
        }
        return false;
    }

    private void startGame(Integer joker) {
        World world = Bukkit.getWorld("world");
        Location spawnLocation = world.getSpawnLocation();
        ForceItemBattle.setSpawnLocation(spawnLocation);

        Bukkit.getOnlinePlayers().forEach(player -> {

            player.sendMessage(" ");
            player.sendMessage("§8» §6§lMystery Item Battle §8«");
            player.sendMessage(" ");
            player.sendMessage("  §8● §7Duration §8» §a" + ForceItemBattle.getTimer().getTime() / 60 + " minutes");
            player.sendMessage("  §8● §7Joker §8» §a" + joker);
            player.sendMessage("  §8● §7Food §8» §a" + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.food") ? "§2✔" : "§4✘"));
            player.sendMessage("  §8● §7Keep Inventory §8» §a" + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.keepinventory") ? "§2✔" : "§4✘"));
            player.sendMessage("  §8● §7Backpack §8» §a" + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack") ? "§2✔" : "§4✘"));
            player.sendMessage("  §8● §7PvP §8» §a" + (ForceItemBattle.getInstance().getConfig().getBoolean("settings.pvp") ? "§2✔" : "§4✘"));
            player.sendMessage(" ");
            player.sendMessage(" §8● §7Useful Commands:");
            player.sendMessage("  §8» §6/info");
            player.sendMessage("  §8» §6/infowiki");
            player.sendMessage("");

            player.setHealth(20);
            player.setSaturation(20);
            player.getInventory().clear();
            player.getInventory().setItem(4, new ItemBuilder(Material.BARRIER).setAmount(joker).setDisplayName("§8» §5Skip").getItemStack());

            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
            player.getPassengers().forEach(Entity::remove);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(spawnLocation);
            player.setScoreboard(ForceItemBattle.getGamemanager().getBoard());
            player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

            ForceItemBattle.getGamemanager().getJokers().put(player.getUniqueId(), joker);

            if(ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack")) {
                ForceItemBattle.getBackpack().createBackpack(player);
            }

            ArmorStand itemDisplay = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, 2, 0), EntityType.ARMOR_STAND);
            itemDisplay.getEquipment().setHelmet(new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player)));
            itemDisplay.setInvisible(true);
            itemDisplay.setInvulnerable(true);
            //itemDisplay.setGlowing(true);
            itemDisplay.setGravity(false);
            player.addPassenger(itemDisplay);

        });
        Bukkit.getWorld("world").setTime(0);
        ForceItemBattle.getTimer().setRunning(true);
    }
}
