package forceitembattle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Mocked Bukkit players for tests.
 *
 * Only the UUID and the name are stubbed — that is everything the code under test touches, and keeping
 * the mock this thin means a test fails loudly if the code under test starts reaching for the
 * server instead of staying in our own objects.
 */
public final class Players {

    private Players() {
    }

    /**
     * @param seed a single hex digit, so UUID ordering is predictable — {@code "a"} sorts before
     *             {@code "b"}, which is what the primary-writer rule keys on
     */
    public static Player mockPlayer(String seed) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(
                UUID.fromString("00000000-0000-0000-0000-00000000000" + seed));
        when(player.getName()).thenReturn("player_" + seed);
        return player;
    }
}
