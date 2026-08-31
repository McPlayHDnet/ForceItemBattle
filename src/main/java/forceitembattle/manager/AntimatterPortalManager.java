package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Owns the Antimatter Depths portals: the one in the overworld ruin that a player opens with a
 * Totem of Antimatter, and the private Depths it drops them into.
 *
 * <p>A portal belongs to whoever opened it. That is the whole point of the mechanic — the Depths
 * behind it is a loot dungeon, so a shared portal would mean the first player through empties the
 * barrels and the vault for everyone. Ownership is enforced twice over: the portal surface is an
 * {@link ItemDisplay} hidden from every player but its owner, and the walk-in check only ever runs
 * against portals that player opened. A second player standing in the same frame sees nothing and
 * walks through empty air until they spend a totem of their own.
 *
 * <p>The portal surface is a single scaled item display rather than the grid of armour-stand
 * markers the older antimatter teleporter uses. Armour stands cannot be scaled, so covering a
 * 3x4 opening that way costs twelve entities, each with a helmet slot and marker flags; one display
 * with {@code scale = [3, 4, 1]} does the same job and can be hidden per-player, which armour stands
 * carrying a real helmet item cannot.
 */
public class AntimatterPortalManager implements Manager {

    /**
     * Marks portal surfaces so a restart can sweep up displays whose owner map did not survive.
     * Without it a stale portal would hang in the world visible to everyone, since the per-player
     * hide state lives only in memory.
     */
    private static final String PORTAL_TAG = "fib_antimatter_portal";

