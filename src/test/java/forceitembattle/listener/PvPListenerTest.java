package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.model.GameState;
import forceitembattle.settings.GameSettings;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link PvPListener}'s outermost gate: <b>nothing takes damage unless the round is running</b>.
 *
 * <p>This is what makes a pause genuinely safe. Players are pinned to their block by
 * {@code GameRulesListener}, locked out of every inventory action by {@code PreGameLockListener},
 * and — here — cannot be hurt at all. A player standing in lava when {@code /pause} is typed does
 * not burn, so a death during a pause cannot happen and nothing downstream has to cope with one.
 */
class PvPListenerTest extends ListenerTestBase {

    private PvPListener listener;

    @BeforeEach
    void setUpListener() {
        this.listener = new PvPListener(this.roundPhase, mock(GameSettings.class));
    }

    private EntityDamageEvent damage(PlayerMock player, EntityDamageEvent.DamageCause cause) {
        return new EntityDamageEvent(player, cause,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build(),
                4.0);
    }

    /** The pause specifically — the case that matters and the one this class exists to state. */
    @Test
    void nothingCanBeHurtWhilePaused() {
        phase(GameState.PAUSED_GAME);

        EntityDamageEvent event = damage(player("Understudy1"), EntityDamageEvent.DamageCause.LAVA);
        this.listener.onEntityDamage(event);

        assertTrue(event.isCancelled(),
                "a player standing in lava when /pause is typed must not burn");
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING", "PAUSED_GAME", "END_GAME"})
    void damageIsRefusedOutsideARunningRound(GameState state) {
        phase(state);

        EntityDamageEvent event = damage(player("Understudy1"), EntityDamageEvent.DamageCause.FALL);
        this.listener.onEntityDamage(event);

        assertTrue(event.isCancelled(), state + " must not allow damage");
    }

    /** Every cause, not just the ones with special handling further down the method. */
    @ParameterizedTest
    @EnumSource(value = EntityDamageEvent.DamageCause.class,
            names = {"LAVA", "FIRE", "FIRE_TICK", "FALL", "DROWNING", "STARVATION", "VOID"})
    void everyCauseOfDamageIsRefusedWhilePaused(EntityDamageEvent.DamageCause cause) {
        phase(GameState.PAUSED_GAME);

        EntityDamageEvent event = damage(player("Understudy1"), cause);
        this.listener.onEntityDamage(event);

        assertTrue(event.isCancelled(), cause + " should not hurt a player during a pause");
    }

    @Test
    void aRunningRoundAllowsDamage() {
        phase(GameState.MID_GAME);

        EntityDamageEvent event = damage(player("Understudy1"), EntityDamageEvent.DamageCause.FALL);
        this.listener.onEntityDamage(event);

        assertFalse(event.isCancelled());
    }
}
