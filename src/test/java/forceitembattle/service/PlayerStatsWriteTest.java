package forceitembattle.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Where a player's own contribution gets written.
 *
 * This replaced the same ten-line branch copy-pasted at five call sites (found item, back-to-back
 * rarity, wheel-of-fortune use, death, antimatter teleporter). Routing to the wrong row is silent —
 * the stat lands somewhere, just not where the site meant — so each branch is verified here.
 */
class PlayerStatsWriteTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private FibStatisticsClient statistics;

    @BeforeEach
    void setUp() {
        statistics = mock(FibStatisticsClient.class);
    }

    private static ForceItemPlayer player(UUID uuid) {
        Player bukkit = mock(Player.class);
        Mockito.when(bukkit.getUniqueId()).thenReturn(uuid);
        return new ForceItemPlayer(bukkit, Material.DIRT, 0, 0);
    }

    private void record(ForceItemPlayer forceItemPlayer) {
        PlayerStatsWrite.record(statistics, ALICE, forceItemPlayer,
                () -> FIBServiceClient.soloUpdate().deathsAdd(1L),
                () -> FIBServiceClient.memberUpdate().deathsAdd(1L));
    }

    @Test
    void aSoloPlayerGetsASoloWrite() {
        record(player(ALICE));

        verify(statistics).updateSoloStatisticsAsync(eq(ALICE), any(FibSoloStatisticsUpdateRequestDto.class));
        verify(statistics, never()).updateMemberStatisticsAsync(any(), any(), any(), any());
    }

    /** The member row is addressed (player, teammate, member) with the acting player as member. */
    @Test
    void aPlayerInATeamGetsAMemberWriteAgainstTheirOwnContribution() {
        ForceItemPlayer alice = player(ALICE);
        ForceItemPlayer bob = player(BOB);
        Team team = new Team(1, Material.STONE, 0, 0, alice, bob);
        alice.setCurrentTeam(team);
        bob.setCurrentTeam(team);

        record(alice);

        verify(statistics).updateMemberStatisticsAsync(
                eq(ALICE), eq(BOB), eq(ALICE), any(FibTeamMemberStatsUpdateRequestDto.class));
        verify(statistics, never()).updateSoloStatisticsAsync(any(), any());
    }

    /**
     * An odd player count leaves someone alone in a team. They have no member row to write to, and
     * must not silently fall through to solo stats — a team game never touches the solo table.
     */
    @Test
    void aOnePersonTeamWritesNothing() {
        ForceItemPlayer lonely = player(ALICE);
        Team team = new Team(1, Material.STONE, 0, 0, lonely);
        lonely.setCurrentTeam(team);

        record(lonely);

        verifyNoInteractions(statistics);
    }

    /**
     * Someone who connected after the round began has no roster entry, and nothing to attribute.
     * Falling through to a solo write is how a spectator who wandered over an antimatter teleporter
     * pad ends up on the stats page owning exactly one teleporter use and nothing else.
     */
    @Test
    void aPlayerWithNoRosterEntryWritesNothing() {
        record(null);

        verifyNoInteractions(statistics);
    }

    /**
     * The other shape of spectator: someone who took the lobby's spectate toggle keeps their roster
     * entry, so the null check above never sees them and the flag is what has to stop the write.
     */
    @Test
    void aSpectatorWritesNothing() {
        ForceItemPlayer spectator = player(ALICE);
        spectator.setSpectator(true);

        record(spectator);

        verifyNoInteractions(statistics);
    }

    /** A spectator on a team is still a spectator — the team branch must not rescue the write. */
    @Test
    void aSpectatorInATeamWritesNothing() {
        ForceItemPlayer spectator = player(ALICE);
        ForceItemPlayer bob = player(BOB);
        Team team = new Team(1, Material.STONE, 0, 0, spectator, bob);
        spectator.setCurrentTeam(team);
        bob.setCurrentTeam(team);
        spectator.setSpectator(true);

        record(spectator);

        verifyNoInteractions(statistics);
    }

    /** Only the branch that runs builds its payload; the other supplier is never invoked. */
    @Test
    void theUnusedPayloadIsNeverBuilt() {
        boolean[] memberBuilt = {false};

        PlayerStatsWrite.record(statistics, ALICE, player(ALICE),
                () -> FIBServiceClient.soloUpdate().deathsAdd(1L),
                () -> {
                    memberBuilt[0] = true;
                    return FIBServiceClient.memberUpdate();
                });

        org.junit.jupiter.api.Assertions.assertFalse(memberBuilt[0]);
    }
}
