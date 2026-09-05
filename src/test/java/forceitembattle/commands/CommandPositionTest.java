package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.contextWith;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.commands.player.CommandPosition;
import forceitembattle.manager.PositionManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Scheduler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * {@code /pos}: the shared list of structure locations players call out to each other.
 *
 * <p>The dispatch has no subcommand keyword — whether {@code /pos fortress} <em>saves</em> where you
 * stand or <em>shows</em> where someone else saved depends on whether the name already exists.
 * Backwards, it overwrites a teammate's location and the only sign is changed coordinates.
 *
 * <p>{@code remove} is the one real keyword and the only part carrying a permission node
 * ({@code forceitembattle.position.remove}) rather than a {@link Precondition} — the kind of node
 * that gets dropped from a permissions file unnoticed, so both sides are asserted.
 */
class CommandPositionTest {

    private ServerMock server;
    private WorldMock world;
    private Roster roster;
    private PositionManager positions;
    private CommandPosition command;
    private final Map<String, Location> saved = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.saved.clear();

        Scheduler.init(MockBukkit.createMockPlugin());
        this.roster = new Roster();
        this.positions = mock(PositionManager.class);

        when(this.positions.getAllPositions()).thenReturn(this.saved);
        when(this.positions.positionExist(any()))
                .thenAnswer(invocation -> this.saved.containsKey(invocation.getArgument(0)));

        this.command = new CommandPosition(this.roster, this.positions);
        inARoundThatIs(GameState.MID_GAME, GameSetting.POSITIONS);
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private void inARoundThatIs(GameState state, GameSetting... settings) {
        ((CustomCommand) this.command).setContext(contextWith(state, this.roster, settings));
    }

    /** Joins someone and puts them on the roster, which is what {@code /pos} requires. */
    private PlayerMock joinPlaying(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.teleport(new Location(this.world, 120, 65, -30));
        this.roster.add(player.getUniqueId(), new ForceItemPlayer(player, Material.DIRT, 0, 0));
        return player;
    }

