package forceitembattle.manager;

import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.GameItems;
import forceitembattle.model.GameState;
import forceitembattle.model.JokerSpend;
import forceitembattle.model.MenuItem;
import java.util.List;
import java.util.OptionalInt;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Puts a player into one of the states a round holds them in. See {@code CONTEXT.md § Player Outfitting}.
 *
 * <p>The adapter half of {@link RoundSetup}: every number and material it writes was decided
 * elsewhere, which is why {@link #toResultScreen} is handed its destination rather than looking
 * one up.
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

    /** @param resultSpawn where the room gathers, or {@code null} to leave them where they stand */
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

    /** Copies first: a live passenger view throws {@code ConcurrentModificationException}. */
    private static void dismount(Player player) {
        List.copyOf(player.getPassengers()).forEach(Entity::remove);
    }

    /**
     * Also reached at {@code END_GAME}, which shares the LOBBY admission. The spectate button is
     * answered only by the {@code PRE_GAME} handler, so there it is a dead button — left so on purpose.
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
     * The single writer of the joker stack; zero removes it. {@link JokerSpend} decides the number.
     *
     * <p>Finds the stack rather than assuming slot 4, because players move it. Slot 4 is the
     * fallback for someone holding none, which is what a fresh round-start write is.
     */
    public static void setJokerStack(Player player, int amount) {
        PlayerInventory inventory = player.getInventory();
        int slot = inventory.first(GameItems.jokerMaterial());
        if (slot == -1) {
            slot = JOKER_SLOT;
        }

        inventory.setItem(slot, amount > 0 ? GameItems.jokers(amount) : null);
    }

    /** The same lookup {@link #setJokerStack} writes through, so reader and writer cannot disagree. */
    public static OptionalInt jokerStackIn(Player player) {
        int slot = player.getInventory().first(GameItems.jokerMaterial());
        if (slot == -1) {
            return OptionalInt.empty();
        }

        ItemStack stack = player.getInventory().getItem(slot);
        return stack == null ? OptionalInt.empty() : OptionalInt.of(stack.getAmount());
    }

    /** {@code ItemFlag.values()} on every button: they are decoration, never mechanical tooltips. */
    private static ItemStack buttonStack(MenuItem menuItem) {
        return new ItemBuilder(menuItem.material())
                .setDisplayName(menuItem.label())
                .addItemFlags(ItemFlag.values())
                .setPersistentData(MenuItem.markerKey(), PersistentDataType.STRING, menuItem.name())
                .getItemStack();
    }

    /** Never looks at the material: an item that merely looks like a button is not one. */
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
