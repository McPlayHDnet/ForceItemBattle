package forceitembattle.ceremony;

import com.destroystokyo.paper.profile.PlayerProfile;
import forceitembattle.ceremony.StageLayout.Facing;
import forceitembattle.ceremony.StageLayout.Grid;
import forceitembattle.ceremony.StageLayout.Point;
import forceitembattle.gui.ResultDisplay;
import forceitembattle.manager.Manager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.ScoreOwner;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The end-of-round reveal, built in the sky near spawn: each owner's items pop up in a spotlight and
 * then settle into a grid floating in open air, an audience seated facing it, and a podium once the winner is out.
 *
 * <p>Every entity is non-persistent, so a crash or restart leaves nothing behind to sweep up.
 */
public final class ResultStage implements Manager {

    private static final int POP_TICKS = 3;
    private static final int SETTLE_TICKS = 3;
    private static final int CLEAR_TICKS = 5;
    private static final long FIRST_ITEM_DELAY = CLEAR_TICKS + 15;
    private static final long DEALT_TO_NAME_TICKS = 60;
    private static final long STEP_RISE_TICKS = 20;
    private static final long RELEASE_TICKS = 100;

    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

    private final GameSettings settings;
    private final SeatSpawner seatSpawner;
    private final Cues cues = new Cues();

    private final List<Entity> fixtures = new ArrayList<>();
    private final List<ItemDisplay> grid = new ArrayList<>();
    private final Map<UUID, Entity> seats = new HashMap<>();

    @Nullable
    private Location anchor;
    @Nullable
    private TextDisplay title;
    @Nullable
    private TextDisplay counter;
    @Nullable
    private TextDisplay card;

    private int nextSeat;
    private boolean revealing;

    public ResultStage(GameSettings settings, SeatSpawner seatSpawner) {
        this.settings = settings;
        this.seatSpawner = seatSpawner;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
        this.close();
    }

    public boolean isOpen() {
        return this.anchor != null;
    }

    public boolean isRevealing() {
        return this.revealing;
    }

    /** Builds the stage near {@code spawn} and seats everyone online in front of it. */
    public void open(Location spawn) {
        this.close();

        World world = spawn.getWorld();
        double anchorX = spawn.getBlockX() + 0.5;
        double anchorZ = spawn.getBlockZ() + 0.5 - StageLayout.SEAT_DISTANCE;
        int anchorY = StageLayout.anchorY(
                (x, z) -> world.getHighestBlockYAt((int) Math.floor(anchorX + x), (int) Math.floor(anchorZ + z)),
                world.getMaxHeight());
        this.anchor = new Location(world, anchorX, anchorY, anchorZ);


        this.title = this.text(StageLayout.TITLE, 3.0f, Display.Billboard.VERTICAL, StagePalette.NONE);
        this.counter = this.text(StageLayout.COUNTER, 1.6f, Display.Billboard.VERTICAL, StagePalette.NONE);
        this.card = this.text(StageLayout.CARD, StageLayout.CARD_SCALE, Display.Billboard.CENTER, StagePalette.NONE);
        this.write(this.title, "<gold><b>RESULTS");
        this.write(this.counter, "<gray>The first reveal is on its way");

        Bukkit.getOnlinePlayers().forEach(this::seat);
    }

    /** Seats a player, reusing their seat if they already had one. Does nothing while no stage stands. */
    public void seat(Player player) {
        if (this.anchor == null || !player.isOnline()) {
            return;
        }

        Entity seat = this.seats.get(player.getUniqueId());
        if (seat == null || !seat.isValid()) {
            seat = this.seatSpawner.spawn(this.locationOf(StageLayout.seat(this.nextSeat++)));
            this.seats.put(player.getUniqueId(), seat);
        }
        // The dismount lock would refuse the leaveVehicle below.
        if (seat.equals(player.getVehicle())) {
            return;
        }

        Point at = this.pointOf(seat.getLocation());
        Facing facing = StageLayout.lookAt(at.plus(0, StageLayout.EYE_HEIGHT, 0), StageLayout.SPOTLIGHT);
        Location view = seat.getLocation().clone();
        view.setYaw(facing.yaw());
        view.setPitch(facing.pitch());

        player.leaveVehicle();
        player.teleport(view);
        seat.addPassenger(player);
    }

    public boolean isSeat(Entity vehicle) {
        return vehicle != null && this.seats.containsValue(vehicle);
    }

