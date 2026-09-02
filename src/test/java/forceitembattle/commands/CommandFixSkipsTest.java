package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.commands.player.CommandFixSkips;
import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /fixskips}: reissuing a player's remaining jokers without duplicating them.
 *
 * <p>Worth covering because it is not only a command: {@code onRespawn} runs
 * {@code "/fixskips -silent"}, so a failure here is a failure on respawn. The comment in the source
 * records exactly that — the branch this replaced asked the TEAM setting and then dereferenced
 * {@code currentTeam()}, so a player with no team in a round configured for teams crashed when they
 * died.
 *
 * <p>{@code squad()} is what fixed it: the team in a team game, just this player otherwise, without
 * asking a setting.
 */
class CommandFixSkipsTest {

    private ServerMock server;
    private Roster roster;
    private Inventory backpack;
    private CommandFixSkips command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        BackpackManager backpacks = mock(BackpackManager.class);
        this.roster = new Roster();
        this.backpack = Bukkit.createInventory(null, 9 * 3);

        when(backpacks.getBackpackForPlayer(org.mockito.ArgumentMatchers.any())).thenReturn(this.backpack);

        RoundPhase phase = new RoundPhase();
        phase.moveTo(GameState.MID_GAME);

        this.command = new CommandFixSkips(this.roster, backpacks);
        ((CustomCommand) this.command).setContext(new CommandContext(phase, null, this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer onRoster(String name, int jokers) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, jokers, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    private static PlayerMock mockOf(ForceItemPlayer entry) {
        return (PlayerMock) entry.player();
    }

    private static int jokersHeldBy(ForceItemPlayer entry) {
        int held = 0;
        for (ItemStack stack : mockOf(entry).getInventory().getContents()) {
            if (stack != null && stack.getType() == Gamemanager.getJokerMaterial()) {
                held += stack.getAmount();
            }
        }
        return held;
    }

    @Nested
    class Solo {

        @Test
        void theRemainingJokersAreHandedBack() {
            ForceItemPlayer entry = onRoster("Understudy1", 3);

            command.onCommand(mockOf(entry), null, "fixskips", new String[0]);

            assertEquals(3, jokersHeldBy(entry));
        }

        /** The duplicate-removal half: whatever was held before is cleared first. */
        @Test
        void staleJokersAreClearedBeforeTheReissue() {
            ForceItemPlayer entry = onRoster("Understudy1", 2);
            mockOf(entry).getInventory().addItem(Gamemanager.getJokers(7));

            command.onCommand(mockOf(entry), null, "fixskips", new String[0]);

            assertEquals(2, jokersHeldBy(entry), "the pool decides the count, not what was held");
        }

        @Test
        void withNoJokersLeftNothingIsHandedBack() {
            ForceItemPlayer entry = onRoster("Understudy1", 0);

            command.onCommand(mockOf(entry), null, "fixskips", new String[0]);

            assertEquals(0, jokersHeldBy(entry));
            assertTrue(mockOf(entry).nextMessage().contains("don't have any jokers left"));
        }
    }

    @Nested
    class Silently {

        /** {@code onRespawn} runs this form, so it must say nothing on the paths that refuse. */
        @Test
        void theSilentFlagSuppressesTheRefusals() {
            ForceItemPlayer entry = onRoster("Understudy1", 0);

            command.onCommand(mockOf(entry), null, "fixskips", new String[]{"-silent"});

            assertNull(mockOf(entry).nextMessage(), "-silent must not talk");
        }

        @Test
        void theSilentFlagSuppressesTheSuccessLine() {
            ForceItemPlayer entry = onRoster("Understudy1", 2);

            command.onCommand(mockOf(entry), null, "fixskips", new String[]{"-silent"});

            assertEquals(2, jokersHeldBy(entry));
            assertNull(mockOf(entry).nextMessage());
        }

        @Test
        void someoneNotInTheRoundIsRefusedSilently() {
            PlayerMock stranger = server.addPlayer("Latecomer");

            command.onCommand(stranger, null, "fixskips", new String[]{"-silent"});

            assertNull(stranger.nextMessage());
        }
    }

    @Nested
    class InATeam {

        /**
         * A player with no team in a round configured for teams. Branching on the setting rather
         * than on {@code isInTeam()} dereferences a null team here — on respawn.
         */
        @Test
        void aPlayerWithoutATeamDoesNotThrow() {
            ForceItemPlayer entry = onRoster("Understudy1", 2);

            assertDoesNotThrow(() ->
                    command.onCommand(mockOf(entry), null, "fixskips", new String[]{"-silent"}));
            assertEquals(2, jokersHeldBy(entry));
        }

        /** The pool is shared, so every member's stale jokers are cleared, not just the caller's. */
        @Test
        void everyMembersStaleJokersAreCleared() {
            ForceItemPlayer first = onRoster("Understudy1", 4);
            ForceItemPlayer second = onRoster("Understudy2", 4);

            Team team = new Team(1, null, 0, 0, first);
            team.addPlayer(second);
            first.setCurrentTeam(team);
            second.setCurrentTeam(team);
            team.setJokers(4);

            mockOf(second).getInventory().addItem(Gamemanager.getJokers(9));

            command.onCommand(mockOf(first), null, "fixskips", new String[0]);

            assertEquals(0, jokersHeldBy(second), "the teammate's stale stack is cleared");
            assertEquals(4, jokersHeldBy(first), "the caller gets the shared pool");
        }
    }
}
