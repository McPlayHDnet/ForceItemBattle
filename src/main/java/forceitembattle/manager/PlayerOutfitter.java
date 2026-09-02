package forceitembattle.manager;

import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.GameItems;
import forceitembattle.model.GameState;
import forceitembattle.model.MenuItem;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Puts a player into one of the states a round holds them in.
 *
 * <p>The adapter half of {@link RoundSetup}: everything here is a Bukkit write, and every number and
 * material it writes was decided elsewhere. That is why {@link #toResultScreen} is handed its
 * destination rather than looking one up — a world lookup would be a decision, and decisions do not
 * live in the adapter.
 */
public final class PlayerOutfitter {

    private static final int JOKER_SLOT = 4;

    private PlayerOutfitter() {
    }

    public static void toSpectator(Player player) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * @param jokersOnHotbar zero means no stack at all, which is not the same as a stack of zero
     */
    public static void toPlayer(Player player, int jokersOnHotbar) {
        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();

        // Before the tools: addItem fills the first free slot, so handing out tools first would put
        // one of them in the joker slot.
        if (jokersOnHotbar > 0) {
            player.getInventory().setItem(JOKER_SLOT, GameItems.jokers(jokersOnHotbar));
        }

        for (Material tool : RoundSetup.STARTING_KIT) {
            player.getInventory().addItem(new ItemStack(tool));
        }

        player.setLevel(0);
        player.setExp(0);
        player.setWalkSpeed(0.2f);
        player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
        dismount(player);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setGameMode(GameMode.SURVIVAL);
        player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
    }

    /**
     * Entered from two directions — {@code finishGame} sweeps everyone online into it, and a player
     * who was offline when the round ended enters it on rejoin.
     *
     * @param resultSpawn where the room is gathered, or {@code null} to leave them where they stand.
     *                    Nullable because {@code Dimension.OVERWORLD.world()} is.
     */
    public static void toResultScreen(Player player, @Nullable Location resultSpawn) {
        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        // Bukkit refuses to teleport an entity that is carrying a passenger — teleport returns false
        // and does nothing — so this has to come first.
        dismount(player);
        if (resultSpawn != null) {
            player.teleport(resultSpawn);
        }
        player.setGameMode(GameMode.CREATIVE);
        player.playerListName(Component.text(player.getName()));

        giveOpeningBar(player, MenuItem.Menu.RESULT, GameState.END_GAME);
    }

    /**
     * Copies the list first: Bukkit makes "no guarantees as to its mutability", and a live view
     * throws {@code ConcurrentModificationException} when removed from while walking it.
     */
    private static void dismount(Player player) {
        List.copyOf(player.getPassengers()).forEach(Entity::remove);
    }

    /**
     * Someone waiting for a round to start — and, because {@code END_GAME} shares the {@code LOBBY}
     * admission with {@code PRE_GAME}, someone who arrives while a result screen is still up. The
     * spectate button handed out here is only answered by the {@code PRE_GAME} click handler, so at
     * {@code END_GAME} it is a dead button. Left that way on purpose.
     */
    public static void toLobby(Player player, GameState phase) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);

        giveOpeningBar(player, MenuItem.Menu.LOBBY, phase);
    }

    /** Hands one team member their share of the pool. Offline members are skipped. */
    public static void giveJokerShare(Player player, int jokers) {
        if (player == null || !player.isOnline() || jokers <= 0) {
            return;
        }
        player.getInventory().setItem(JOKER_SLOT, GameItems.jokers(jokers));
    }

    private static void giveOpeningBar(Player player, MenuItem.Menu menu, GameState phase) {
        for (MenuItem menuItem : MenuItem.openingBar(menu, phase)) {
            setButton(player, menuItem);
        }
    }

    /** Public because the spectate toggle replaces slot 8 in place rather than relaying the bar. */
    public static void setButton(Player player, MenuItem menuItem) {
        player.getInventory().setItem(menuItem.slot(), buttonStack(menuItem));
    }

    /**
     * {@code ItemFlag.values()} goes on every button, not just the written book: a menu button is
     * decoration and should never show mechanical tooltip text.
     */
    private static ItemStack buttonStack(MenuItem menuItem) {
        return new ItemBuilder(menuItem.material())
                .setDisplayName(menuItem.label())
                .addItemFlags(ItemFlag.values())
                .setPersistentData(MenuItem.markerKey(), PersistentDataType.STRING, menuItem.name())
                .getItemStack();
    }

    /**
     * The button this stack is, or {@code null} if it is not one. Never looks at the material, which
     * is the point: an item that merely looks like a button is not one, however it was obtained.
     */
    @Nullable
    public static MenuItem buttonOf(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        return MenuItem.byMarker(itemMeta.getPersistentDataContainer()
                .get(MenuItem.markerKey(), PersistentDataType.STRING));
    }
}
