package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.preset.GamePreset;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.GameState;
import forceitembattle.util.ItemBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class Gamemanager {

    private static final Material JOKER_MATERIAL = Material.BARRIER;
    private final ForceItemBattle forceItemBattle;
    @Getter
    private final Map<UUID, ForceItemPlayer> forceItemPlayerMap;
    @Getter
    private final Map<UUID, Map<Integer, Map<Integer, ItemStack>>> savedInventory = new HashMap<>();
    @Getter
    @Setter
    private GameState currentGameState;
    @Setter
    private GamePreset currentGamePreset;

    public Gamemanager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.currentGameState = GameState.PRE_GAME;
        this.currentGamePreset = null;

        this.forceItemPlayerMap = new HashMap<>();
    }

    public static Material getJokerMaterial() {
        return JOKER_MATERIAL;
    }

    public static ItemStack getJokers(int amount) {
        return new ItemBuilder(JOKER_MATERIAL)
                .setAmount(amount)
                .setDisplayName("§8» §5Skip")
                .getItemStack();
    }

    public static boolean isJoker(Material material) {
        return material == Material.BARRIER;
    }

    public static boolean isJoker(ItemStack itemStack) {
        return isJoker(itemStack.getType());
    }

    public void addPlayer(Player player, ForceItemPlayer forceItemPlayer) {
        this.forceItemPlayerMap.put(player.getUniqueId(), forceItemPlayer);
        this.forceItemBattle.getStatsManager().createPlayerStats(forceItemPlayer);
    }

    public Material generateMaterial() {
        return this.forceItemBattle.getItemDifficultiesManager().getHardMaterial();
    }

    public String getCurrentMaterialName(ForceItemPlayer forceItemPlayer) {
        return WordUtils.capitalizeFully(forceItemPlayer.getCurrentMaterial().toString().replace("_", " "));
    }

    public String formatMaterialName(String material) {
        String materialName = WordUtils.capitalizeFully(material.replace("_", " "));
        String[] wordsToIgnore = {"and", "with", "of", "on", "a", "the"};
        for (String word : wordsToIgnore) {
            materialName = materialName.replace(WordUtils.capitalize(word), word.toLowerCase());
        }
        return materialName.replace(" ", "_");
    }

    public void initializeMats() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ForceItemPlayer forceItemPlayer = this.getForceItemPlayer(player.getUniqueId());
            forceItemPlayer.setCurrentScore(0);
            forceItemPlayer.setCurrentMaterial(this.generateMaterial());
        });
    }

    public void forceSkipItem(Player player) {
        if (!forceItemPlayerExist(player.getUniqueId())) {
            return;
        }

        ForceItemPlayer gamePlayer = getForceItemPlayer(player.getUniqueId());
        gamePlayer.setCurrentMaterial(this.generateMaterial());

        if (!this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.NETHER)) {
            gamePlayer.updateItemDisplay();
        }

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
        list.sort((o1, o2) -> {
            if (order) {
                if (o1.getValue().getCurrentScore().compareTo(o2.getValue().getCurrentScore()) == 0)
                    return o1.getKey().compareTo(o2.getKey());
                return o1.getValue().getCurrentScore().compareTo(o2.getValue().getCurrentScore());
            } else {
                return o2.getValue().getCurrentScore().compareTo(o1.getValue().getCurrentScore()) == 0
                        ? o2.getKey().compareTo(o1.getKey())
                        : o2.getValue().getCurrentScore().compareTo(o1.getValue().getCurrentScore());
            }
        });
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    public boolean forceItemPlayerExist(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid) != null;
    }

    public ForceItemPlayer getForceItemPlayer(UUID uuid) {
        return this.forceItemPlayerMap.get(uuid);
    }

    public boolean isPreGame() {
        return this.getCurrentGameState() == GameState.PRE_GAME;
    }

    public boolean isPausedGame() {
        return this.getCurrentGameState() == GameState.PAUSED_GAME;
    }

    public boolean isMidGame() {
        return this.getCurrentGameState() == GameState.MID_GAME;
    }

    public boolean isEndGame() {
        return this.getCurrentGameState() == GameState.END_GAME;
    }

    public GamePreset currentGamePreset() {
        return currentGamePreset;
    }
}
