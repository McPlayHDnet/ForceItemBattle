package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
                ItemStack stack = new ItemStack(Material.BARRIER, Integer.parseInt(args[1]));
                ItemMeta m = stack.getItemMeta();
                m.setDisplayName(ChatColor.DARK_PURPLE + "Skip");
                stack.setItemMeta(m);

                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.setHealth(20);
                    player.setSaturation(20);
                    player.getInventory().clear();
                    player.setLevel(0);
                    player.setExp(0);
                    player.setWalkSpeed(0.2f);
                    player.getPassengers().forEach(Entity::remove);
                    player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(Bukkit.getWorld("world").getSpawnLocation());
                    player.setScoreboard(ForceItemBattle.getGamemanager().getBoard());
                    player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

                    ArmorStand itemDisplay = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, 2, 0), EntityType.ARMOR_STAND);
                    itemDisplay.getEquipment().setHelmet(new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player)));
                    itemDisplay.setInvisible(true);
                    itemDisplay.setInvulnerable(true);
                    //itemDisplay.setGlowing(true);
                    itemDisplay.setGravity(false);
                    player.addPassenger(itemDisplay);

                    if (ForceItemBattle.getGamemanager().isPlayerInMaps(player)) {
                        player.setGameMode(GameMode.SURVIVAL);
                        if (!ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                            player.getInventory().setItem(4, stack);
                            ForceItemBattle.getInstance().logToFile(player.getName() + " -> " + ForceItemBattle.getGamemanager().getPlayerTeamSTRING(player));
                        } else ForceItemBattle.getInstance().logToFile(player.getName());
                    } else {
                        player.setGameMode(GameMode.SPECTATOR);
                        ForceItemBattle.getInstance().logToFile(player.getName() + " -> Spectator");
                    }
                });
                if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame"))
                    ForceItemBattle.getBackpack().addToAllBp(stack);
                Bukkit.getWorld("world").setTime(0);
                ForceItemBattle.getTimer().setRunning(true);

                Bukkit.broadcastMessage(ChatColor.GOLD + "The game was started with " + args[1] + " jokers. " + args[0] + " minutes left.");

            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
                sender.sendMessage(ChatColor.RED + "<time> and <jokers> have to be numbers");
            }
        } else if (args.length == 0) {
            ForceItemBattle.getGamemanager().startGame(sender);
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /start <time in min> <jokers>");
        }
        return false;
    }
}
