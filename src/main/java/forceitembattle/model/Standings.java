package forceitembattle.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/** Feeds both the /result reveal and the placement/won fields in match history. */
public final class Standings {

    private Standings() {
    }

    /** Dense ranking: two tied at the top are both first and the next is second. */
    public static <T> Map<T, Integer> of(List<T> entities, ToIntFunction<T> score) {
        List<T> sorted = entities.stream()
                .sorted(Comparator.comparingInt(score).reversed())
                .toList();

        Map<T, Integer> placesMap = new LinkedHashMap<>();

        int place = 0;
        Integer previousScore = null;
        for (T entity : sorted) {
            int currentScore = score.applyAsInt(entity);
            if (previousScore == null || currentScore != previousScore) {
                place++;
            }
            placesMap.put(entity, place);
            previousScore = currentScore;
        }
        return placesMap;
    }

    public static Map<ForceItemPlayer, Integer> ofPlayers(Map<UUID, ForceItemPlayer> playerMap) {
        return of(new ArrayList<>(playerMap.values()), ForceItemPlayer::currentScore);
    }

    public static Map<Team, Integer> ofTeams(List<Team> teams) {
        return of(teams, Team::score);
    }

    /**
     * Ties break on UUID, so the result screen deals tied players out the same way every time.
     *
     * @param ascending lowest score first when true
     */
    public static Map<UUID, ForceItemPlayer> sortedByScore(Map<UUID, ForceItemPlayer> roster,
                                                           boolean ascending) {
        Comparator<Map.Entry<UUID, ForceItemPlayer>> comparator =
                Comparator.comparingInt((Map.Entry<UUID, ForceItemPlayer> e) -> e.getValue().currentScore())
                        .thenComparing(Map.Entry::getKey);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return roster.entrySet().stream()
                .sorted(comparator)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b,
                        LinkedHashMap::new));
    }
}
