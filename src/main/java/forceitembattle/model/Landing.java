package forceitembattle.model;

import org.bukkit.Material;

/**
 * Whether a scatter destination has anything to stand on.
 *
 * <p>Both scatter paths in {@code PortalListener} throw a player thousands of blocks out and drop
 * them onto the highest block in the column they land in. When the column is empty — genuinely
 * unbuilt terrain, or the void — there is no highest block, and the destination is a spot in mid
 * air with nothing beneath it. This is the rule that decides whether to put a floor there.
 *
 * <h2>Why this is a class and not an {@code if}</h2>
 *
 * <p>It was an {@code if}, inline in the teleport, and it was wrong for as long as it existed:
 *
 * <pre>{@code if (!block.getType().isBlock()) block.setType(Material.STONE);}</pre>
 *
 * <p>{@link Material#isBlock()} asks whether a material is a <em>placeable block type</em>, not
 * whether there is a block present. It is {@code true} for {@code AIR}, and for {@code WATER} and
 * {@code LAVA} too — so the condition was false for every material a player can land on and the
 * floor was never placed. The guard read plausibly and did nothing, which is the shape a bug takes
 * when the rule it encodes lives somewhere no test can reach it.
 *
 * <p><b>{@link Material#isAir()} and not {@link Material#isSolid()}</b>, deliberately. {@code
 * isSolid()} is false for water, lava, snow layers and tall grass, so it would plug an ocean
 * landing with a stone column and leave a stone scar on every snowy or grassy arrival. Air is
 * exactly the case the original guard was reaching for: nothing there at all.
 */
public final class Landing {

    private Landing() {
    }

    /**
     * Whether a floor has to be placed, given the block directly beneath the destination.
     *
     * @param below the material at destination minus one block, as
     *              {@code World#getHighestBlockYAt} left it
     */
    public static boolean needsFloor(Material below) {
        return below.isAir();
    }
}