    /**
     * Deals one owner's items into the grid.
     *
     * @param onDealt    runs the moment the last item lands, before the name goes out, so the
     *                   {@code [Inventory]} link in the announcement never opens an empty archive
     * @param onComplete runs once the name, title and chat line are out
     */
    public void reveal(ResultCeremony.Reveal reveal, Runnable onDealt, Runnable onComplete) {
        this.revealing = true;
        this.clearGrid();

        ScoreOwner owner = reveal.owner();
        List<ForceItem> items = List.copyOf(owner.foundItems());
        boolean event = this.settings.isSettingEnabled(GameSetting.EVENT);
        Grid layout = StageLayout.gridFor(items.size());

        this.write(this.title, "<gray><b>????????");
        this.write(this.counter, "<gold>0 <gray>Items");

        long at = FIRST_ITEM_DELAY;
        long dealtAt = at;
        for (int index = 0; index < items.size(); index++) {
            ForceItem item = items.get(index);
            int position = index;
            this.cues.at(at, () -> this.present(owner, item, position, items.size(), layout, event));
            dealtAt = at + StageTimeline.holdFor(item, event) + StageTimeline.FLIGHT_TICKS;
            at += StageTimeline.gapAfter(item, event);
        }
        if (items.isEmpty()) {
            this.cues.at(at, () -> this.showCard("<gray>No items found", null));
            dealtAt = at + 20;
        }

        this.cues.at(dealtAt, () -> {
            this.showCard("", null);
            onDealt.run();
        });
        this.cues.at(dealtAt + DEALT_TO_NAME_TICKS, () -> {
            this.announce(reveal, items.size());
            this.revealing = false;
            onComplete.run();
        });
    }

    /** The winner's moment: the podium rises, the top three take their steps, and the seats let go. */
    public void finale(List<ResultCeremony.Reveal> podium) {
        if (this.anchor == null) {
            return;
        }

        Map<Integer, List<ScoreOwner>> byPlace = new TreeMap<>();
        podium.forEach(reveal -> byPlace.computeIfAbsent(reveal.place(), place -> new ArrayList<>())
                .add(reveal.owner()));

        this.playToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        byPlace.forEach((place, owners) -> {
            if (place > 3) {
                return;
            }
            this.raiseStep(place);
            // Third place first, so the winner is the last to appear.
            this.cues.at(STEP_RISE_TICKS + (4 - place) * 15L, () -> this.standOnStep(place, owners));
        });

        for (long delay = STEP_RISE_TICKS + 45; delay <= STEP_RISE_TICKS + 105; delay += 20) {
            this.cues.at(delay, this::launchFireworks);
        }

        this.cues.at(RELEASE_TICKS + STEP_RISE_TICKS + 45, this::releaseSeats);
    }

    /** Takes the whole stage down. Safe to call when none is standing. */
    public void close() {
        this.cues.clear();

        List<Entity> vacated = new ArrayList<>(this.seats.values());
        this.seats.clear();
        vacated.forEach(Entity::remove);

        this.grid.forEach(Entity::remove);
        this.grid.clear();
        this.fixtures.forEach(Entity::remove);
        this.fixtures.clear();

        this.anchor = null;
        this.title = null;
        this.counter = null;
        this.card = null;
        this.nextSeat = 0;
        this.revealing = false;
    }

