package forceitembattle.model.stats;

import java.util.UUID;
import javax.annotation.Nullable;

/** Replaces {@code FibPlayerIdentityDto} outside {@code service/}. */
public record PlayerIdentity(@Nullable UUID uuid, @Nullable String name) {

    /**
     * The name, or the front of the UUID when the service knows only the id.
     *
     * @param fallback shown when there is no identity at all
     */
    public static String displayName(@Nullable PlayerIdentity identity, String fallback) {
        if (identity == null || identity.uuid() == null) {
            return fallback;
        }
        return identity.name() != null ? identity.name() : identity.uuid().toString().substring(0, 8);
    }
}
