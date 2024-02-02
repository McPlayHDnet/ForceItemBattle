package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItem;
import forceitembattle.util.ForceItemPlayer;
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

    private final Map<UUID, ForceItemPlayer> forceItemPlayerMap;

    public Map<UUID, Map<Integer, ItemStack[]>> savedInventory = new HashMap<>();

    public GameState currentGameState;

    public Gamemanager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.currentGameState = GameState.PRE_GAME;

        this.forceItemPlayerMap = new HashMap<>();
    }

    public void addPlayer(Player player, ForceItemPlayer forceItemPlayer) {
        this.forceItemPlayerMap.put(player.getUniqueId(), forceItemPlayer);
    }

    public Material generateMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().getHardMaterial();
    }

    public String getCurrentMaterialName(ForceItemPlayer forceItemPlayer) {
        return WordUtils.capitalizeFully(forceItemPlayer.currentMaterial().toString().replace("_", " "));
    }

    public void initializeMats() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
            forceItemPlayer.setCurrentScore(0);
            forceItemPlayer.setCurrentMaterial(this.generateMaterial());
        });
    }

    public void forceSkipItem(String player) {
        Player p = Bukkit.getPlayer(player);

        //ArmorStand armorStand = (ArmorStand) p.getPassengers().get(0);
        //armorStand.getEquipment().setHelmet(new ItemStack(this.getCurrentMaterial(p)));

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

    public Map<UUID, ForceItemPlayer> sortByValue(Map<UUID, ForceItemPlayer> unsortMap, final boolean order) {
        List<Map.Entry<UUID, ForceItemPlayer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().currentScore().compareTo(o2.getValue().currentScore()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().currentScore().compareTo(o2.getValue().currentScore()) : o2.getValue().currentScore().compareTo(o1.getValue().currentScore()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().currentScore().compareTo(o1.getValue().currentScore()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    public boolean forceItemPlayerExist(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid) != null;
    }

    public ForceItemPlayer getForceItemPlayer(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid);
    }

    public Map<UUID, ForceItemPlayer> forceItemPlayerMap() {
        return this.forceItemPlayerMap;
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

    public Map<UUID, Map<Integer, ItemStack[]>> getSavedInventory() {
        return savedInventory;
    }
}