    private void present(ScoreOwner owner, ForceItem item, int index, int count, Grid layout, boolean event) {
        Color glow = StagePalette.glowOf(item);
        ItemDisplay display = this.spawn(StageLayout.SPOTLIGHT, ItemDisplay.class, spawned -> {
            spawned.setItemStack(CustomMaterials.itemStackOf(item.material()));
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            // Aimed at the audience rather than billboarded: a billboard copies the camera's angle,
            // so every item off the centre of the view was seen from the side.
            spawned.setBillboard(Display.Billboard.FIXED);
            Facing facing = StageLayout.towardsAudience(StageLayout.SPOTLIGHT);
            spawned.setRotation(facing.yaw(), facing.pitch());
            spawned.setBrightness(FULL_BRIGHT);
            spawned.setTeleportDuration(StageTimeline.FLIGHT_TICKS);
            spawned.setTransformation(facingViewer(0f));
            if (glow != null) {
                spawned.setGlowing(true);
                spawned.setGlowColorOverride(glow);
            }
        });
        this.grid.add(display);

        // A transformation set in the spawn tick is the client's starting point, not an animation.
        this.cues.at(1, () -> animate(display, facingViewer(StageLayout.SPOTLIGHT_SCALE * 1.15f), POP_TICKS));
        this.cues.at(1 + POP_TICKS, () -> animate(display, facingViewer(StageLayout.SPOTLIGHT_SCALE), SETTLE_TICKS));
        this.cues.at(StageTimeline.holdFor(item, event), () -> {
            Point slot = layout.slot(index);
            Facing facing = StageLayout.towardsAudience(slot);
            Location target = this.locationOf(slot);
            target.setYaw(facing.yaw());
            target.setPitch(facing.pitch());
            display.teleport(target);
            animate(display, facingViewer(layout.itemScale()), StageTimeline.FLIGHT_TICKS);
        });

        this.showCard(ItemCard.of(owner, item), glow);
        this.write(this.counter, "<gold>" + (index + 1) + " <gray>Items");

        this.playToAll(Sound.ENTITY_ITEM_PICKUP, 0.6f, StageTimeline.pitch(index, count));
        Rarity rarity = StageTimeline.rarityOf(item);
        if (rarity != null) {
            Bukkit.getOnlinePlayers().forEach(rarity::playTo);
            Particle particle = rarity == Rarity.RNGESUS || rarity == Rarity.EXTRAORDINARY
                    ? Particle.TOTEM_OF_UNDYING
                    : Particle.END_ROD;
            this.world().spawnParticle(particle, this.locationOf(StageLayout.SPOTLIGHT), 40, 0.8, 0.8, 0.8, 0.15);
        }
    }

    private void announce(ResultCeremony.Reveal reveal, int itemCount) {
        ScoreOwner owner = reveal.owner();
        String placeColor = Text.placeColor(reveal.place());
        String name = ResultDisplay.nameOf(owner);

        this.write(this.title, placeColor + "<b>" + reveal.place() + ". <white>" + name);
        this.write(this.counter, "<gold>" + itemCount + " Items found");

        Title.Times times = Title.Times.times(Duration.ofMillis(750), Duration.ofMillis(1750), Duration.ofMillis(750));
        Title shown = Title.title(Text.of(placeColor + reveal.place() + "<white>. " + name),
                Text.of("<gold>" + itemCount + " Items found"), times);
        Component chatLine = Text.of(placeColor + reveal.place() + "<white>. " + name
                + " <dark_gray>┃ <gold>" + itemCount + " Items found "
                + "<dark_gray>» <click:run_command:/result " + ResultDisplay.resultArgumentFor(owner)
                + "><dark_gray>[<aqua>Inventory<dark_gray>]");

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showTitle(shown);
            viewer.sendMessage(chatLine);
        }
        this.playToAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void clearGrid() {
        List<ItemDisplay> leaving = new ArrayList<>(this.grid);
        this.grid.clear();
        leaving.forEach(display -> animate(display, facingViewer(0f), CLEAR_TICKS));
        this.cues.at(CLEAR_TICKS + 1, () -> leaving.forEach(Entity::remove));
    }

    private void raiseStep(int place) {
        double x = StageLayout.podiumX(place);
        float height = (float) StageLayout.podiumHeight(place);
        float width = (float) StageLayout.STEP_WIDTH;
        float depth = (float) StageLayout.STEP_DEPTH;

        BlockDisplay step = this.keep(this.spawn(
                new Point(x - width / 2, 0, StageLayout.PODIUM_Z - depth / 2), BlockDisplay.class, display -> {
                    display.setBlock(StagePalette.stepOf(place).createBlockData());
                    display.setBrightness(FULL_BRIGHT);
                    display.setTransformation(box(width, 0.01f, depth));
                }));
        this.cues.at(1, () -> animate(step, box(width, height, depth), (int) STEP_RISE_TICKS));
    }

