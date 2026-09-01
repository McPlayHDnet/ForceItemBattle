package forceitembattle.model;

/** Back-to-backs by rarity. The read half of what {@link Rarity} writes. */
public record RarityCounts(long rare, long epic, long legendary, long rngesus, long extraordinary) {

    public static final RarityCounts NONE = new RarityCounts(0, 0, 0, 0, 0);
}
