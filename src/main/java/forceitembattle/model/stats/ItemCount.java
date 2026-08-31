package forceitembattle.model.stats;

/**
 * How many times one item has been found. The service names items as strings; turning that into a
 * {@link org.bukkit.Material} is the caller's business, because only a caller knows what to do when
 * the name does not resolve.
 */
public record ItemCount(String itemName, long count) {
}
