package forceitembattle.achievements.global;


public record GlobalRule(GlobalStat stat, long threshold) {

    public GlobalRule {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Global threshold must be positive, got " + threshold);
        }
    }

    public boolean isMet(long current) {
        return current >= threshold;
    }
}