    private void alreadySaved(String name) {
        this.saved.put(name, new Location(this.world, -400, 40, 900));
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "pos", args);
    }

    /** Runs the command, then lets the async body it scheduled finish. */
    private void runAndSettle(PlayerMock player, String... args) {
        run(player, args);
        this.server.getScheduler().waitAsyncTasksFinished();
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class Saving {

        @Test
        void anUnknownNameSavesWhereYouAreStanding() {
            PlayerMock player = joinPlaying("Understudy1");

            run(player, "fortress");

            verify(positions).createPosition(eq("fortress"), any(Location.class));
        }

        /** Names are not one word: the arguments are rejoined. */
        @Test
        void aMultiWordNameIsKeptWhole() {
            PlayerMock player = joinPlaying("Understudy1");

            run(player, "nether", "fortress");

            verify(positions).createPosition(eq("nether fortress"), any(Location.class));
        }

        /** The point of the command: everyone is told, not just the person who saved it. */
        @Test
        void thewholeServerIsToldWhoSavedWhatAndWhere() {
            PlayerMock player = joinPlaying("Understudy1");
            PlayerMock bystander = joinPlaying("Understudy2");

            run(player, "fortress");

            String heard = screenOf(bystander);
            assertTrue(heard.contains("Understudy1"), heard);
            assertTrue(heard.contains("fortress"), heard);
            assertTrue(heard.contains("120"), "the coordinates are in the announcement:\n" + heard);
        }

        /**
         * The dispatch decision. An existing name shows rather than saves, so a teammate's saved
         * coordinates are never quietly replaced by yours.
         */
        @Test
        void anExistingNameIsNotOverwritten() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");

            run(player, "fortress");

            verify(positions, never()).createPosition(any(), any());
        }
    }

    @Nested
    class Removing {

        @Test
        void anExistingPositionIsRemoved() {
            PlayerMock player = joinPlaying("Understudy1");
            player.addAttachment(MockBukkit.createMockPlugin(),
                    "forceitembattle.position.remove", true);
            alreadySaved("fortress");

            run(player, "remove", "fortress");

            verify(positions).removePosition("fortress");
            assertSaid(player, "has been removed");
        }

        @Test
        void allClearsTheWholeList() {
            PlayerMock player = joinPlaying("Understudy1");
            player.addAttachment(MockBukkit.createMockPlugin(),
                    "forceitembattle.position.remove", true);
            alreadySaved("fortress");

            run(player, "remove", "all");

            verify(positions).clearPositions();
        }

        @Test
        void aNameThatWasNeverSavedIsRefused() {
            PlayerMock player = joinPlaying("Understudy1");
            player.addAttachment(MockBukkit.createMockPlugin(),
                    "forceitembattle.position.remove", true);

            run(player, "remove", "fortress");

            assertSaid(player, "does not exist");
            verify(positions, never()).removePosition(any());
        }

        /**
         * The hand-checked node. It is the only one in the plugin, so nothing else would notice if
         * it stopped being enforced.
         */
        @Test
        void withoutTheNodeNothingIsRemoved() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");

            run(player, "remove", "fortress");

            assertSaid(player, "do not have permission");
            verify(positions, never()).removePosition(any());
        }

        @Test
        void andNotEvenAllGetsThrough() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");

            run(player, "remove", "all");

            assertSaid(player, "do not have permission");
            verify(positions, never()).clearPositions();
        }
    }

    @Nested
    class WhoMayUseIt {

        /** Deliberately silent rather than a refusal, which is how it behaved before the sweep. */
        @Test
        void aSpectatorIsIgnoredWithoutAWord() {
            PlayerMock player = joinPlaying("Understudy1");
            roster.get(player.getUniqueId()).setSpectator(true);

            run(player, "fortress");

            assertTrue(screenOf(player).isEmpty(), "/pos from a spectator is a no-op, not a refusal");
            verifyNoInteractions(positions);
        }

        @Test
        void soIsSomeoneWhoJoinedMidRound() {
            PlayerMock latecomer = server.addPlayer("Latecomer");

            run(latecomer, "fortress");

            assertTrue(screenOf(latecomer).isEmpty());
            verifyNoInteractions(positions);
        }

        @Test
        void withPositionsTurnedOffItIsRefused() {
            PlayerMock player = joinPlaying("Understudy1");
            inARoundThatIs(GameState.MID_GAME);

            run(player, "fortress");

            assertSaid(player, "Positions are disabled");
            verifyNoInteractions(positions);
        }

        /** On an event round it is ops only, like {@code /pause} and {@code /resume}. */
        @Test
        void onAnEventRoundANonOpIsRefused() {
            PlayerMock player = joinPlaying("Understudy1");
            inARoundThatIs(GameState.MID_GAME, GameSetting.POSITIONS, GameSetting.EVENT);

            run(player, "fortress");

            assertSaid(player, "permission");
            verifyNoInteractions(positions);
        }

        @Test
        void onAnEventRoundAnOpStillMay() {
            PlayerMock admin = joinPlaying("Admin");
            admin.setOp(true);
            inARoundThatIs(GameState.MID_GAME, GameSetting.POSITIONS, GameSetting.EVENT);

            run(admin, "fortress");

            verify(positions).createPosition(eq("fortress"), any(Location.class));
        }
    }

    /** {@code /pos} and {@code /pos list} — the whole shared list, distances included. */
    @Nested
    class Listing {

        @Test
        void withNoArgumentTheWholeListIsSent() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");
            alreadySaved("ancient city");

            runAndSettle(player);

            String said = screenOf(player);
            assertTrue(said.contains("All saved locations"), said);
            assertTrue(said.contains("fortress"), said);
            assertTrue(said.contains("ancient city"), said);
        }

        /** {@code list} is the same path by keyword, and must not be read as a name to save. */
        @Test
        void theListKeywordSavesNothing() {
            PlayerMock player = joinPlaying("Understudy1");

            runAndSettle(player, "list");

            verify(positions, never()).createPosition(any(), any());
        }

        @Test
        void anEmptyListSaysSoRatherThanSendingAHeader() {
            PlayerMock player = joinPlaying("Understudy1");

            runAndSettle(player);

            assertSaid(player, "Nobody added any locations yet.");
        }
    }

    /**
     * {@code /pos <existing name>} — the branch that shows rather than saves. The dispatch decision
     * is covered in {@link Saving}; what is pinned here is that the async body actually runs and
     * that it draws the line, which is the half that was unreachable.
     */
    @Nested
    class Showing {

        @Test
        void anExistingNameReportsWhereItIs() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");
            when(positions.getPosition("fortress")).thenReturn(new Location(world, -400, 40, 900));

            runAndSettle(player, "fortress");

            assertSaid(player, "fortress");
        }

        @Test
        void andDrawsTheLineToIt() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");
            Location fortress = new Location(world, -400, 40, 900);
            when(positions.getPosition("fortress")).thenReturn(fortress);

            runAndSettle(player, "fortress");

            verify(positions).playParticleLine(eq(player), eq(fortress), any());
        }

        @Test
        void andSavesNothingOverTheTopOfIt() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");
            when(positions.getPosition("fortress")).thenReturn(new Location(world, -400, 40, 900));

            runAndSettle(player, "fortress");

            verify(positions, never()).createPosition(any(), any());
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void theSavedNamesAreOffered() {
            PlayerMock player = joinPlaying("Understudy1");
            alreadySaved("fortress");
            alreadySaved("ancient city");

            List<String> offered = command.onTabComplete(player, "pos", new String[]{""});

            assertTrue(offered.contains("fortress"), offered.toString());
            assertTrue(offered.contains("ancient city"), offered.toString());
        }
    }
}
