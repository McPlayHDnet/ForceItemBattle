package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Finishing places. The ranking feeds both the /result reveal and the {@code placement} / {@code won}
 * fields written to match history, so a tie handled wrongly here shows up as a wrong winner.
 */
class PlacementTest {

    private final Gamemanager gamemanager = new Gamemanager(mock(ForceItemBattle.class));

    private static ForceItemPlayer player(String seed, int score) {
        Player bukkit = mock(Player.class);
        Mockito.when(bukkit.getUniqueId()).thenReturn(
                UUID.fromString("00000000-0000-0000-0000-00000000000" + seed));
        return new ForceItemPlayer(bukkit, Material.DIRT, 0, score);
    }

    private static Map<UUID, ForceItemPlayer> roster(ForceItemPlayer... players) {
        Map<UUID, ForceItemPlayer> map = new LinkedHashMap<>();
        for (ForceItemPlayer player : players) {
            map.put(player.player().getUniqueId(), player);
        }
        return map;
    }

    @Test
    void placesRunHighestScoreFirst() {
        ForceItemPlayer low = player("a", 1);
        ForceItemPlayer high = player("b", 9);
        ForceItemPlayer mid = player("c", 5);

        Map<ForceItemPlayer, Integer> places = gamemanager.calculatePlaces(roster(low, high, mid));

        assertEquals(1, places.get(high));
        assertEquals(2, places.get(mid));
        assertEquals(3, places.get(low));
    }

    /**
     * Equal scores share a place, and the next distinct score takes the place immediately after —
     * 1, 1, 2 rather than the 1, 1, 3 that competition ranking would give.
     */
    @Test
    void tiedScoresShareAPlaceAndDoNotSkipTheNextOne() {
        ForceItemPlayer first = player("a", 9);
        ForceItemPlayer alsoFirst = player("b", 9);
        ForceItemPlayer third = player("c", 2);

        Map<ForceItemPlayer, Integer> places = gamemanager.calculatePlaces(roster(first, alsoFirst, third));

        assertEquals(1, places.get(first));
        assertEquals(1, places.get(alsoFirst));
        assertEquals(2, places.get(third));
    }

    @Test
    void everyoneOnZeroSharesFirstPlace() {
        ForceItemPlayer a = player("a", 0);
        ForceItemPlayer b = player("b", 0);

        Map<ForceItemPlayer, Integer> places = gamemanager.calculatePlaces(roster(a, b));

        assertEquals(1, places.get(a));
        assertEquals(1, places.get(b));
    }

    @Test
    void teamsAreRankedTheSameWay() {
        Team winner = new Team(1, Material.STONE, 12, 0);
        Team runnerUp = new Team(2, Material.STONE, 4, 0);
        Team tiedRunnerUp = new Team(3, Material.STONE, 4, 0);

        Map<Team, Integer> places =
                gamemanager.calculatePlaces(List.of(runnerUp, winner, tiedRunnerUp));

        assertEquals(1, places.get(winner));
        assertEquals(2, places.get(runnerUp));
        assertEquals(2, places.get(tiedRunnerUp));
    }

    @Test
    void sortByValueOrdersDescendingWhenAskedFor() {
        ForceItemPlayer low = player("a", 1);
        ForceItemPlayer high = player("b", 9);

        List<ForceItemPlayer> descending =
                new ArrayList<>(gamemanager.sortByValue(roster(low, high), false).values());

        assertEquals(high, descending.get(0));
        assertEquals(low, descending.get(1));
    }

    @Test
    void sortByValueOrdersAscendingWhenAskedFor() {
        ForceItemPlayer low = player("a", 1);
        ForceItemPlayer high = player("b", 9);

        List<ForceItemPlayer> ascending =
                new ArrayList<>(gamemanager.sortByValue(roster(low, high), true).values());

        assertEquals(low, ascending.get(0));
        assertEquals(high, ascending.get(1));
    }
}
