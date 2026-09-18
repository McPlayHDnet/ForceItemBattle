package forceitembattle.randomevents;

import forceitembattle.util.Scheduler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpecialTrader implements RandomEvent {

    private static final int RETRY_SECONDS = 20;
    private static final int MAX_ATTEMPTS = 5;

    private final EventContext context;

    @Override
    public void start() {
        this.attemptSpawn(1);
    }

    @Override
    public boolean isInstant() {
        return true;
    }

    /**
     * An ocean start can leave no solid ground near spawn. Since this event fires at most once
     * per round, a failed roll is worth retrying rather than writing off.
     */
    private void attemptSpawn(int attempt) {
        if (this.context.traders().spawnSpecialTrader()) {
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            this.context.plugin().getLogger().warning("Special Trader could not be spawned after "
                    + MAX_ATTEMPTS + " attempts; skipping it this round.");
            return;
        }

        Scheduler.runLaterSync(() -> {
            if (!this.context.roundPhase().roundRunning()) {
                return;
            }
            this.attemptSpawn(attempt + 1);
        }, RETRY_SECONDS * 20L);
    }
}
