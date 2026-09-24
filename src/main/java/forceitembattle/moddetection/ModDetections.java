package forceitembattle.moddetection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Kept past a player's quit on purpose: a player who leaves before an op checks should still show up.
public class ModDetections {

    public record Detection(UUID playerId, String playerName, ModFinding finding) {
        public String message() {
            return this.finding.message(this.playerName);
        }
    }

    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<UUID, Set<ModFinding>> findings = new LinkedHashMap<>();

    /** Returns false when this player already has this finding. */
    public boolean record(UUID playerId, String playerName, ModFinding finding) {
        this.names.put(playerId, playerName);
        return this.findings.computeIfAbsent(playerId, id -> EnumSet.noneOf(ModFinding.class)).add(finding);
    }

    public List<Detection> all() {
        List<Detection> all = new ArrayList<>();
        this.findings.forEach((playerId, playerFindings) -> playerFindings.forEach(
                finding -> all.add(new Detection(playerId, this.names.get(playerId), finding))));
        return all;
    }

    public void clear() {
        this.names.clear();
        this.findings.clear();
    }
}