    /**
     * The structure registry. {@code Registry.STRUCTURE} is deprecated in favour of
     * {@link RegistryAccess}; looked up per call rather than held in a field, because a manager is
     * constructed before the server is fully up.
     */
    private static Registry<Structure> structureRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
    }


    private static final NamespacedKey ANTIMATTER_DIMENSION = new NamespacedKey("fib", "antimatter");
    private static final NamespacedKey DEPTHS_STRUCTURE = new NamespacedKey("fib", "antimatter_depths");
    private static final String PORTAL_MODEL = "antimatter_portal";

    /** The frame opening in the datapack structure: 3 blocks wide, 4 tall, 1 thick. */
    private static final float PORTAL_WIDTH = 3.0f;
    private static final float PORTAL_HEIGHT = 4.0f;

    /**
     * Where the portal sits relative to the vault that opens it. Both vaults face outward, away
     * from the frame, so the plane is {@value} blocks behind the vault along its facing, and the
     * opening's centre is 3.5 above the vault's own centre. Deriving it from the vault instead of
     * hardcoding structure coordinates means it survives the structure being rotated on placement.
     */
    private static final int PLANE_OFFSET = 2;
    private static final double OPENING_RISE = 3.5;

    /** How long the blindness lasts before the portal is revealed, in ticks. */
    private static final int REVEAL_DELAY = 45;

    /** How far out to look for a Depths, and how far from origin to start looking. */
    private static final int DEPTHS_SEARCH_RADIUS = 100;
    private static final int DEPTHS_SCATTER = 20_000;

    /** The start room's footprint, sorted, used to pick it out of the structure's piece list. */
    private static final int[] START_ROOM_SIZE = {19, 19, 48};

    private final ForceItemBattle plugin;
    private final Map<UUID, List<ActivePortal>> portalsByOwner = new HashMap<>();

    /**
     * Frames a player has already paid for but whose surface has not been spawned yet.
     *
     * <p>The totem is taken the moment the vault is clicked and the portal only appears
     * {@value #REVEAL_DELAY} ticks later, so for that window {@link #portalsByOwner} has no record
     * of it. Without this, a second click inside those two seconds — which spam-clicking a vault
     * produces easily — passed the duplicate check and cost a second totem for nothing.
     */
    private final Map<UUID, List<Location>> openingByOwner = new HashMap<>();
    private final Map<UUID, Location> depthsByPlayer = new HashMap<>();
    private final Map<UUID, Location> returnByPlayer = new HashMap<>();
    private final Map<UUID, ActivePortal> returnPortalByPlayer = new HashMap<>();
    private final Random random = new Random();

    public AntimatterPortalManager(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        // Portals never outlive the round that opened them, and their per-player visibility is
        // memory-only, so anything left tagged from a crash or restart has to go.
        Scheduler.runSync(() -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                    if (entity.getScoreboardTags().contains(PORTAL_TAG)) {
                        entity.remove();
                    }
                }
            }
        });
    }

    @Override
    public void disable() {
        this.portalsByOwner.values().stream()
                .flatMap(List::stream)
                .forEach(portal -> portal.display().remove());
        this.portalsByOwner.clear();
        this.openingByOwner.clear();
        this.depthsByPlayer.clear();
        this.returnByPlayer.clear();
        this.returnPortalByPlayer.values().forEach(portal -> portal.display().remove());
        this.returnPortalByPlayer.clear();
    }

    /**
     * Whether this block is a vault belonging to an Antimatter Depths portal.
     *
     * <p>Asks the vault what it takes rather than sniffing the blocks around it: the key item is
     * exactly what distinguishes this vault from the nether-star ones in the antimatter teleporter
     * and from any trial chamber vault, and it is set by the same datapack that builds the frame.
     */
    public boolean isPortalVault(Block block) {
        if (block.getType() != Material.VAULT) {
            return false;
        }
        return block.getState() instanceof org.bukkit.block.Vault vault
                && CustomMaterials.TOTEM_OF_ANTIMATTER.matches(vault.getKeyItem());
    }

    /**
     * Opens the portal in front of {@code vaultBlock} for {@code player}, consuming one totem.
     * Returns false when the player already has this portal open, so the caller can leave the
     * totem in their hand.
     */
    public boolean activate(Player player, Block vaultBlock) {
        Frame frame = frameOf(vaultBlock);
        if (frame == null) {
            return false;
        }

        if (portalAt(player, frame.centre()) != null || isOpening(player, frame.centre())) {
            player.sendMessage(Text.of("<dark_purple>This portal is already open for you."));
            return false;
        }

        World world = vaultBlock.getWorld();
        // Where the player stands to spend the totem is a known-good spot outside the frame, which
        // makes it the natural place to put them back down on the way home — anywhere derived from
        // the portal itself risks landing them inside it and bouncing them straight back.
        Location returnSpot = player.getLocation();
        this.openingByOwner.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>())
                .add(frame.centre());
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, REVEAL_DELAY + 20, 0, false, false));
        world.playSound(frame.centre(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.6f);
        world.strikeLightningEffect(frame.centre());

        Scheduler.runLaterSync(() -> {
            // Released first, so a player who logs out mid-reveal does not leave the frame
            // permanently marked as opening and unusable for the rest of the round.
            forgetOpening(player, frame.centre());
            if (!player.isOnline()) {
                return;
            }
            ItemDisplay display = spawnSurface(frame);
            hideFromEveryoneExcept(display, player);

            this.portalsByOwner.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>())
                    .add(new ActivePortal(frame.centre(), frame.region(), display, returnSpot));

            world.playSound(frame.centre(), Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 1.4f);
        }, REVEAL_DELAY);

        return true;
    }

    /** The portal {@code player} owns that they are currently standing in, or null. */
    @Nullable
    public ActivePortal portalPlayerIsStandingIn(Player player) {
        List<ActivePortal> portals = this.portalsByOwner.get(player.getUniqueId());
        if (portals == null) {
            return null;
        }
        for (ActivePortal portal : portals) {
            if (portal.region().contains(player.getLocation().toVector())) {
                return portal;
            }
        }
        return null;
    }

    /** Whether this player is standing in the return portal of their own Depths. */
    public boolean isInReturnPortal(Player player) {
        ActivePortal portal = this.returnPortalByPlayer.get(player.getUniqueId());
        return portal != null && portal.region().contains(player.getLocation().toVector());
    }

    /** Remembers where to put this player when they come back out of the Depths. */
    public void rememberReturn(Player player, ActivePortal portal) {
        this.returnByPlayer.put(player.getUniqueId(), portal.returnSpot());
    }

    /** Where this player came into the Depths from, or null if they never used a portal. */
    @Nullable
    public Location returnFor(Player player) {
        return this.returnByPlayer.get(player.getUniqueId());
    }

    /** Whether this world is the antimatter dimension. */
    public boolean isAntimatterWorld(World world) {
        return world.getKey().equals(ANTIMATTER_DIMENSION);
    }

    /**
     * Hides every open portal from a player who did not open it. Called on join, because
     * {@link Player#hideEntity} only applies to players who were online when the portal was spawned.
     *
     * <p>Covers the return portals down in the Depths as well as the ruins up top. They are just as
     * private — one is spawned per player on arrival, so a Depths that two players have somehow both
     * been given holds two frames stacked in the same place, each meant for one of them.
     */
    public void hideForeignPortals(Player player) {
        this.portalsByOwner.forEach((owner, portals) -> {
            if (owner.equals(player.getUniqueId())) {
                return;
            }
            portals.forEach(portal -> player.hideEntity(this.plugin, portal.display()));
        });
        this.returnPortalByPlayer.forEach((owner, portal) -> {
            if (owner.equals(player.getUniqueId())) {
                return;
            }
            player.hideEntity(this.plugin, portal.display());
        });
    }

    /**
     * This player's own Depths, picked once and kept for the round so going back through the portal
     * returns them to the same dungeon rather than a fresh one.
     */
    @Nullable
    public Location depthsFor(Player player) {
        Location known = this.depthsByPlayer.get(player.getUniqueId());
        if (known != null) {
            return known;
        }

        World antimatter = Bukkit.getWorld(ANTIMATTER_DIMENSION);
        if (antimatter == null) {
            this.plugin.getLogger().warning("Dimension " + ANTIMATTER_DIMENSION
                    + " is not loaded — is the FIB_Worldgen datapack enabled?");
            return null;
        }

        Structure depths = structureRegistry().get(DEPTHS_STRUCTURE);
        if (depths == null) {
            this.plugin.getLogger().warning("Structure " + DEPTHS_STRUCTURE + " is not registered.");
            return null;
        }

        // Scatter the search origin, then take the nearest Depths nobody has been given yet.
        //
        // The scatter alone is not enough. The structure set spaces these 156 chunks apart, so a
        // 20k box holds only about 256 of them, and two uniform draws collide about as often as a
        // birthday clash in a room of that size — around one game in ten at eight players, one in
        // two at twenty. Sharing a dungeon defeats the entire point of the private portal, since
        // whoever arrives first empties the barrels and the vault for both.
        //
        // Asking for an unexplored one is what actually makes it exclusive: locating with that flag
        // set marks the structure as referenced and later searches skip it, which is the same
        // mechanism that stops two ocean explorer maps pointing at one monument. It persists in the
        // chunk data, so it survives the reload a player logging out and back in causes.
        Location origin = new Location(antimatter,
                this.random.nextInt(-DEPTHS_SCATTER, DEPTHS_SCATTER), 64,
                this.random.nextInt(-DEPTHS_SCATTER, DEPTHS_SCATTER));

        var result = antimatter.locateNearestStructure(origin, depths, DEPTHS_SEARCH_RADIUS, true);
        if (result == null) {
            // The radius counts placement cells rather than chunks, so at this spacing it reaches
            // roughly a quarter of a million blocks out — running dry means every Depths in range
            // is already claimed, not that the search was too small.
            this.plugin.getLogger().warning("No unclaimed " + DEPTHS_STRUCTURE + " found within "
                    + DEPTHS_SEARCH_RADIUS + " placement cells of " + origin);
            return null;
        }

        Location landing = arrivalAtReturnPortal(player, antimatter, result.getLocation());
        if (landing == null) {
            return null;
        }
        this.depthsByPlayer.put(player.getUniqueId(), landing);
        return landing;
    }

    /**
     * Where to put a player arriving in this Depths, or null if this one cannot take them.
     *
     * <p>Refusing is the only safe answer when the return portal cannot be found. The Depths sits in
     * a void dimension and the way out is the frame in its start room; dropping someone in without
     * one — which an earlier version did, onto whatever ground it could find nearby — left them with
     * no way home at all, because {@link #isInReturnPortal} has nothing to match against. Dying was
     * the only exit. A refusal costs the player a walk back into the portal, and because the search
     * above has already claimed this Depths, stepping through again rolls a different one.
     */
    @Nullable
    private Location arrivalAtReturnPortal(Player player, World world, Location structureLocation) {
        Frame frame = findReturnFrame(world, structureLocation);
        if (frame == null) {
            this.plugin.getLogger().warning("No return portal frame found in the Depths near "
                    + structureLocation + " — refusing to send " + player.getName() + " there.");
            return null;
        }

        Location arrival = arrivalInFrontOf(frame);
        if (arrival == null) {
            this.plugin.getLogger().warning("Return portal frame at " + frame.centre()
                    + " has no standable side — refusing to send " + player.getName() + " there.");
            return null;
        }

        // Fill the frame, so the way home looks like the way in rather than an empty hole.
        ItemDisplay display = spawnSurface(frame);
        hideFromEveryoneExcept(display, player);
        this.returnPortalByPlayer.put(player.getUniqueId(),
                new ActivePortal(frame.centre(), frame.region(), display, arrival));

        return arrival;
    }

    /**
     * The reinforced deepslate frame of the Depths' return portal, in the start room.
     *
     * <p>Looks for the frame rather than for portal blocks inside it. The room used to carry a 3x3
     * of real {@code nether_portal} blocks and those were what this found, but they were a
     * placeholder — and vanilla would have broken them anyway, since a nether portal validates its
     * frame on neighbour updates and only accepts obsidian. The frame is the durable landmark, and
     * the surface that fills it is ours to draw.
     *
     * <p>The start room is found by its 48x19x19 footprint, which is unique in the pool, so the
     * search is confined to that one piece instead of sweeping chunks outward from the structure
     * root — the sweep cost about five seconds in chunk loads.
     */
    @Nullable
    private Frame findReturnFrame(World world, Location structureLocation) {
        int chunkX = structureLocation.getBlockX() >> 4;
        int chunkZ = structureLocation.getBlockZ() >> 4;

        for (GeneratedStructure generated : world.getStructures(chunkX, chunkZ)) {
            if (!DEPTHS_STRUCTURE.equals(structureRegistry().getKey(generated.getStructure()))) {
                continue;
            }
            for (StructurePiece piece : generated.getPieces()) {
                if (!isStartRoom(piece.getBoundingBox())) {
                    continue;
                }
                Frame frame = frameFromRing(world, piece.getBoundingBox());
                if (frame != null) {
                    return frame;
                }
            }
        }
        return null;
    }

    /**
     * Whether this bounding box is the Depths start room, by its 48x19x19 footprint. Compared
     * sorted, so it does not care how the structure was rotated on placement.
     */
    private boolean isStartRoom(BoundingBox box) {
        int[] dimensions = {
                (int) Math.round(box.getWidthX()),
                (int) Math.round(box.getHeight()),
                (int) Math.round(box.getWidthZ()),
        };
        java.util.Arrays.sort(dimensions);
        for (int index = 0; index < START_ROOM_SIZE.length; index++) {
            // Tolerate a block either way: whether a piece box counts its far edge is not worth
            // depending on.
            if (Math.abs(dimensions[index] - START_ROOM_SIZE[index]) > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Derives the portal opening from the ring of reinforced deepslate inside {@code box}.
     *
     * <p>The ring is the only reinforced deepslate in the start room, so its own bounds give the
     * frame: the opening is the interior, and whichever axis the ring is flat on is the plane.
     */
    @Nullable
    private Frame frameFromRing(World world, BoundingBox box) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean found = false;

        for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
            for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
                for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.REINFORCED_DEEPSLATE) {
                        continue;
                    }
                    found = true;
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                }
            }
        }
        if (!found) {
            return null;
        }

        boolean acrossX = minZ == maxZ;
        float width = (acrossX ? maxX - minX : maxZ - minZ) - 1;
        float height = maxY - minY - 1;
        if (width < 1 || height < 1) {
            return null;
        }

        Location centre = new Location(world,
                (minX + maxX + 1) / 2.0,
                (minY + maxY + 1) / 2.0,
                (minZ + maxZ + 1) / 2.0);
        BoundingBox region = BoundingBox.of(centre,
                acrossX ? width / 2 : 0.5, height / 2, acrossX ? 0.5 : width / 2);

        return new Frame(centre, region, acrossX ? 0.0f : 90.0f, width, height);
    }

    /**
     * Where to put a player arriving at {@code frame}: on the floor in front of it, facing it.
     *
     * <p>Which side is the room and which is the wall behind it is decided by looking, not assumed:
     * only one side has standable floor.
     */
    @Nullable
    private Location arrivalInFrontOf(Frame frame) {
        World world = frame.centre().getWorld();
        BlockFace[] sides = frame.yaw() == 0.0f
                ? new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH}
                : new BlockFace[]{BlockFace.EAST, BlockFace.WEST};

        int bottom = (int) Math.floor(frame.centre().getY() - frame.height() / 2);
        for (BlockFace side : sides) {
            Block standing = world.getBlockAt(
                    (int) Math.floor(frame.centre().getX()) + side.getModX(),
                    bottom,
                    (int) Math.floor(frame.centre().getZ()) + side.getModZ());
            if (standing.getType().isAir()
                    && standing.getRelative(BlockFace.UP).getType().isAir()
                    && standing.getRelative(BlockFace.DOWN).getType().isSolid()) {
                Location arrival = standing.getLocation().add(0.5, 0.0, 0.5);
                // Face into the room, not back at the portal you just came out of: the standable
                // side is the room side, so looking along it puts the stairs dead ahead.
                arrival.setDirection(new org.bukkit.util.Vector(side.getModX(), 0.0, side.getModZ()));
                return arrival;
            }
        }
        return null;
    }

    /** Works out where the portal surface goes from the vault that opens it. */
    @Nullable
    private Frame frameOf(Block vaultBlock) {
        if (!(vaultBlock.getBlockData() instanceof Directional directional)) {
            return null;
        }
        BlockFace facing = directional.getFacing();
        if (facing.getModY() != 0) {
            return null;
        }

        // Both vaults look outward, so the frame is behind them.
        BlockFace intoFrame = facing.getOppositeFace();
        Location centre = vaultBlock.getLocation().add(0.5, 0.5, 0.5)
                .add(intoFrame.getModX() * PLANE_OFFSET, OPENING_RISE, intoFrame.getModZ() * PLANE_OFFSET);

        boolean acrossX = facing.getModZ() != 0;
        // The plane spans width across the frame and height upward; it is only a block thick.
        BoundingBox region = BoundingBox.of(centre,
                acrossX ? PORTAL_WIDTH / 2 : 0.5,
                PORTAL_HEIGHT / 2,
                acrossX ? 0.5 : PORTAL_WIDTH / 2);

        // Yaw 0 leaves the model's face along Z; a frame running across X needs a quarter turn.
        float yaw = acrossX ? 0.0f : 90.0f;
        return new Frame(centre, region, yaw, PORTAL_WIDTH, PORTAL_HEIGHT);
    }

    private ItemDisplay spawnSurface(Frame frame) {
        return frame.centre().getWorld().spawn(frame.centre(), ItemDisplay.class, display -> {
            display.setItemStack(new ItemBuilder(Material.SNOWBALL)
                    .setCustomModelDataStrings(List.of(PORTAL_MODEL))
                    .getItemStack());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(4.0f);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Quaternionf(),
                    new Vector3f(frame.width(), frame.height(), 1.0f),
                    new Quaternionf()));
            display.setRotation(frame.yaw(), 0.0f);
            display.addScoreboardTag(PORTAL_TAG);
        });
    }

    private void hideFromEveryoneExcept(ItemDisplay display, Player owner) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(owner)) {
                online.hideEntity(this.plugin, display);
            }
        }
    }

    private boolean isOpening(Player player, Location centre) {
        List<Location> opening = this.openingByOwner.get(player.getUniqueId());
        return opening != null && opening.stream().anyMatch(pending -> sameFrame(pending, centre));
    }

    private void forgetOpening(Player player, Location centre) {
        List<Location> opening = this.openingByOwner.get(player.getUniqueId());
        if (opening == null) {
            return;
        }
        opening.removeIf(pending -> sameFrame(pending, centre));
        if (opening.isEmpty()) {
            this.openingByOwner.remove(player.getUniqueId());
        }
    }

    private boolean sameFrame(Location one, Location other) {
        return one.getWorld().equals(other.getWorld()) && one.distanceSquared(other) < 1.0;
    }

    @Nullable
    private ActivePortal portalAt(Player player, Location centre) {
        List<ActivePortal> portals = this.portalsByOwner.get(player.getUniqueId());
        if (portals == null) {
            return null;
        }
        return portals.stream()
                .filter(portal -> sameFrame(portal.centre(), centre))
                .findFirst()
                .orElse(null);
    }

    /** A portal surface standing in the world, owned by whoever opened it. */
    public record ActivePortal(Location centre, BoundingBox region, ItemDisplay display,
                              Location returnSpot) {
    }

    private record Frame(Location centre, BoundingBox region, float yaw, float width, float height) {
    }
}
