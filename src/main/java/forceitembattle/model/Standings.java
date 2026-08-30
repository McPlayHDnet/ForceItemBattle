package forceitembattle.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Who finished where.
 *
 * <p>The ranking feeds both the {@code /result} reveal and the {@code placement} / {@code won}
 * fields written to match history, so a tie handled wrongly here shows up as a wrong winner.
 *
 * <p>These were methods on {@code Gamemanager}, and pure ones — they take a collection and a way to
 * score it and return a map. Keeping them there meant {@code AchievementManager} had to depend on
 * the class that starts and finishes rounds in order to ask who came first, which was the last of
 * the seven dependency cycles that ran through {@code Gamemanager}.
 */
public final class Standings {

    private Standings() {
    }

    /**
     * Places, densely ranked: equal scores share a place and the next distinct score takes the
     * next one, so two players tied at the top are both first and the third is second.
     */
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

    /** Solo placings, by each player's own score. */
    public static Map<ForceItemPlayer, Integer> ofPlayers(Map<UUID, ForceItemPlayer> playerMap) {
        return of(new ArrayList<>(playerMap.values()), ForceItemPlayer::currentScore);
    }

    /** Team placings, by the shared score. */
    public static Map<Team, Integer> ofTeams(List<Team> teams) {
        return of(teams, Team::getCurrentScore);
    }

    /**
     * The roster ordered by score.
     *
     * <p>Ties break on UUID so the order is stable between calls — without it the result screen
     * could deal two tied players out in a different order each time it was opened.
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
