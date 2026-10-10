package forceitembattle.model.stats;

/** The name stays a string: only the caller knows what to do when it doesn't resolve to a Material. */
public record ItemCount(String itemName, long count) {
}
