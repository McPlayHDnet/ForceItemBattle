package forceitembattle.listener;

import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.util.Scheduler;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * A server, a world and a player, for the listeners.
 *
 * <p>These were the least testable classes in the plugin for two reasons, and both are now gone.
 * They held the whole plugin, so a test had to stand up 23 managers — the sweep replaced that with
 * named collaborators, and nine of the ten phase-gated listeners now hold nothing but a
 * {@link RoundPhase}. And their events carry {@code ItemStack}s and {@code Player}s, which needed a
 * running server — MockBukkit is the server.
 *
 * <p>{@link Scheduler} is pointed at a registered {@code PluginMock} here, so a handler that
 * schedules can be driven with {@link #tick(long)}. It is reset in teardown because the field is
 * static and outlives the mocked server -- see {@code SchedulerHarnessTest}.
 *
 * <p>The listeners are constructed directly rather than registered with the plugin manager. These
 * tests are about what a handler decides, not about Bukkit's dispatch, and calling the method is
 * the shortest path to that question.
 */
abstract class ListenerTestBase {

    protected ServerMock server;
    protected WorldMock world;
    protected RoundPhase roundPhase;

    @BeforeEach
    void setUpServer() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.roundPhase = new RoundPhase();
        Scheduler.init(MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDownServer() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    /**
     * Runs whatever the handler under test handed to {@link Scheduler}. A listener that schedules
     * has done nothing observable until this is called, which is worth asserting in its own right.
     */
    protected void tick(long ticks) {
        this.server.getScheduler().performTicks(ticks);
    }

    protected PlayerMock player(String name) {
        return this.server.addPlayer(name);
    }

    /** Puts the round in a phase. A real {@link RoundPhase}; it depends on nothing. */
    protected void phase(GameState state) {
        this.roundPhase.moveTo(state);
    }

    protected Location at(double x, double y, double z) {
        return new Location(this.world, x, y, z);
    }
}
