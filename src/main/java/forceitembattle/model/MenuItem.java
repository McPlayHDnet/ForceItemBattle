package forceitembattle.model;

import java.util.ArrayList;
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
 * <p>Which material means which action in which phase is a table. It used to be written as five
 * blocks that had to agree by eye — two writers handing out stacks, two click handlers switching on
 * raw {@link Material}, and a third block inside one of those handlers rewriting a slot — with
 * nothing relating the columns. What that produced is recorded in {@code CONTEXT.md § Menu Items}.
 *
 * <p><b>This names actions; it does not perform them.</b> The same split as {@link Admission}: an
 * enum constant is an answer, and one adapter turns the answer into behaviour. That is what keeps
 * this file free of {@code ItemStack}, inventories and the plugin graph, and therefore testable.
 * {@code PlayerOutfitter} writes a bar; {@code ClickableItemsListener} looks a button up and acts.
 *
 * <h2>Two columns, and which two</h2>
 *
 * <p><b>Where</b> a button sits is a property of its {@link Menu}, and <b>when</b> it can be
 * clicked is a property of the phase. Those look like the same column and are not, which cost a
 * wrong first attempt: it hung the slot off the phase, on the strength of Collection being slot 0
 * in the lobby and slot 2 on the result screen. That holds right up until a player who never joined
 * the round arrives at {@code END_GAME} — {@code END_GAME} shares the {@code LOBBY} admission with
 * {@code PRE_GAME} — and gets handed the lobby bar during the result screen. Keyed by phase, they
 * got the result screen's bar instead.
 *
 * <p>So: one slot per constant, fixed within its own bar; a set of phases saying when it answers a
 * click. The lobby bar at {@code END_GAME} then comes out as Collection and Achievements in their
 * lobby slots and no slot 8 at all, which is what fixed the dead spectate pearl without anyone
 * writing a phase check.
 */
public enum MenuItem {

    /** Opens the collection book. On both bars, in different slots, clickable in both phases. */
    COLLECTION(Material.WRITTEN_BOOK, "<dark_gray>» <dark_aqua>Collection",
            Menu.LOBBY, 0, EnumSet.of(GameState.PRE_GAME, GameState.END_GAME)),

    /** The result screen's copy of the same action, four slots to the left. */
    RESULT_COLLECTION(Material.WRITTEN_BOOK, "<dark_gray>» <dark_aqua>Collection",
            Menu.RESULT, 2, EnumSet.of(GameState.END_GAME)),

    ACHIEVEMENTS(Material.LIME_DYE, "<dark_gray>» <green>Achievements",
            Menu.LOBBY, 4, EnumSet.of(GameState.PRE_GAME, GameState.END_GAME)),

    RESULT_ACHIEVEMENTS(Material.LIME_DYE, "<dark_gray>» <green>Achievements",
            Menu.RESULT, 1, EnumSet.of(GameState.END_GAME)),

    /**
     * Opt out of the round about to start. The only lobby button that is not clickable at
     * {@code END_GAME}: there is nothing to opt out of once a round has finished. It used to be
     * handed out there anyway, and no handler answered it.
     */
    SPECTATE_ROUND(Material.ENDER_PEARL, "<dark_gray>» <gray>Spectate game",
            Menu.LOBBY, 8, EnumSet.of(GameState.PRE_GAME)),

    /**
     * Opt back into the round about to start: what slot 8 becomes once a player has opted out.
     *
     * <p>{@linkplain #isOpeningButton Not part of the opening bar} — the one thing slot and phase
     * together cannot say. It really does live in slot 8 of the lobby bar, the same slot as
     * {@link #SPECTATE_ROUND}, because it is that button flipped over; a bar derived from placement
     * alone would write both into one slot and let declaration order pick the winner.
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
     * Harmless now because the reader never looks at the material: it reads {@link #markerKey()}
     * and gets a constant. Before that it was a real collision, hidden only by the two handlers
     * living in different halves of one file and never being read side by side.
     */
    TO_END(Material.ENDER_EYE, "<dark_gray>» <dark_purple>End",
            Menu.RESULT, 7, EnumSet.of(GameState.END_GAME)),

    SPECTATE_RESULT(Material.SPYGLASS, "<dark_gray>» <green>Spectate",
            Menu.RESULT, 8, EnumSet.of(GameState.END_GAME));

    /** Which hotbar a button belongs to. One per player state that has one. */
    public enum Menu {
        /** Waiting for a round, whether or not a result screen is up for other players. */
        LOBBY,
        /** The screen the round ends on. */
        RESULT
    }

    /**
     * Stamped into every button the plugin hands out, valued with the constant's {@link #name()}.
     *
     * <p>The reader consults this and nothing else. Dispatching on {@link Material} meant anything
     * that looked like a button was one: a grass block a player was holding to build with
     * teleported them, a spyglass they picked up dropped them into spectator mode, and an ender
     * pearl before a round quietly took them out of it. A marker is how {@code CustomMaterials}
     * already tells its custom items from the vanilla ones they share a material with.
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

    /** Which hotbar slot this occupies on its own bar. */
    public int slot() {
        return this.slot;
    }

    /** Whether a click on this does anything in {@code state}. */
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
     * {@code state}.
     *
     * <p>Both arguments earn their place. The menu decides which buttons and where; the phase drops
     * the ones that would be inert. The lobby bar is the only place the two differ — it is laid out
     * in two phases, and at {@code END_GAME} the spectate pearl falls out of the answer on its own.
     */
    public static List<MenuItem> openingBar(Menu menu, GameState state) {
        List<MenuItem> bar = new ArrayList<>();
        for (MenuItem menuItem : values()) {
            if (menuItem.openingButton && menuItem.menu == menu && menuItem.isLiveIn(state)) {
                bar.add(menuItem);
            }
        }
        return bar;
    }

    /**
     * The constant a marker value names, or {@code null} if it names nothing.
     *
     * <p>Null rather than an exception: a value left behind by a constant that has since been
     * renamed should leave an inert item in someone's hand, not throw inside an event handler.
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
