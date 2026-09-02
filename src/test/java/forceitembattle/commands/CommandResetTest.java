package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.commands.admin.CommandReset;
import forceitembattle.util.SeedPool;
import forceitembattle.util.WorldReset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /reset}: kick everyone and restart on a new world.
 *
 * <p>The invariant this exists for is the one the command's own comment states: <b>the seed is
 * resolved before anybody is kicked</b>. Every refusal path — no seed pool, an unknown biome, a
 * pool that throws — has to leave the server exactly as it found it, because the alternative is a
 * room full of kicked players and no reset scheduled. That state is unrecoverable by anything short
 * of restarting the server by hand, and it is one moved statement away.
 *
 * <p>So each refusal is asserted three ways over: the player is told, nobody was kicked, and no
 * reset was scheduled.
 *
 * <p>{@code scheduleReset} itself is not exercised. It registers a JVM shutdown hook, deletes the
 * world directory and calls {@code Bukkit.restart()}; {@link WorldReset} is a mock here, so what is pinned
 * is the decision to call it and the seed handed over — the part this command owns.
 */
class CommandResetTest {

    private ServerMock server;
    private WorldReset worldReset;
    private SeedPool seedPool;
    private CommandReset command;

    /** Every player this test joined, so "was anyone kicked" survives them going offline. */
    private final List<PlayerMock> joined = new ArrayList<>();

    /** Kick screens, in plain text. The command's only outward effect besides the reset itself. */
    private final List<String> kickScreens = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.worldReset = mock(WorldReset.class);
        this.seedPool = mock(SeedPool.class);
        this.joined.clear();
        this.kickScreens.clear();


        this.server.getPluginManager().registerEvents(new KickRecorder(), MockBukkit.createMockPlugin());

        this.command = new CommandReset(this.seedPool, this.worldReset);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Public so Bukkit's reflective dispatch can see the handler.
     *
     * <p>It reads {@code leaveMessage()}, not {@code reason()}. MockBukkit's {@code kick(Component)}
     * builds its {@code PlayerKickEvent} with the two components the other way round — a literal
     * "Plugin" lands in the reason and the component the caller passed lands in the leave message.
     * On a real server the two are the other way round. This is asserting what the command handed
     * to {@code kick}, so it follows MockBukkit's slot rather than the name.
     */
    public class KickRecorder implements Listener {
        @EventHandler
        public void onKick(PlayerKickEvent event) {
            kickScreens.add(PlainTextComponentSerializer.plainText().serialize(event.leaveMessage()));
        }
    }

    // --- fixtures ---------------------------------------------------------------------------

