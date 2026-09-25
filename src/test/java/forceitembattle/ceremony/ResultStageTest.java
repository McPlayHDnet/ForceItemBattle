package forceitembattle.ceremony;

import static forceitembattle.ceremony.StageTimelineTest.backToBack;
import static forceitembattle.ceremony.StageTimelineTest.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.ResultCeremony.Reveal;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.joml.Quaternionf;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDismountEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * {@link ResultStage}, driven through MockBukkit's scheduler. What the client draws — interpolation,
 * glow, billboarding — is not observable here; where each entity is, what it carries and when the
 * callbacks fire is.
 */
class ResultStageTest {

    /** Longer than any reveal below takes. */
    private static final int WHOLE_REVEAL = 600;

    private ServerMock server;
    private WorldMock world;
    private ResultStage stage;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = new StageWorldMock(this.server, "world");
        this.server.addWorld(this.world);
        Scheduler.init(MockBukkit.createMockPlugin());
        this.stage = new ResultStage(mock(GameSettings.class), StageWorldMock.SEATS);
    }

    @AfterEach
    void tearDown() {
        this.stage.close();
        Scheduler.reset();
        MockBukkit.unmock();
    }

    private void tick(int ticks) {
        this.server.getScheduler().performTicks(ticks);
    }

    private ScoreOwner owner(PlayerMock player, ForceItem... items) {
        ForceItemPlayer participant = new ForceItemPlayer(player, Material.STONE, 0, 0);
        for (ForceItem item : items) {
            participant.scoreOwner().record(item);
        }
        return participant.scoreOwner();
    }

    private List<ItemDisplay> itemsOnStage() {
        return this.world.getEntitiesByClass(ItemDisplay.class).stream()
                .filter(display -> !this.stage.isSeat(display))
                .toList();
    }

    private List<String> texts() {
        List<String> texts = new ArrayList<>();
        for (TextDisplay display : this.world.getEntitiesByClass(TextDisplay.class)) {
            texts.add(PlainTextComponentSerializer.plainText().serialize(display.text()));
        }
        return texts;
    }

    private void open() {
        this.stage.open(this.world.getSpawnLocation());
    }

    @Nested
    class Opening {

        @Test
        void everyoneOnlineIsSeatedFacingTheCanvas() {
            PlayerMock first = server.addPlayer("Understudy1");
            PlayerMock second = server.addPlayer("Understudy2");

            open();

            for (PlayerMock player : List.of(first, second)) {
                assertTrue(player.isInsideVehicle(), player.getName() + " is seated");
                assertTrue(stage.isSeat(player.getVehicle()));
                assertTrue(Math.abs(Math.abs(player.getLocation().getYaw()) - 180) < 30, "facing north");
            }
            assertTrue(first.getVehicle() != second.getVehicle(), "one seat each");
        }

        /** A seat each, floating, with nothing built under the audience. */
        @Test
        void theAudienceFloats() {
            PlayerMock player = server.addPlayer("Understudy1");

            open();

            double seatZ = player.getVehicle().getLocation().getZ();
            long reaching = world.getEntitiesByClass(BlockDisplay.class).stream()
                    .filter(display -> display.getLocation().getZ() + display.getTransformation().getScale().z() > seatZ - 1)
                    .count();
            assertEquals(0, reaching, "nothing built reaches the seats");
        }

        @Test
        void theStageFloatsAboveTheGround() {
            PlayerMock player = server.addPlayer("Understudy1");

            open();

            int ground = world.getHighestBlockYAt(player.getLocation());
            assertTrue(player.getVehicle().getLocation().getY() > ground);
        }

        /** Items float in open sky: nothing is built behind them, only the waiting title. */
        @Test
        void nothingIsBuiltUntilTheFirstItemLands() {
            open();

            assertTrue(stage.isOpen());
            assertTrue(texts().contains("RESULTS"));
            assertTrue(world.getEntitiesByClass(BlockDisplay.class).isEmpty());
        }

        @Test
        void aPlayerWhoRejoinsGetsTheirOldSeatBack() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            Entity seat = player.getVehicle();

            player.leaveVehicle();
            stage.seat(player);

            assertEquals(seat, player.getVehicle());
        }

        @Test
        void seatingSomeoneAlreadySeatedLeavesThemThere() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            Entity seat = player.getVehicle();

            stage.seat(player);

            assertEquals(seat, player.getVehicle());
        }

        @Test
        void seatingDoesNothingWithoutAStage() {
            PlayerMock player = server.addPlayer("Understudy1");

            stage.seat(player);

            assertFalse(player.isInsideVehicle());
        }
    }

    @Nested
    class Revealing {

        @Test
        void everyItemEndsUpOnTheCanvas() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain(), plain()), 2, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            assertEquals(3, itemsOnStage().size());
        }

        /** The chat line's [Inventory] link must never open before the archive it points at exists. */
        @Test
        void theArchiveIsWrittenBeforeTheNameGoesOut() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            List<String> order = new ArrayList<>();

            stage.reveal(new Reveal(owner(player, plain(), plain()), 1, true),
                    () -> order.add("dealt"), () -> order.add("complete"));
            tick(WHOLE_REVEAL);

            assertEquals(List.of("dealt", "complete"), order);
        }

        @Test
        void theNameIsHiddenUntilTheEnd() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain()), 3, false), () -> { }, () -> { });
            tick(30);
            assertTrue(stage.isRevealing());
            assertFalse(texts().stream().anyMatch(text -> text.contains("Understudy1")));

            tick(WHOLE_REVEAL);
            assertFalse(stage.isRevealing());
            assertTrue(texts().contains("3. Understudy1"));
            assertTrue(texts().contains("2 Items found"));
        }

        @Test
        void everyoneIsToldWhereToOpenTheInventory() {
            PlayerMock player = server.addPlayer("Understudy1");
            PlayerMock bystander = server.addPlayer("Understudy2");
            open();

            stage.reveal(new Reveal(owner(player, plain()), 2, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            String said = PlainTextComponentSerializer.plainText().serialize(nextComponent(bystander));
            assertTrue(said.contains("Understudy1") && said.contains("[Inventory]"), said);
        }

        @Test
        void aChainGlowsInItsRarity() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), backToBack(Rarity.LEGENDARY)), 1, false),
                    () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            List<ItemDisplay> glowing = itemsOnStage().stream().filter(Entity::isGlowing).toList();
            assertEquals(1, glowing.size());
            assertEquals(StagePalette.glowOf(Rarity.LEGENDARY), glowing.getFirst().getGlowColorOverride());
        }

        /**
         * The renderer turns item displays half a turn by itself, so every item carries a half turn
         * back or the viewer sees its reverse. Checked after the flight, where a transformation
         * built without it would have lost it.
         */
        @Test
        void everyItemFacesTheViewer() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain()), 1, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            Quaternionf halfTurn = new Quaternionf().rotateY((float) Math.PI);
            for (ItemDisplay item : itemsOnStage()) {
                Quaternionf rotation = item.getTransformation().getLeftRotation();
                assertTrue(Math.abs(rotation.dot(halfTurn)) > 0.999, "rotation " + rotation);
            }
        }

        /** Every settled item faces the same way, flat like an inventory slot, not billboarded. */
        @Test
        void everyItemLiesFlatOnTheCanvas() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain(), plain(), plain()), 1, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            for (ItemDisplay item : itemsOnStage()) {
                Location at = item.getLocation();
                assertEquals(Display.Billboard.FIXED, item.getBillboard());
                assertEquals(0, at.getYaw(), 1e-3);
                assertEquals(0, at.getPitch(), 1e-3);
            }
        }

        /** No backing of any kind: after a full reveal, the only blocks built are none at all. */
        @Test
        void theItemsHangInOpenSky() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain(), plain()), 1, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            assertEquals(3, itemsOnStage().size());
            assertTrue(world.getEntitiesByClass(BlockDisplay.class).isEmpty());
        }

        @Test
        void theNextRevealClearsThePreviousItems() {
            PlayerMock player = server.addPlayer("Understudy1");
            PlayerMock other = server.addPlayer("Understudy2");
            open();

            stage.reveal(new Reveal(owner(player, plain(), plain(), plain()), 2, false), () -> { }, () -> { });
            tick(WHOLE_REVEAL);
            stage.reveal(new Reveal(owner(other, plain()), 1, true), () -> { }, () -> { });
            tick(WHOLE_REVEAL);

            assertEquals(1, itemsOnStage().size());
        }

        @Test
        void anOwnerWithNothingStillFinishes() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            List<String> order = new ArrayList<>();

            stage.reveal(new Reveal(owner(player), 1, true), () -> order.add("dealt"), () -> order.add("complete"));
            tick(WHOLE_REVEAL);

            assertEquals(List.of("dealt", "complete"), order);
            assertTrue(texts().contains("0 Items found"));
        }
    }

    @Nested
    class TheFinale {

        @Test
        void theTopThreeStandOnThePodiumAndTheSeatsLetGo() {
            PlayerMock first = server.addPlayer("Understudy1");
            PlayerMock second = server.addPlayer("Understudy2");
            PlayerMock third = server.addPlayer("Understudy3");
            PlayerMock fourth = server.addPlayer("Understudy4");
            open();

            stage.finale(List.of(
                    new Reveal(owner(first, plain(), plain(), plain()), 1, true),
                    new Reveal(owner(second, plain(), plain()), 2, false),
                    new Reveal(owner(third, plain()), 3, false)));
            tick(WHOLE_REVEAL);

            List<Mannequin> standing = world.getEntitiesByClass(Mannequin.class).stream().toList();
            assertEquals(3, standing.size());
            Mannequin winner = standing.stream()
                    .max((a, b) -> Double.compare(a.getLocation().getY(), b.getLocation().getY()))
                    .orElseThrow();
            assertEquals(first.getUniqueId(), winner.getProfile().uuid());

            assertTrue(texts().contains("1. 3 Items\nUnderstudy1"));
            for (Player player : List.of(first, second, third, fourth)) {
                assertFalse(player.isInsideVehicle(), player.getName() + " is free to move");
            }
        }

        /** A comma-joined team line ran wide enough to cover the neighbouring steps' labels. */
        @Test
        void anUnnamedTeamIsListedAMemberPerLine() {
            PlayerMock alice = server.addPlayer("Alice");
            PlayerMock bob = server.addPlayer("Bob");
            open();

            Team team = new Team(1, Material.STONE, 0, 0,
                    new ForceItemPlayer(alice, Material.STONE, 0, 0),
                    new ForceItemPlayer(bob, Material.STONE, 0, 0));
            team.record(plain());
            stage.finale(List.of(new Reveal(team, 1, true)));
            tick(WHOLE_REVEAL);

            assertTrue(texts().contains("1. 1 Items\nAlice\nBob"), texts().toString());
        }
    }

    @Nested
    class TheFinaleStaysStill {

        /** The winner's items used to spin once the result was out; they stay facing the viewer. */
        @Test
        void theItemsDoNotTurnOnceTheWinnerIsOut() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            ScoreOwner winner = owner(player, plain(), plain(), plain());

            stage.reveal(new Reveal(winner, 1, true), () -> { }, () -> { });
            tick(WHOLE_REVEAL);
            stage.finale(List.of(new Reveal(winner, 1, true)));
            tick(WHOLE_REVEAL);

            Quaternionf halfTurn = new Quaternionf().rotateY((float) Math.PI);
            assertEquals(3, itemsOnStage().size());
            for (ItemDisplay item : itemsOnStage()) {
                assertTrue(Math.abs(item.getTransformation().getLeftRotation().dot(halfTurn)) > 0.999);
            }
        }

        /** The podium floats like the seats: in front of the canvas face, only the steps are built. */
        @Test
        void thePodiumFloats() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            double canvasFace = player.getVehicle().getLocation().getZ() - StageLayout.SEAT_DISTANCE;

            stage.finale(List.of(new Reveal(owner(player, plain()), 1, true)));
            tick(WHOLE_REVEAL);

            Set<Material> steps = Set.of(Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK);
            List<BlockDisplay> inFront = world.getEntitiesByClass(BlockDisplay.class).stream()
                    .filter(display -> display.getLocation().getZ() + display.getTransformation().getScale().z() > canvasFace)
                    .toList();
            assertFalse(inFront.isEmpty(), "the steps are there");
            assertTrue(inFront.stream().allMatch(display -> steps.contains(display.getBlock().getMaterial())));
        }
    }

    @Nested
    class Closing {

        @Test
        void closingLeavesNothingBehind() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();
            stage.reveal(new Reveal(owner(player, plain(), plain()), 1, true), () -> { }, () -> { });
            tick(WHOLE_REVEAL);
            stage.finale(List.of(new Reveal(owner(player, plain(), plain()), 1, true)));
            tick(40);

            stage.close();
            tick(WHOLE_REVEAL);

            assertFalse(stage.isOpen());
            assertEquals(List.of(player), List.copyOf(world.getEntities()));
            assertFalse(player.isInsideVehicle());
        }
    }

    @Nested
    class TheSeatListener {

        @Test
        void sneakingDoesNotGetYouOffYourSeat() {
            PlayerMock player = server.addPlayer("Understudy1");
            open();

            EntityDismountEvent event = new EntityDismountEvent(player, player.getVehicle());
            new ResultStageListener(stage).onDismount(event);

            assertTrue(event.isCancelled());
        }

        @Test
        void anythingElseMayBeLeft() {
            PlayerMock player = server.addPlayer("Understudy1");
            Entity cart = world.spawn(player.getLocation(), ItemDisplay.class);

            EntityDismountEvent event = new EntityDismountEvent(player, cart);
            new ResultStageListener(stage).onDismount(event);

            assertFalse(event.isCancelled());
            assertNotNull(cart);
        }
    }

    private static Component nextComponent(PlayerMock player) {
        Component component = player.nextComponentMessage();
        assertNotNull(component, player.getName() + " was told nothing");
        return component;
    }
}
