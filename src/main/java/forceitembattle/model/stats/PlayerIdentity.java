package forceitembattle.model.stats;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Who a statistic or an unlock belongs to, in the game's own words.
 *
 * <p>Replaces {@code FibPlayerIdentityDto} at every call site outside {@code service/}. The
 * rendering rule below existed three times — byte-identical in {@code CommandStats} and
 * {@code CommandLeaderboard}, and differing only in its fallback string in
 * {@code AchievementInventory} — which is the shape a rule takes just before the copies start
 * disagreeing about something that matters.
 */
public record PlayerIdentity(@Nullable UUID uuid, @Nullable String name) {

    /**
     * What to show for this player: their name, or the front of their UUID when the service knows
     * the id but not the name.
     *
     * @param fallback shown when there is no identity at all. Callers differ on the wording —
     *                 "?" in a stats line, "Unknown" in a GUI lore line — which is the only thing
     *                 the three copies actually disagreed about.
     */
    public static String displayName(@Nullable PlayerIdentity identity, String fallback) {
        if (identity == null || identity.uuid() == null) {
            return fallback;
        }
        return identity.name() != null ? identity.name() : identity.uuid().toString().substring(0, 8);
    }
}
