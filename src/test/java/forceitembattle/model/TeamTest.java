package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * {@link Team#teammateOf} and {@link Team#isPrimaryWriter}, which replaced nine hand-rolled
 * teammate lookups and two inlined copies of the lower-UUID dedupe rule.
 */
class TeamTest {

    private static ForceItemPlayer player(String seed) {
        return new ForceItemPlayer(Players.mockPlayer(seed), Material.DIRT, 0, 0);
    }

    private static Team team(ForceItemPlayer... members) {
        Team team = new Team(1, Material.STONE, 0, 0, members);
        for (ForceItemPlayer member : members) {
            member.setCurrentTeam(team);
        }
        return team;
    }

    @Test
    void teammateOfReturnsTheOtherMember() {
        ForceItemPlayer alice = player("a");
        ForceItemPlayer bob = player("b");
        Team team = team(alice, bob);

        assertSame(bob, team.teammateOf(alice).orElseThrow());
        assertSame(alice, team.teammateOf(bob).orElseThrow());
    }

    @Test
    void teammateOfIsEmptyForAOnePersonTeam() {
        ForceItemPlayer lonely = player("a");

        assertTrue(team(lonely).teammateOf(lonely).isEmpty());
    }

    /**
     * Exactly one member of a pair must own once-per-team writes (gamesPlayed, gamesWon), or every
     * game counts twice. Both sides have to agree without coordinating, so the rule is "lowest UUID".
     */
    @Test
    void exactlyOneSideOfAPairIsThePrimaryWriter() {
        ForceItemPlayer alice = player("a");
        ForceItemPlayer bob = player("b");
        Team team = team(alice, bob);

        assertTrue(team.isPrimaryWriter(alice));
        assertFalse(team.isPrimaryWriter(bob));
    }

    @Test
    void primaryWriterDoesNotDependOnConstructionOrder() {
        ForceItemPlayer alice = player("a");
        ForceItemPlayer bob = player("b");
        Team team = team(bob, alice); // reversed

        assertTrue(team.isPrimaryWriter(alice));
        assertFalse(team.isPrimaryWriter(bob));
    }

    @Test
    void theOnlyMemberOfATeamIsItsPrimaryWriter() {
        ForceItemPlayer lonely = player("a");

        assertTrue(team(lonely).isPrimaryWriter(lonely));
    }

    @Test
    void foundItemsAreExposedButNotMutable() {
        Team team = team(player("a"));
        team.addFoundItemToList(null); // nulls are ignored rather than stored

        assertEquals(0, team.getFoundItems().size());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> team.getFoundItems().add(null));
    }

    @Test
    void teamDisplayIsBracketedWhetherNamedOrNot() {
        Team unnamed = team(player("a"));
        assertTrue(unnamed.getTeamDisplay().contains("[#1]"));

        unnamed.setName("Rockets");
        assertTrue(unnamed.getTeamDisplay().contains("[Rockets]"));
    }
}
