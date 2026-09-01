package forceitembattle.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * One button on a menu hotbar: what it is made of, what it says, which bar it belongs to and where,
 * and when it can be clicked.
 *
 * <p><b>This names actions; it does not perform them.</b> An enum constant is an answer and one
 * adapter turns it into behaviour, which is what keeps this file free of {@code ItemStack},
 * inventories and the plugin graph, and therefore testable. {@code PlayerOutfitter} writes a bar;
 * {@code ClickableItemsListener} looks a button up and acts.
 *
 * <p><b>Where</b> a button sits belongs to its {@link Menu}; <b>when</b> it can be clicked belongs to
 * the phase. These look like one column and are not — hanging the slot off the phase breaks as soon
 * as a player who never joined the round reaches {@code END_GAME}, which shares the {@code LOBBY}
 * admission with {@code PRE_GAME} and so is handed the lobby bar during the result screen.
 */
public enum MenuItem {

    COLLECTION(Material.WRITTEN_BOOK, "<dark_gray>» <dark_aqua>Collection",
            Menu.LOBBY, 0, EnumSet.of(GameState.PRE_GAME, GameState.END_GAME)),

    RESULT_COLLECTION(Material.WRITTEN_BOOK, "<dark_gray>» <dark_aqua>Collection",
            Menu.RESULT, 2, EnumSet.of(GameState.END_GAME)),

    ACHIEVEMENTS(Material.LIME_DYE, "<dark_gray>» <green>Achievements",
            Menu.LOBBY, 4, EnumSet.of(GameState.PRE_GAME, GameState.END_GAME)),

    RESULT_ACHIEVEMENTS(Material.LIME_DYE, "<dark_gray>» <green>Achievements",
            Menu.RESULT, 1, EnumSet.of(GameState.END_GAME)),

    /** The only lobby button not clickable at {@code END_GAME}: nothing to opt out of by then. */
    SPECTATE_ROUND(Material.ENDER_PEARL, "<dark_gray>» <gray>Spectate game",
            Menu.LOBBY, 8, EnumSet.of(GameState.PRE_GAME)),

    /**
     * What slot 8 becomes once a player has opted out. {@linkplain #isOpeningButton Not an opening
     * button} — it shares slot 8 with {@link #SPECTATE_ROUND} because it is that button flipped over,
     * and a bar derived from placement alone would let declaration order pick between them.
     */
    PLAY_ROUND(Material.ENDER_EYE, "<dark_gray>» <gray>Play game",
            Menu.LOBBY, 8, EnumSet.of(GameState.PRE_GAME), false),

    TELEPORTER(Material.COMPASS, "<dark_gray>» <yellow>Teleporter",
            Menu.RESULT, 3, EnumSet.of(GameState.END_GAME)),

    TO_OVERWORLD(Material.GRASS_BLOCK, "<dark_gray>» <dark_green>Overworld",
            Menu.RESULT, 5, EnumSet.of(GameState.END_GAME)),

    TO_NETHER(Material.NETHERRACK, "<dark_gray>» <red>Nether",
            Menu.RESULT, 6, EnumSet.of(GameState.END_GAME)),

    /**
     * Shares {@link Material#ENDER_EYE} with {@link #PLAY_ROUND} and means something unrelated.
     * Harmless only because the reader dispatches on {@link #markerKey()}, never on the material.
     */
    TO_END(Material.ENDER_EYE, "<dark_gray>» <dark_purple>End",
            Menu.RESULT, 7, EnumSet.of(GameState.END_GAME)),

    SPECTATE_RESULT(Material.SPYGLASS, "<dark_gray>» <green>Spectate",
            Menu.RESULT, 8, EnumSet.of(GameState.END_GAME));

    /** Which hotbar a button belongs to. */
    public enum Menu {
        LOBBY,
        RESULT
    }

    /**
     * Stamped into every button the plugin hands out, valued with the constant's {@link #name()}.
     * The reader consults this and nothing else — dispatching on {@link Material} meant anything that
     * looked like a button was one, so a grass block held to build with teleported the player.
     */
    private static final NamespacedKey MARKER_KEY = new NamespacedKey("fib", "menu_item");

    private final Material material;
    private final String label;
    private final Menu menu;
    private final int slot;
    private final Set<GameState> livePhases;
    private final boolean openingButton;

    MenuItem(Material material, String label, Menu menu, int slot, Set<GameState> livePhases) {
        this(material, label, menu, slot, livePhases, true);
    }

    MenuItem(Material material, String label, Menu menu, int slot, Set<GameState> livePhases,
             boolean openingButton) {
        this.material = material;
        this.label = label;
        this.menu = menu;
        this.slot = slot;
        this.livePhases = livePhases;
        this.openingButton = openingButton;
    }

    public static NamespacedKey markerKey() {
        return MARKER_KEY;
    }

    public Material material() {
        return this.material;
    }

    /** MiniMessage, as the display name. */
    public String label() {
        return this.label;
    }

    public Menu menu() {
        return this.menu;
    }

    public int slot() {
        return this.slot;
    }

    public boolean isLiveIn(GameState state) {
        return this.livePhases.contains(state);
    }

    /**
     * Whether this is written when the bar is first laid out, as opposed to only ever replacing
     * another button in place. False for exactly one constant, {@link #PLAY_ROUND}.
     */
    public boolean isOpeningButton() {
        return this.openingButton;
    }

    /**
     * The bar as it is first handed out: every opening button on {@code menu} that is clickable in
     * {@code state}. The menu decides which buttons and where; the phase drops the inert ones.
     */
    public static List<MenuItem> openingBar(Menu menu, GameState state) {
        return Arrays.stream(values())
                .filter(menuItem -> menuItem.openingButton && menuItem.menu == menu && menuItem.isLiveIn(state))
                .toList();
    }

    /**
     * The constant a marker value names. Null rather than an exception: a value left behind by a
     * since-renamed constant should leave an inert item in someone's hand, not throw in a handler.
     */
    @Nullable
    public static MenuItem byMarker(@Nullable String marker) {
        if (marker == null) {
            return null;
        }
        for (MenuItem menuItem : values()) {
            if (menuItem.name().equals(marker)) {
                return menuItem;
            }
        }
        return null;
    }
}
