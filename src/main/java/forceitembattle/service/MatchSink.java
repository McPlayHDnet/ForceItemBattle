package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import java.util.UUID;

/** Where a finished match goes. See {@code CONTEXT.md § Service Writes}. */
public interface MatchSink {

    /** @param onPersisted runs once the write lands, and not at all if it fails */
    void submitMatch(UUID matchId, FibMatchSubmitRequestDto request, Runnable onPersisted);
}
