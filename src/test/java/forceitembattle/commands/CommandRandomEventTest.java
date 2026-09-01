package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.contextWith;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.admin.CommandRandomEvent;
import forceitembattle.manager.RandomEventManager;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.randomevents.RandomEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /randomevent}: firing one of the timed events on demand.
 *
 * <p>Small, and worth covering for one reason: it is the only way to put a random event into a
 * known state, so the round-test harness and every manual check of an event start here. What it
 * has to get right is that an unknown name is refused before {@code trigger} is reached — and that
 * a refusal from the manager, which owns the "one event at a time" rule, reaches the player rather
 * than being swallowed into a silent no-op.
 */
class CommandRandomEventTest {

    private ServerMock server;
    private RandomEventManager events;
    private CommandRandomEvent command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.events = mock(RandomEventManager.class);
        when(plugin.getRandomEventManager()).thenReturn(this.events);
        when(this.events.trigger(any())).thenReturn(true);

        this.command = new CommandRandomEvent(plugin);
        ((CustomCommand) this.command).setContext(
                contextWith(GameState.MID_GAME, new Roster()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock joinOp(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.setOp(true);
        return player;
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "randomevent", args);
    }

    @Test
    void anEventIsTriggeredByItsId() {
        PlayerMock admin = joinOp("Admin");

        run(admin, RandomEvents.POINT_HUNT.id());

        verify(events).trigger(RandomEvents.POINT_HUNT);
    }

    @Test
    void theIdIsCaseInsensitive() {
        PlayerMock admin = joinOp("Admin");

        run(admin, RandomEvents.POINT_HUNT.id().toUpperCase());

        verify(events).trigger(RandomEvents.POINT_HUNT);
    }

    /** With no argument, the form and the list of what can be typed. */
    @Test
    void noArgumentsListsWhatCanBeTriggered() {
        PlayerMock admin = joinOp("Admin");

        run(admin);

        String said = screenOf(admin);
        for (String id : RandomEvents.ids()) {
            assertTrue(said.contains(id), "expected " + id + " to be offered:\n" + said);
        }
        verifyNoInteractions(events);
    }

    @Test
    void moreThanOneArgumentIsAlsoTheForm() {
        PlayerMock admin = joinOp("Admin");

        run(admin, RandomEvents.POINT_HUNT.id(), "now");

        assertSaid(admin, "randomevent");
        verifyNoInteractions(events);
    }

    @Test
    void anUnknownEventIsRefusedBeforeTheManagerIsAsked() {
        PlayerMock admin = joinOp("Admin");

        run(admin, "not_an_event");

        assertSaid(admin, "is not an event");
        verify(events, never()).trigger(any());
    }

    /** The manager owns "one at a time", and its refusal has to reach the player. */
    @Test
    void aRefusalFromTheManagerIsReported() {
        PlayerMock admin = joinOp("Admin");
        when(events.trigger(any())).thenReturn(false);

        run(admin, RandomEvents.POINT_HUNT.id());

        assertSaid(admin, "already running");
    }

    @Test
    void aSuccessfulTriggerSaysNothing() {
        PlayerMock admin = joinOp("Admin");

        run(admin, RandomEvents.POINT_HUNT.id());

        assertTrue(screenOf(admin).isEmpty(), "the event announces itself; the command does not");
    }

    @Test
    void aNonOpIsRefused() {
        PlayerMock player = server.addPlayer("Understudy1");

        run(player, RandomEvents.POINT_HUNT.id());

        assertSaid(player, "permission");
        verifyNoInteractions(events);
    }

    @Test
    void thereIsNoEventToFireOutsideARound() {
        PlayerMock admin = joinOp("Admin");
        ((CustomCommand) command).setContext(contextWith(GameState.PRE_GAME, new Roster()));

        run(admin, RandomEvents.POINT_HUNT.id());

        assertSaid(admin, "not running");
        verifyNoInteractions(events);
    }

    @Test
    void everyEventIsOfferedForCompletion() {
        PlayerMock admin = joinOp("Admin");

        assertEquals(RandomEvents.ids(), command.onTabComplete(admin, "randomevent", new String[]{""}));
    }
}