    private void standOnStep(int place, List<ScoreOwner> owners) {
        double x = StageLayout.podiumX(place);
        double top = StageLayout.podiumHeight(place);

        List<ForceItemPlayer> members = owners.stream().flatMap(owner -> owner.members().stream()).toList();
        for (int index = 0; index < members.size(); index++) {
            Player member = members.get(index).player();
            if (member == null) {
                continue;
            }
            Point spot = new Point(x + StageLayout.memberOffset(index, members.size()), top, StageLayout.PODIUM_Z);
            this.keep(this.spawn(spot, Mannequin.class, mannequin -> {
                mannequin.setProfile(profileOf(member));
                mannequin.setImmovable(true);
                mannequin.setGravity(false);
                mannequin.setInvulnerable(true);
                mannequin.setDescription(Text.of(Text.placeColor(place) + ordinal(place) + " Place"));
            }));
        }

        String names = String.join("<gray>, <white>", owners.stream().map(ResultDisplay::nameOf).toList());
        int items = owners.getFirst().foundItems().size();
        TextDisplay label = this.text(new Point(x, top + 2.3, StageLayout.PODIUM_Z), 1.3f,
                Display.Billboard.VERTICAL, StagePalette.CARD);
        this.write(label, Text.placeColor(place) + "<b>" + place + ".</b> <white>" + names
                + "\n<gold>" + items + " Items");

        Location at = this.locationOf(new Point(x, top + 1, StageLayout.PODIUM_Z));
        this.world().spawnParticle(Particle.CLOUD, at, 30, 0.5, 0.8, 0.5, 0.02);
        this.playToAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, place == 1 ? 1.2f : 0.8f);
    }

    private void launchFireworks() {
        for (int place = 1; place <= 3; place++) {
            int shown = place;
            for (double side : new double[]{-1.6, 1.6}) {
                Point from = new Point(StageLayout.podiumX(place) + side, 0.5, StageLayout.PODIUM_Z + 1);
                this.spawn(from, Firework.class, spawned -> {
                    FireworkMeta meta = spawned.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder()
                            .with(shown == 1 ? FireworkEffect.Type.STAR : FireworkEffect.Type.BALL_LARGE)
                            .withColor(StagePalette.fireworkOf(shown))
                            .withFade(Color.WHITE)
                            .flicker(true)
                            .build());
                    meta.setPower(1);
                    spawned.setFireworkMeta(meta);
                });
            }
        }
    }

    private void releaseSeats() {
        List<Entity> vacated = new ArrayList<>(this.seats.values());
        this.seats.clear();
        vacated.forEach(Entity::remove);
    }

    private TextDisplay text(Point point, float scale, Display.Billboard billboard, Color background) {
        return this.keep(this.spawn(point, TextDisplay.class, display -> {
            display.setBillboard(billboard);
            display.setBrightness(FULL_BRIGHT);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setLineWidth(400);
            display.setBackgroundColor(background);
            display.setTransformation(scaled(scale));
        }));
    }

    /** An empty card drops its backdrop too, or a small dark box would hang in the spotlight. */
    private void showCard(String miniMessage, @Nullable Color glow) {
        if (this.card == null) {
            return;
        }
        this.card.setBackgroundColor(miniMessage.isEmpty() ? StagePalette.NONE : StagePalette.cardFor(glow));
        this.write(this.card, miniMessage);
    }

    private void write(@Nullable TextDisplay display, String miniMessage) {
        if (display != null) {
            display.text(Text.of(miniMessage));
        }
    }

    private <T extends Entity> T spawn(Point point, Class<T> type, Consumer<T> setup) {
        return this.world().spawn(this.locationOf(point), type, spawned -> {
            spawned.setPersistent(false);
            setup.accept(spawned);
        });
    }

    /** Stays until the stage closes. */
    private <T extends Entity> T keep(T entity) {
        this.fixtures.add(entity);
        return entity;
    }

    private void playToAll(Sound sound, float volume, float pitch) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.playSound(viewer.getLocation(), sound, volume, pitch);
        }
    }

    private World world() {
        return this.anchor.getWorld();
    }

    private Location locationOf(Point point) {
        return this.anchor.clone().add(point.x(), point.y(), point.z());
    }

    private Point pointOf(Location location) {
        return new Point(location.getX() - this.anchor.getX(),
                location.getY() - this.anchor.getY(),
                location.getZ() - this.anchor.getZ());
    }

    private static ResolvableProfile profileOf(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        return ResolvableProfile.resolvableProfile(profile);
    }

    private static String ordinal(int place) {
        return switch (place) {
            case 1 -> "1st";
            case 2 -> "2nd";
            default -> "3rd";
        };
    }

    private static void animate(Display display, Transformation target, int ticks) {
        if (!display.isValid()) {
            return;
        }
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(target);
    }

    private static Transformation scaled(float scale) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f());
    }

    /**
     * The item renderer turns every item display half a turn on its own, so under a billboard the
     * viewer saw the back of each item. Every item transformation carries this, or an animation
     * between two of them would visibly turn the item round.
     */
    private static Transformation facingViewer(float scale) {
        return new Transformation(new Vector3f(), new Quaternionf().rotateY((float) Math.PI),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private static Transformation box(float width, float height, float depth) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(width, height, depth), new AxisAngle4f());
    }
}
