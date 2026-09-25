package forceitembattle.manager;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.FileLogger;
import forceitembattle.util.Scheduler;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class TimerManagerTest {

    private ServerMock server;
    private RoundPhase phase;
    private RoundClock clock;
    private Gamemanager gamemanager;
    private TimerManager timer;

    @TempDir
    Path logs;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        Scheduler.init(MockBukkit.createMockPlugin());
        FileLogger.init(this.logs.toFile());

        this.phase = new RoundPhase();
        this.clock = new RoundClock();
        this.gamemanager = mock(Gamemanager.class);
        doAnswer(invocation -> {
            this.phase.moveTo(GameState.END_GAME);
            return null;
        }).when(this.gamemanager).finishGame();

        this.timer = new TimerManager(MockBukkit.createMockPlugin(), this.clock, new Roster(), this.phase,
                mock(GameSettings.class), this.gamemanager, mock(ItemDifficultiesManager.class),
                mock(RandomEventManager.class), mock(TabListManager.class));
        this.timer.enable();
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    private void playARound(int seconds) {
        this.clock.startRound(seconds);
        this.phase.moveTo(GameState.MID_GAME);
        this.server.getScheduler().performTicks(20L * (seconds + 2));
    }

    @Test
    void aRoundEndsWhenItsClockRunsOut() {
        playARound(3);

        verify(this.gamemanager).finishGame();
    }

    /** The task used to cancel itself on expiry, so the second round of a session never ended. */
    @Test
    void aSecondRoundInTheSameSessionAlsoEnds() {
        playARound(3);
        playARound(3);

        verify(this.gamemanager, times(2)).finishGame();
    }

    @Test
    void theResultScreenDoesNotFinishTheRoundAgain() {
        playARound(3);

        this.server.getScheduler().performTicks(20L * 10);

        verify(this.gamemanager).finishGame();
    }
}
