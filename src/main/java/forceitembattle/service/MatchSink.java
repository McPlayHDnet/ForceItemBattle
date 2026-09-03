package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import java.util.UUID;

/**
 * Where a finished match goes, with the transport taken out.
 *
 * <p>{@link MatchHistoryReporter} assembles the whole submission — participants, teams, per-item
 * timings with the pauses subtracted, and the settings snapshot — and that assembly is the part
 * worth testing. It used to hand the result straight to a concrete client carrying OkHttp, so the
 * only way in was {@code new MatchHistoryReporter(null, null, null, null)} plus reflection, which is
 * what {@code PauseAccountingTest} was reduced to.
 *
 * <p>One method, because there is one submission. The production implementation is
 * {@link FibMatchHistoryClient}.
 */
public interface MatchSink {

    /**
     * @param onPersisted runs once the write has landed, and not at all if it fails — the game loop
     *                    hangs collection-achievement evaluation off it, which only makes sense
     *                    against a match the service already knows about
     */
    void submitMatch(UUID matchId, FibMatchSubmitRequestDto request, Runnable onPersisted);
}