    private PlayerMock join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        this.joined.add(player);
        return player;
    }

    private PlayerMock joinOp(String name) {
        PlayerMock player = join(name);
        player.setOp(true);
        return player;
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "reset", args);
    }

    /** A working pool holding one group. */
    private void poolWith(String group, long seed) {
        when(this.seedPool.isAvailable()).thenReturn(true);
        when(this.seedPool.has(group)).thenReturn(true);
        when(this.seedPool.groups()).thenReturn(Set.of(group));
        try {
            when(this.seedPool.randomSeed(group)).thenReturn(seed);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Nobody kicked, nothing scheduled: what every refusal must leave behind. */
    private void assertServerUntouched() {
        verify(this.worldReset, never()).scheduleReset(any());
        assertTrue(this.kickScreens.isEmpty(),
                "a path that refused kicked somebody: " + this.kickScreens);
        for (PlayerMock player : this.joined) {
            assertTrue(player.isOnline(), player.getName() + " was kicked by a path that refused");
        }
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class WithNoBiome {

        /** A bare {@code /reset} is a random world, expressed as a null seed. */
        @Test
        void aRandomWorldIsScheduledWithNoSeed() {
            PlayerMock admin = joinOp("Admin");

            run(admin);

            verify(worldReset).scheduleReset(null);
        }

        @Test
        void everybodyIsKickedNotJustTheCaller() {
            joinOp("Admin");
            join("Understudy1");

            run(joined.get(0));

            assertEquals(2, kickScreens.size(), "the whole server goes, not just the caller");
        }

        @Test
        void theKickScreenSaysWhatHappened() {
            PlayerMock admin = joinOp("Admin");

            run(admin);

            assertTrue(kickScreens.get(0).contains("The world is being reset"), kickScreens.toString());
        }

        /** The seed pool is not even consulted when no biome was asked for. */
        @Test
        void theSeedPoolIsNotConsulted() {
            PlayerMock admin = joinOp("Admin");

            run(admin);

            verify(seedPool, never()).isAvailable();
        }

        @Test
        void noForcedBiomeIsClaimed() {
            PlayerMock admin = joinOp("Admin");

            run(admin);

            assertFalse(kickScreens.get(0).contains("Forced Biome"), kickScreens.toString());
        }
    }

    @Nested
    class WithABiome {

        @Test
        void theBiomesSeedIsWhatGetsScheduled() {
            PlayerMock admin = joinOp("Admin");
            poolWith("jungle", 4242L);

            run(admin, "jungle");

            verify(worldReset).scheduleReset(eq(4242L));
        }

        @Test
        void theBiomeNameIsCaseInsensitive() {
            PlayerMock admin = joinOp("Admin");
            poolWith("jungle", 4242L);

            run(admin, "JUNGLE");

            verify(worldReset).scheduleReset(eq(4242L));
        }

        /** The kick screen names the biome, prettified, so players know what they are rejoining. */
        @Test
        void theKickScreenNamesThePrettifiedBiome() {
            PlayerMock admin = joinOp("Admin");
            poolWith("old_growth_pine_taiga", 1L);

            run(admin, "old_growth_pine_taiga");

            assertTrue(kickScreens.get(0).contains("Old Growth Pine Taiga"), kickScreens.toString());
        }
    }

    /** The half that matters: each of these must abort before the kick loop. */
    @Nested
    class Refusals {

        @Test
        void aMissingSeedPoolAbortsBeforeKickingAnyone() {
            PlayerMock admin = joinOp("Admin");
            command = new CommandReset(null, worldReset);
            ((CustomCommand) command).setContext(new CommandContext(null, null, null));

            run(admin, "jungle");

            assertSaid(admin, "seed pool not loaded");
            assertServerUntouched();
        }

        @Test
        void aSeedPoolThatFailedToLoadAbortsToo() {
            PlayerMock admin = joinOp("Admin");
            when(seedPool.isAvailable()).thenReturn(false);

            run(admin, "jungle");

            assertSaid(admin, "seed pool not loaded");
            assertServerUntouched();
        }

        @Test
        void anUnknownBiomeAbortsBeforeKickingAnyone() {
            PlayerMock admin = joinOp("Admin");
            when(seedPool.isAvailable()).thenReturn(true);
            when(seedPool.has(any())).thenReturn(false);

            run(admin, "not_a_biome");

            String said = screenOf(admin);
            assertTrue(said.contains("Unknown biome"), said);
            assertTrue(said.contains("not_a_biome"), said);
            assertServerUntouched();
        }

        /** Reading the seed file is the last thing that can fail, and it fails after validation. */
        @Test
        void anUnreadableSeedFileAbortsBeforeKickingAnyone() throws IOException {
            PlayerMock admin = joinOp("Admin");
            when(seedPool.isAvailable()).thenReturn(true);
            when(seedPool.has("jungle")).thenReturn(true);
            when(seedPool.randomSeed("jungle")).thenThrow(new IOException("seeds.txt is gone"));

            run(admin, "jungle");

            String said = screenOf(admin);
            assertTrue(said.contains("Failed to pick a seed"), said);
            assertTrue(said.contains("seeds.txt is gone"), "the cause is worth surfacing:\n" + said);
            assertServerUntouched();
        }

        /** The gate is declared, not hand-rolled: a non-op never reaches the body. */
        @Test
        void aNonOpIsRefusedByTheDeclaredPrecondition() {
            PlayerMock player = join("Understudy1");

            run(player, "jungle");

            assertSaid(player, "permission");
            assertServerUntouched();
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void theBiomeGroupsAreOffered() {
            PlayerMock admin = joinOp("Admin");
            poolWith("jungle", 1L);

            assertEquals(List.of("jungle"),
                    command.onTabComplete(admin, "reset", new String[]{""}));
        }

        @Test
        void whatIsTypedNarrowsTheOffer() {
            PlayerMock admin = joinOp("Admin");
            when(seedPool.isAvailable()).thenReturn(true);
            when(seedPool.groups()).thenReturn(Set.of("jungle", "desert"));

            assertEquals(List.of("desert"),
                    command.onTabComplete(admin, "reset", new String[]{"des"}));
        }

        @Test
        void withNoUsablePoolNothingIsOffered() {
            PlayerMock admin = joinOp("Admin");
            when(seedPool.isAvailable()).thenReturn(false);

            assertTrue(command.onTabComplete(admin, "reset", new String[]{""}).isEmpty());
        }

        @Test
        void withNoPoolAtAllNothingIsOffered() {
            PlayerMock admin = joinOp("Admin");
            command = new CommandReset(null, worldReset);
            ((CustomCommand) command).setContext(new CommandContext(null, null, null));

            assertTrue(command.onTabComplete(admin, "reset", new String[]{""}).isEmpty());
        }

        /** {@code /reset} takes one argument, so there is nothing to complete after it. */
        @Test
        void thereIsNothingToCompleteAfterTheBiome() {
            PlayerMock admin = joinOp("Admin");
            poolWith("jungle", 1L);

            assertTrue(command.onTabComplete(admin, "reset", new String[]{"jungle", ""}).isEmpty());
        }
    }
}
