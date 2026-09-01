package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandTeams;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.ConfigSource;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.Ruleset;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /teams}: who the subcommands act on.
 *
 * <p>Every branch hands a {@link ForceItemPlayer} to {@code TeamsManager}, which dereferences it
 * without checking — so the command's job is to resolve two of them or refuse.
 *
 * <p>The gates themselves (teams enabled, PRE_GAME) are declared preconditions and covered by
 * {@code CustomCommandTest}; these cover what happens once they hold.
 */
class CommandTeamsTest {

    private ServerMock server;
    private Roster roster;
    private TeamsManager teamManager;
    private CommandTeams command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.roster = new Roster();
        this.teamManager = mock(TeamsManager.class);

        when(plugin.getRoster()).thenReturn(this.roster);
        when(plugin.getTeamManager()).thenReturn(this.teamManager);

        Map<String, Object> settings = new HashMap<>();
        settings.put(GameSetting.TEAM.configPath(), true);
        RoundPhase phase = new RoundPhase();
        phase.moveTo(GameState.PRE_GAME);

        this.command = new CommandTeams(plugin);
        ((CustomCommand) this.command).setContext(
                new CommandContext(phase, new Ruleset(new MapConfig(settings)), this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private record MapConfig(Map<String, Object> values) implements ConfigSource {
        @Override
        public boolean getBoolean(String path) {
            return Boolean.TRUE.equals(this.values.get(path));
        }

        @Override
        public int getInt(String path) {
            return 0;
        }

        @Override
        public void set(String path, Object value) {
            this.values.put(path, value);
        }

        @Override
        public void save() {
        }
    }

    /** Joins someone and puts them on the roster. */
    private PlayerMock onRoster(String name) {
        PlayerMock player = this.server.addPlayer(name);
        this.roster.add(player.getUniqueId(), new ForceItemPlayer(player, Material.DIRT, 0, 0));
        return player;
    }

    private ForceItemPlayer entryOf(PlayerMock player) {
        return this.roster.get(player.getUniqueId());
    }

    @Nested
    class ResolvingTheCaller {

        @Test
        void someoneWithNoRosterEntryIsRefused() {
            PlayerMock caller = server.addPlayer("Stranger");

            command.onCommand(caller, null, "teams", new String[]{"list"});

            assertTrue(caller.nextMessage().contains("not in this round"));
            verifyNoInteractions(teamManager);
        }

        @Test
        void leaveActsOnTheCaller() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[]{"leave"});

            verify(teamManager).leave(entryOf(caller));
        }

        @Test
        void listActsOnTheCaller() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[]{"list"});

            verify(teamManager).showTeamList(entryOf(caller));
        }
    }

    @Nested
    class ResolvingTheTarget {

        @Test
        void inviteHandsOverBothEntries() {
            PlayerMock caller = onRoster("Understudy1");
            PlayerMock target = onRoster("Understudy2");

            command.onCommand(caller, null, "teams", new String[]{"invite", "Understudy2"});

            verify(teamManager).invite(entryOf(caller), entryOf(target));
        }

        @Test
        void acceptAndDeclineDoTheSame() {
            PlayerMock caller = onRoster("Understudy1");
            PlayerMock target = onRoster("Understudy2");

            command.onCommand(caller, null, "teams", new String[]{"accept", "Understudy2"});
            command.onCommand(caller, null, "teams", new String[]{"decline", "Understudy2"});

            verify(teamManager).accept(entryOf(caller), entryOf(target));
            verify(teamManager).decline(entryOf(caller), entryOf(target));
        }

        /** The double-lookup bug: the null check must guard the object actually used. */
        @Test
        void anOfflineNameIsRefusedBeforeAnythingIsHandedOver() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[]{"invite", "Nobody"});

            assertTrue(caller.nextMessage().contains("is not online"));
            verify(teamManager, never()).invite(any(), any());
        }

        /** Online but never joined the round — TeamsManager would have dereferenced a null. */
        @Test
        void anOnlineTargetWithNoRosterEntryIsRefused() {
            PlayerMock caller = onRoster("Understudy1");
            server.addPlayer("Bystander");

            command.onCommand(caller, null, "teams", new String[]{"invite", "Bystander"});

            assertTrue(caller.nextMessage().contains("not in this round"));
            verify(teamManager, never()).invite(any(), any());
        }
    }

    @Nested
    class Usage {

        @Test
        void anUnknownOneArgumentSubcommandGetsHelp() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[]{"banana"});

            verifyNoInteractions(teamManager);
            assertTrue(caller.nextMessage() != null, "the help message goes out");
        }

        @Test
        void anUnknownTwoArgumentSubcommandGetsHelpWithoutLookingUpTheTarget() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[]{"banana", "Understudy2"});

            verifyNoInteractions(teamManager);
        }

        @Test
        void noArgumentsGetsHelp() {
            PlayerMock caller = onRoster("Understudy1");

            command.onCommand(caller, null, "teams", new String[0]);

            verifyNoInteractions(teamManager);
        }
    }
}
