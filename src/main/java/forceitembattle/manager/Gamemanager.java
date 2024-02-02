package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItem;
import forceitembattle.util.GameState;
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

    private ForceItemBattle forceItemBattle;
    private final Map<UUID, Integer> score = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> jokers = new HashMap<>();
    private final Map<UUID, Material> currentMaterial = new HashMap<UUID, Material>();
    private final Map<UUID, ArrayList<ForceItem>> itemList = new HashMap<UUID, ArrayList<ForceItem>>();

    public Map<UUID, Map<Integer, ItemStack[]>> savedInventory = new HashMap<>();

    public GameState currentGameState;

    public Gamemanager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.currentGameState = GameState.PRE_GAME;
    }

    public boolean isPlayerInMaps(Player player) {
        boolean value = score.containsKey(player.getUniqueId());
        if (!currentMaterial.containsKey(player.getUniqueId())) value = false;
        return value;
    }

    public void checkItem(Player player, Material material, boolean usedSkip) {
        if (!isPlayerInMaps(player)) return;
        if (material == currentMaterial.get(player.getUniqueId())) {
            score.put(player.getUniqueId(), score.get(player.getUniqueId()) + 1);
            ArrayList<ForceItem> mat = itemList.get(player.getUniqueId());
            mat.add(new ForceItem(material, this.forceItemBattle.getTimer().formatSeconds(this.forceItemBattle.getTimer().getTime()), usedSkip));
            itemList.put(player.getUniqueId(), mat);
            currentMaterial.put(player.getUniqueId(), generateMaterial());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);

            ArmorStand armorStand = (ArmorStand) player.getPassengers().get(0);
            armorStand.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(player)));

            this.forceItemBattle.logToFile("[" + this.forceItemBattle.getTimer().getTime() + "] | " + player.getName() + " got item: " + material.toString() + " | new points: " + score.get(player.getUniqueId()));
        }
        
    }

    public Material generateMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().getHardMaterial();
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
        Bukkit.getOnlinePlayers().forEach(player -> {
            score.put(player.getUniqueId(), 0);
            currentMaterial.put(player.getUniqueId(), generateMaterial());

            ArrayList<ForceItem> mat = new ArrayList<>();
            itemList.put(player.getUniqueId(), mat);
        });
    }

    public void skipItem(String player) {
        Player p = Bukkit.getPlayer(player);
        if (!isPlayerInMaps(p)) return;
        currentMaterial.put(p.getUniqueId(), generateMaterial());

        ArmorStand armorStand = (ArmorStand) p.getPassengers().get(0);
        armorStand.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(p)));

        this.forceItemBattle.getTimer().sendActionBar();
    }

    public void finishGame() {
        this.setCurrentGameState(GameState.END_GAME);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setHealth(20);
            player.setSaturation(20);
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.teleport(Bukkit.getWorld("world").getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
            player.getPassengers().forEach(Entity::remove);
            player.setPlayerListName(player.getName());
            player.setAllowFlight(true);
            player.setFlySpeed(0.1f);
            if (player.isOp()) {
                player.sendMessage(ChatColor.RED + "Use /result to see the results from every player");
            }
        });
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

    public boolean isPreGame() {
        return this.getCurrentGameState() == GameState.PRE_GAME;
    }

    public boolean isMidGame() {
        return this.getCurrentGameState() == GameState.MID_GAME;
    }

    public boolean isEndGame() {
        return this.getCurrentGameState() == GameState.END_GAME;
    }

    public void setCurrentGameState(GameState gameState) {
        this.currentGameState = gameState;
    }

    public GameState getCurrentGameState() {
        return currentGameState;
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
