package forceitembattle.achievements.global;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A snapshot of every {@link GlobalStat} for one player, solo and team already folded together. */
public record GlobalStats(Map<GlobalStat, Long> values) {

    public static GlobalStats of(GlobalStatSources sources) {
        Map<GlobalStat, Long> values = new EnumMap<>(GlobalStat.class);
        for (GlobalStat stat : GlobalStat.values()) {
            values.put(stat, stat.read(sources));
        }
        return new GlobalStats(Collections.unmodifiableMap(values));
    }

    public long get(GlobalStat stat) {
        return values.getOrDefault(stat, 0L);
    }

    public boolean isMet(GlobalRule rule) {
        return rule.isMet(get(rule.stat()));
    }
}
