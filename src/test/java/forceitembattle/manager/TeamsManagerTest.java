package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class TeamsManagerTest {

    private ServerMock server;
    private Roster roster;
    private TeamsManager teams;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roster = new Roster();
        this.teams = new TeamsManager(mock(JavaPlugin.class), this.roster, mock(ScoreboardManager.class));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, null, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    @Nested
    class Inviting {

        @Test
        void aSentInviteRegistersTheInvitersTeam() {
            ForceItemPlayer inviter = join("Understudy1");
            ForceItemPlayer target = join("Understudy2");

            teams.invite(inviter, target);

            assertTrue(teams.getTeams().contains(inviter.currentTeam()));
        }

        @Test
        void invitingYourselfCreatesNoTeam() {
            ForceItemPlayer inviter = join("Understudy1");

            teams.invite(inviter, inviter);

            assertNull(inviter.currentTeam());
            assertTrue(teams.getTeams().isEmpty());
        }

        @Test
        void invitingSomeoneAlreadyInvitedCreatesNoTeam() {
            ForceItemPlayer first = join("Understudy1");
            ForceItemPlayer second = join("Understudy2");
            ForceItemPlayer target = join("Understudy3");
            teams.invite(first, target);

            teams.invite(second, target);

            assertNull(second.currentTeam());
            assertEquals(1, teams.getTeams().size());
        }

        /** No team of the inviter's may exist unless the round can see it. */
        @Test
        void everyAssignedTeamIsRegistered() {
            ForceItemPlayer a = join("Understudy1");
            ForceItemPlayer b = join("Understudy2");
            ForceItemPlayer c = join("Understudy3");

            teams.invite(a, a);
            teams.invite(a, b);
            teams.invite(c, b);
            teams.invite(c, c);

            for (ForceItemPlayer entry : roster.players().values()) {
                if (entry.currentTeam() != null) {
                    assertTrue(teams.getTeams().contains(entry.currentTeam()),
                            entry.player().getName() + " holds a team the round cannot see");
                }
            }
        }

        @Test
        void acceptingJoinsTheInvitersTeam() {
            ForceItemPlayer inviter = join("Understudy1");
            ForceItemPlayer target = join("Understudy2");
            teams.invite(inviter, target);

            teams.accept(target, inviter);

            assertSame(inviter.currentTeam(), target.currentTeam());
        }
    }
}
