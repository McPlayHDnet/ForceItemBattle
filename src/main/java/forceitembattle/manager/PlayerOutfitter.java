package forceitembattle.manager;

import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.RoundSetup;
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

/**
 * Puts a player into one of the states a round holds them in.
 *
 * <p>The adapter half of {@link RoundSetup}: everything here is a Bukkit write, and every number
 * and material it writes was decided elsewhere. That split is the whole point — the decisions are
 * testable without a server precisely because this class holds all the parts that are not. It is
 * also why {@link #toResultScreen} is handed its destination rather than looking one up: a world
 * lookup would be a decision, and decisions do not live in the adapter.
 *
 * <p>There are four states and {@code Admission} names them all, so every arriving player reaches
 * exactly one of these. That was not true before: two of the four lived here and the other two
 * lived as loose bodies in {@code Gamemanager.finishGame} and {@code PlayerLifecycleListener} —
 * where the result-screen state existed twice, in copies that had drifted apart. Full account in
 * {@code CONTEXT.md § Player Outfitting}.
 *
 * <p>Slot 4 is the joker slot by convention, which is why it is written before the tools rather
 * than after: {@code addItem} fills the first free slot, and handing out tools first would put one
 * of them where the joker belongs.
 */
public final class PlayerOutfitter {

    /** The hotbar slot the joker stack lives in. */
    private static final int JOKER_SLOT = 4;

    private PlayerOutfitter() {
    }

    /** Someone watching rather than playing: no inventory, no survival, no level bar. */
    public static void toSpectator(Player player) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * Someone playing the round: full health, a clean inventory, their jokers and the starting kit.
     *
     * @param jokersOnHotbar from {@link RoundSetup#jokersOnHotbar}; zero means no stack at all,
     *                       which is not the same as a stack of zero
     */
    public static void toPlayer(Player player, int jokersOnHotbar) {
        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();

        if (jokersOnHotbar > 0) {
            player.getInventory().setItem(JOKER_SLOT, Gamemanager.getJokers(jokersOnHotbar));
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
     * Someone on the result screen: the round is over and they are looking at what it produced.
     *
     * <p>Entered from two directions — {@code finishGame} sweeps everyone online into it, and a
     * player who was <em>offline</em> when the round ended enters it on rejoin. The second path
     * used to be a thinner copy of the first, so a rejoiner arrived still holding the health,
     * mount, level and coloured tab name they disconnected with. Both go through here now.
     *
     * <p>The statement order came over from {@code finishGame} untouched but for one move, and
     * that move is a bug fix rather than a tidy-up. {@code finishGame} teleported <em>before</em>
     * removing passengers, and Bukkit refuses to teleport an entity that is carrying one â€”
     * {@code teleport} returns false and does nothing. So a player who finished a round with
     * anything riding them was never brought to the result spawn; they stayed wherever the round
     * left them, alone, while the rest of the room gathered. Dismounting first is what makes the
     * teleport actually happen, and it is the only reordering here.
     *
     * @param resultSpawn where the room is gathered, or {@code null} to leave them where they
     *                    stand. Nullable because {@code Dimension.OVERWORLD.world()} is.
     */
    public static void toResultScreen(Player player, @Nullable Location resultSpawn) {
        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        dismount(player);
        if (resultSpawn != null) {
            player.teleport(resultSpawn);
        }
        player.setGameMode(GameMode.CREATIVE);
        player.playerListName(Component.text(player.getName()));

        giveResultScreenItems(player);
    }

    /**
     * Removes whatever is riding the player.
     *
     * <p>The one line that did not come over verbatim. Both bodies had
     * {@code player.getPassengers().forEach(Entity::remove)}, which removes from the list it is
     * walking. Bukkit's javadoc says the returned list is "not directly linked" to the entity but
     * in the same breath makes "no guarantees as to its mutability" â€” Paper returns a copy, so
     * this has always been safe in production, and MockBukkit returns a live view, so it throws
     * {@code ConcurrentModificationException} the moment a test puts a real passenger on a player.
     *
     * <p>Copying first is correct against the contract as written rather than against one
     * implementation of it, and it is what made the dismount half of the result-screen state
     * testable at all.
     */
    private static void dismount(Player player) {
        List.copyOf(player.getPassengers()).forEach(Entity::remove);
    }

    /**
     * Someone waiting for a round to start — and, because {@code END_GAME} shares the
     * {@code LOBBY} admission with {@code PRE_GAME}, someone who arrives while a result screen is
     * still up.
     *
     * <p>That second case is why slot 8 is worth a note: the spectate button handed out here is
     * only answered by the {@code PRE_GAME} click handler, so at {@code END_GAME} it is a dead
     * button. Left exactly as it was on purpose — see {@code CONTEXT.md § Player Outfitting}.
     */
    public static void toLobby(Player player) {
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().setItem(0, new ItemBuilder(Material.WRITTEN_BOOK)
                .setDisplayName("<dark_gray>» <dark_aqua>Collection")
                .addItemFlags(ItemFlag.values())
                .getItemStack());
        player.getInventory().setItem(4, new ItemBuilder(Material.LIME_DYE)
                .setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_PEARL)
                .setDisplayName("<dark_gray>» <gray>Spectate game").getItemStack());
    }

    /** Hands one team member their share of the pool. Offline members are skipped. */
    public static void giveJokerShare(Player player, int jokers) {
        if (player == null || !player.isOnline() || jokers <= 0) {
            return;
        }
        player.getInventory().setItem(JOKER_SLOT, Gamemanager.getJokers(jokers));
    }

    /**
     * The result screen's buttons.
     *
     * <p>Was {@code Gamemanager.giveSpectatorItems}, public only so the rejoin path could reach it
     * from another package, and named for spectators although everyone on the result screen is
     * handed it — the winner included.
     */
    private static void giveResultScreenItems(Player player) {
        player.getInventory().setItem(1, new ItemBuilder(Material.LIME_DYE)
                .setDisplayName("<dark_gray>» <green>Achievements").getItemStack());
        player.getInventory().setItem(2, new ItemBuilder(Material.WRITTEN_BOOK)
                .setDisplayName("<dark_gray>» <dark_aqua>Collection")
                .addItemFlags(ItemFlag.values())
                .getItemStack());
        player.getInventory().setItem(3, new ItemBuilder(Material.COMPASS)
                .setDisplayName("<dark_gray>» <yellow>Teleporter").getItemStack());
        player.getInventory().setItem(5, new ItemBuilder(Material.GRASS_BLOCK)
                .setDisplayName("<dark_gray>» <dark_green>Overworld").getItemStack());
        player.getInventory().setItem(6, new ItemBuilder(Material.NETHERRACK)
                .setDisplayName("<dark_gray>» <red>Nether").getItemStack());
        player.getInventory().setItem(7, new ItemBuilder(Material.ENDER_EYE)
                .setDisplayName("<dark_gray>» <dark_purple>End").getItemStack());
        player.getInventory().setItem(8, new ItemBuilder(Material.SPYGLASS)
                .setDisplayName("<dark_gray>» <green>Spectate").getItemStack());
    }
}
