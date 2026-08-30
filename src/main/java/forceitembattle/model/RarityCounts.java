package forceitembattle.model;

/**
 * Back-to-backs by rarity. The read half of what {@link Rarity} writes.
 *
 * <p>Pass 1 left {@code Rarity} holding a generated import because splitting its read and write
 * mappings across two files would have broken up a cohesive table. This does not split it: the
 * table stays whole in {@code Rarity}, and only the type it reads through changes.
 */
public record RarityCounts(long rare, long epic, long legendary, long rngesus, long extraordinary) {

    public static final RarityCounts NONE = new RarityCounts(0, 0, 0, 0, 0);
}
