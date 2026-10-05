package forceitembattle.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/** Where a button sits belongs to its {@link Menu}; when it can be clicked belongs to the phase. */
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

    /** The spectate button flipped over, so it shares slot 8 and is not an opening button. */
    PLAY_ROUND(Material.ENDER_EYE, "<dark_gray>» <gray>Play game",
            Menu.LOBBY, 8, EnumSet.of(GameState.PRE_GAME), false),

    TELEPORTER(Material.COMPASS, "<dark_gray>» <yellow>Teleporter",
            Menu.RESULT, 3, EnumSet.of(GameState.END_GAME)),

    TO_OVERWORLD(Material.GRASS_BLOCK, "<dark_gray>» <dark_green>Overworld",
            Menu.RESULT, 5, EnumSet.of(GameState.END_GAME)),

    TO_NETHER(Material.NETHERRACK, "<dark_gray>» <red>Nether",
            Menu.RESULT, 6, EnumSet.of(GameState.END_GAME)),

    /** Shares ENDER_EYE with {@link #PLAY_ROUND}; harmless because the reader dispatches on {@link #markerKey()}. */
    TO_END(Material.ENDER_EYE, "<dark_gray>» <dark_purple>End",
            Menu.RESULT, 7, EnumSet.of(GameState.END_GAME)),

    SPECTATE_RESULT(Material.SPYGLASS, "<dark_gray>» <green>Spectate",
            Menu.RESULT, 8, EnumSet.of(GameState.END_GAME));

    public enum Menu {
        LOBBY,
        RESULT
    }

    /** The only thing the reader consults: dispatching on Material let a grass block teleport the player. */
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

    /** False for exactly one constant, {@link #PLAY_ROUND}, which only ever replaces another button. */
    public boolean isOpeningButton() {
        return this.openingButton;
    }

    /** The menu decides which buttons and where; the phase drops the inert ones. */
    public static List<MenuItem> openingBar(Menu menu, GameState state) {
        return Arrays.stream(values())
                .filter(menuItem -> menuItem.openingButton && menuItem.menu == menu && menuItem.isLiveIn(state))
                .toList();
    }

    /** Null rather than throwing: a marker from a renamed constant leaves an inert item, not a crash. */
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
