package forceitembattle.achievements.global;

import forceitembattle.model.StatsView;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A snapshot of every {@link GlobalStat} for one player, solo and team already folded together. */
public record GlobalStats(Map<GlobalStat, Long> values) {

    public static GlobalStats of(StatsView solo, StatsView team) {
        Map<GlobalStat, Long> folded = new EnumMap<>(GlobalStat.class);
        for (GlobalStat stat : GlobalStat.values()) {
            folded.put(stat, stat.combine(solo, team));
        }
        return new GlobalStats(Collections.unmodifiableMap(folded));
    }

    public long get(GlobalStat stat) {
        return values.getOrDefault(stat, 0L);
    }

    public boolean isMet(GlobalRule rule) {
        return rule.isMet(get(rule.stat()));
    }
}
