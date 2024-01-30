package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItem;
import org.apache.commons.text.WordUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class Gamemanager {
    private static Map<UUID, Integer> score = new HashMap<UUID, Integer>();
    private static Map<UUID, Integer> jokers = new HashMap<>();
    private static Map<UUID, Material> currentMaterial = new HashMap<UUID, Material>();
    private static Map<UUID, ArrayList<ForceItem>> itemList = new HashMap<UUID, ArrayList<ForceItem>>();
    private static Map<UUID, Integer> delay = new HashMap<UUID, Integer>();

    public Map<UUID, Map<Integer, ItemStack[]>> savedInventory = new HashMap<>();
    private static boolean ASC = true;
    private static boolean DESC = false;

    /////////////////////////////////////// TEAMS ///////////////////////////////////////
    private static Map<String, Integer> scoreTeams = new HashMap<>();
    private static Map<String, Material> currentMaterialTeams = new HashMap<>();

    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = manager.getNewScoreboard();
    Team team1 = board.registerNewTeam("team1");
    Team team2 = board.registerNewTeam("team2");
    Team team3 = board.registerNewTeam("team3");
    Team team4 = board.registerNewTeam("team4");
    Team team5 = board.registerNewTeam("team5");
    Team team6 = board.registerNewTeam("team6");
    Team team7 = board.registerNewTeam("team7");
    Team team8 = board.registerNewTeam("team8");
    Team team9 = board.registerNewTeam("team9");

    public Gamemanager() {
        team1.setDisplayName("[Team1]");
        team1.setPrefix(ChatColor.WHITE + "Team1 | " + ChatColor.WHITE);
        //team1.setColor(ChatColor.WHITE);
        ///////////////////////////////////////////////
        team2.setDisplayName("[Team2]");
        team2.setPrefix(ChatColor.DARK_GRAY + "Team2 | " + ChatColor.WHITE);
        //team2.setColor(ChatColor.DARK_GRAY);
        ///////////////////////////////////////////////
        team3.setDisplayName("[Team3]");
        team3.setPrefix(ChatColor.RED + "Team3 | " + ChatColor.WHITE);
        //team3.setColor(ChatColor.RED);
        ///////////////////////////////////////////////
        team4.setDisplayName("[Team4]");
        team4.setPrefix(ChatColor.YELLOW + "Team4 | " + ChatColor.WHITE);
        //team4.setColor(ChatColor.YELLOW);
        ///////////////////////////////////////////////
        team5.setDisplayName("[Team5]");
        team5.setPrefix(ChatColor.GREEN + "Team5 | " + ChatColor.WHITE);
        //team5.setColor(ChatColor.GREEN);
        ///////////////////////////////////////////////
        team6.setDisplayName("[Team6]");
        team6.setPrefix(ChatColor.AQUA + "Team6 | " + ChatColor.WHITE);
        //team6.setColor(ChatColor.AQUA);
        ///////////////////////////////////////////////
        team7.setDisplayName("[Team7]");
        team7.setPrefix(ChatColor.DARK_BLUE + "Team7 | " + ChatColor.WHITE);
        //team7.setColor(ChatColor.DARK_BLUE);
        ///////////////////////////////////////////////
        team8.setDisplayName("[Team8]");
        team8.setPrefix(ChatColor.DARK_PURPLE + "Team8 | " + ChatColor.WHITE);
        //team8.setColor(ChatColor.DARK_PURPLE);
        ///////////////////////////////////////////////
        team9.setDisplayName("[Team9]");
        team9.setPrefix(ChatColor.LIGHT_PURPLE + "Team9 | " + ChatColor.WHITE);
        //team9.setColor(ChatColor.LIGHT_PURPLE);
    }

    public boolean isPlayerInMaps(Player player) {
        boolean value = true;
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
            /////////////////////////////////////// TEAMS ///////////////////////////////////////
            if (getPlayerTeam(player) == null) value = false;
        } else {
            if (!score.containsKey(player.getUniqueId())) value = false;
            if (!currentMaterial.containsKey(player.getUniqueId())) value = false;
        }
        return value;
    }

    public void checkItem(Player player, Material material, boolean usedSkip) {
        if (!isPlayerInMaps(player)) return;
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
            /////////////////////////////////////// TEAMS ///////////////////////////////////////
            if (material == currentMaterialTeams.get(getPlayerTeamSTRING(player))) {
                scoreTeams.put(getPlayerTeamSTRING(player), scoreTeams.get(getPlayerTeamSTRING(player)) + 1);
                currentMaterialTeams.put(getPlayerTeamSTRING(player), generateMaterial());
                board.getTeam(getPlayerTeamSTRING(player)).getEntries().forEach(e -> {
                    Bukkit.getPlayer(e).playSound(Bukkit.getPlayer(e).getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                });
                ForceItemBattle.getInstance().logToFile("[" + ForceItemBattle.getTimer().getTime() + "] | (" + getPlayerTeamSTRING(player) + ") " + player.getName() + " got item: " + material.toString() + " | new points: " + getTeamScoreFromPlayer(player));
            }
        } else {
            if (material == currentMaterial.get(player.getUniqueId())) {
                score.put(player.getUniqueId(), score.get(player.getUniqueId()) + 1);
                ArrayList<ForceItem> mat = itemList.get(player.getUniqueId());
                mat.add(new ForceItem(material, ForceItemBattle.getTimer().formatSeconds(ForceItemBattle.getTimer().getTime()), usedSkip));
                itemList.put(player.getUniqueId(), mat);
                currentMaterial.put(player.getUniqueId(), generateMaterial());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

                ArmorStand armorStand = (ArmorStand) player.getPassengers().get(0);
                armorStand.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(player)));

                ForceItemBattle.getInstance().logToFile("[" + ForceItemBattle.getTimer().getTime() + "] | " + player.getName() + " got item: " + material.toString() + " | new points: " + score.get(player.getUniqueId()));
            }
        }
    }

    public Material generateMaterial() {
        return ForceItemBattle.getItemDifficultiesManager().getHardMaterial();
    }

    public int getScore(Player player) {
        return score.get(player.getUniqueId());
    }

    public Material getCurrentMaterial(Player player) {
        return currentMaterial.get(player.getUniqueId());
    }

    public String getCurrentMaterialName(Player player) {
        return WordUtils.capitalizeFully(getCurrentMaterial(player).toString().replace("_", " "));
    }

    public void initializeMaps() {
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
            /////////////////////////////////////// TEAMS ///////////////////////////////////////
            for (int i = 1; i <= 9; i++) {
                scoreTeams.put("team" + i, 0);
                currentMaterialTeams.put("team" + i, generateMaterial());
                Bukkit.getOnlinePlayers().forEach(player -> {
                    delay.put(player.getUniqueId(), 5);
                });
            }
            ForceItemBattle.getBackpack().clearAllBp();
        } else {
            Bukkit.getOnlinePlayers().forEach(player -> {
                score.put(player.getUniqueId(), 0);
                currentMaterial.put(player.getUniqueId(), generateMaterial());
                delay.put(player.getUniqueId(), 5);

                ArrayList<ForceItem> mat = new ArrayList<>();
                itemList.put(player.getUniqueId(), mat);
            });
        }

    }

    public void startGame(CommandSender sender) {
        try {
            ForceItemBattle.getTimer().setTime(ForceItemBattle.getInstance().getConfig().getInt("standard.countdown") * 60);
            initializeMaps();

            int num = ForceItemBattle.getInstance().getConfig().getInt("standard.jokers");
            if (num > 64) {
                sender.sendMessage(ChatColor.RED + "The maximum amount of jokers is 64.");
                return;
            }
            ItemStack stack = new ItemStack(Material.BARRIER, num);
            ItemMeta m = stack.getItemMeta();
            assert m != null;
            m.setDisplayName(ChatColor.RED + "Skip");
            stack.setItemMeta(m);

            Bukkit.getOnlinePlayers().forEach(player -> {
                player.setHealth(20);
                player.setSaturation(20);
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                player.setWalkSpeed(0.2f);
                player.setGameMode(GameMode.SURVIVAL);
                player.getPassengers().forEach(Entity::remove);
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
                player.teleport(Bukkit.getWorld("world").getSpawnLocation());
                player.setScoreboard(ForceItemBattle.getGamemanager().getBoard());
                player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);

                ArmorStand itemDisplay = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, 2, 0), EntityType.ARMOR_STAND);
                itemDisplay.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(player)));
                itemDisplay.setInvisible(true);
                itemDisplay.setInvulnerable(true);
                itemDisplay.setGlowing(true);
                itemDisplay.setGravity(false);

                if (ForceItemBattle.getGamemanager().isPlayerInMaps(player)) {
                    player.setGameMode(GameMode.SURVIVAL);
                    if (!ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                        player.getInventory().setItem(4, stack);
                        ForceItemBattle.getInstance().logToFile(player.getName() + " -> " + getPlayerTeamSTRING(player));
                    } else ForceItemBattle.getInstance().logToFile(player.getName());
                } else {
                    player.setGameMode(GameMode.SPECTATOR);
                    ForceItemBattle.getInstance().logToFile(player.getName() + " -> Spectator");
                }
            });
            if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) ForceItemBattle.getBackpack().addToAllBp(stack);
            Bukkit.getWorld("world").setTime(0);
            ForceItemBattle.getTimer().setRunning(true);

            Bukkit.broadcastMessage(ChatColor.GOLD + "The game was started with " + num + " skips. " + ForceItemBattle.getInstance().getConfig().getInt("standard.countdown") + " minutes left.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "<standard.countdown> and <standard.jokers> in config have to be numbers");
        }
    }

    public void skipItem(String player) {
        Player p = Bukkit.getPlayer(player);
        if (!isPlayerInMaps(p)) return;
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
            /////////////////////////////////////// TEAMS ///////////////////////////////////////
            currentMaterialTeams.put(getPlayerTeamSTRING(p), generateMaterial());
        } else {
            currentMaterial.put(p.getUniqueId(), generateMaterial());
        }

        ArmorStand armorStand = (ArmorStand) p.getPassengers().get(0);
        armorStand.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(p)));

        ForceItemBattle.getTimer().sendActionBar();
    }

    public void finishGame() {
        //scoreboard in chat
        if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
            /////////////////////////////////////// TEAMS ///////////////////////////////////////
            Map<String, Integer> sortedMapDesc;
            sortedMapDesc = sortByValueTeams(scoreTeams, DESC);
            printMapTeams(sortedMapDesc);

            ForceItemBattle.getBackpack().clearAllBp();
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.setHealth(20);
                player.setSaturation(20);
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                //player.teleport(Bukkit.getWorld("world").getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(true);
                player.setFlySpeed(0.1f);
                if (player.isOp()) {
                    //player.getInventory().setItem(7, ForceItemBattle.getInvSettings().createGuiItem(Material.COMMAND_BLOCK_MINECART, ChatColor.YELLOW + "Settings", "right click to edit"));
                    //player.getInventory().setItem(8, ForceItemBattle.getInvSettings().createGuiItem(Material.LIME_DYE, ChatColor.GREEN + "Start", "right click to start"));
                    player.sendMessage(ChatColor.RED + "Use /reset to reset the world or use /start to start a new round.");
                }
            });
        } else {
            Map<UUID, Integer> sortedMapDesc;
            sortedMapDesc = sortByValue(score, DESC);
            //printMap(sortedMapDesc);


            Bukkit.getOnlinePlayers().forEach(player -> {
                player.setHealth(20);
                player.setSaturation(20);
                player.getInventory().clear();
                player.setLevel(0);
                player.setExp(0);
                //player.teleport(Bukkit.getWorld("world").getSpawnLocation());
                player.setGameMode(GameMode.ADVENTURE);
                player.getPassengers().forEach(Entity::remove);
                player.setPlayerListName(player.getName());
                player.setAllowFlight(true);
                player.setFlySpeed(0.1f);
                if (player.isOp()) {
                    //player.getInventory().setItem(7, ForceItemBattle.getInvSettings().createGuiItem(Material.COMMAND_BLOCK_MINECART, ChatColor.YELLOW + "Settings", "right click to edit"));
                    //player.getInventory().setItem(8, ForceItemBattle.getInvSettings().createGuiItem(Material.LIME_DYE, ChatColor.GREEN + "Start", "right click to start"));
                    player.sendMessage(ChatColor.RED + "Use /result to see the results from every player");
                }
            });
        }
    }

    public void decreaseDelay() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (delay.containsKey(player.getUniqueId())) {
                if (delay.get(player.getUniqueId()) > 0) {
                    delay.replace(player.getUniqueId(), delay.get(player.getUniqueId()) - 1);
                }
            }
        });
    }

    public boolean hasDelay(Player player) {
        return delay.get(player.getUniqueId()) > 0;
    }

    public void setDelay(Player player, int sek) {
        delay.put(player.getUniqueId(), sek);
    }

    public Map<UUID, Integer> sortByValue(Map<UUID, Integer> unsortMap, final boolean order) {
        List<Map.Entry<UUID, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    private static Map<String, Integer> sortByValueTeams(Map<String, Integer> unsortMap, final boolean order) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    private static void printMap(Map<UUID, Integer> map) {
        map.forEach((key, value) -> {
            Bukkit.broadcastMessage(ChatColor.GOLD.toString() + Bukkit.getPlayer(key).getName() + " | " + value);
            ForceItemBattle.getInstance().logToFile(Bukkit.getPlayer(key).getName() + " | " + value);
        });
    }

    private void printMapTeams(Map<String, Integer> map) {
        map.forEach((key, value) -> {
            switch (key) {
                case "team1": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.WHITE + "Team 1" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team2": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "Team 2" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team3": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.RED + "Team 3" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team4": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "Team 4" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team5": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "Team 5" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team6": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.AQUA + "Team 6" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team7": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.DARK_BLUE + "Team 7" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team8": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "Team 8" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                case "team9": {
                    if (value > 0) {
                        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Team 9" + ChatColor.GOLD + " | " + value + " | " + board.getTeam(key).getEntries());
                    }
                    break;
                }
                default:
                    break;
            }
            ForceItemBattle.getInstance().logToFile(key + " | " + value + " | " + board.getTeam(key).getEntries());
        });
    }

    public Scoreboard getBoard() {
        return board;
    }

    /////////////////////////////////////// TEAMS ///////////////////////////////////////

    public boolean isTeam(Player player, String teamName) {
        if (board.getEntryTeam(player.getName()) == null) return false;
        return board.getEntryTeam(player.getName()).getName().equalsIgnoreCase(teamName);
    }

    public void addPlayerToTeam(Player player, String team) {
        board.getTeam(team).addEntry(player.getDisplayName());
    }

    public void removePlayerFromTeam(Player player) {
        if (board.getEntryTeam(player.getName()) == null) return;
        board.getEntryTeam(player.getName()).removeEntry(player.getName());
    }

    public List<String> getPlayersInTeam(String team) {
        List<String> l = new ArrayList<String>();
        l.add("<< left click to choose >>");
        l.add("<< right click to remove >>");
        l.add("-- players ----------------");
        l.addAll(board.getTeam(team).getEntries());
        return l;
    }

    public Team getPlayerTeam(Player player) {
        return board.getEntryTeam(player.getName());
    }

    public String getPlayerTeamSTRING(Player player) {
        if (board.getEntryTeam(player.getName()) == null) return null;
        return board.getEntryTeam(player.getName()).getName();
    }

    public int getScoreTeams(String team) {
        return scoreTeams.get(team);
    }

    public int getTeamScoreFromPlayer(Player player) {
        return scoreTeams.get(getPlayerTeamSTRING(player));
    }

    public Material getMaterialTeamsFromPlayer(Player player) {
        return currentMaterialTeams.get(getPlayerTeamSTRING(player));
    }

    public Material getMaterialTeams(String team) {
        return currentMaterialTeams.get(team);
    }

    public ArrayList<ForceItem> getItemList(Player player) {
        return itemList.get(player.getUniqueId());
    }

    public Map<UUID, ArrayList<ForceItem>> getItemList() {
        return itemList;
    }

    public Map<UUID, Material> getCurrentMaterial() {
        return currentMaterial;
    }

    public Map<UUID, Integer> getScore() {
        return score;
    }

    public Map<UUID, Integer> getJokers() {
        return jokers;
    }

    public Map<UUID, Map<Integer, ItemStack[]>> getSavedInventory() {
        return savedInventory;
    }
}
