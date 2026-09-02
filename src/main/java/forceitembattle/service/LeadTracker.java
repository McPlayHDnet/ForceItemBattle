package forceitembattle.service;

public final class LeadTracker {

    private Object leader;
    private int leadChanges;

    /** Called when a new match begins, alongside the score reset it mirrors. */
    public void reset() {
        this.leader = null;
        this.leadChanges = 0;
    }

    /**
     * @param soleLeader identity of the unique highest scorer, or null if the top score is shared
     */
    public void onStandingsChanged(Object soleLeader) {
        if (soleLeader == null) {
            // Tie. The incumbent keeps the lead, so the eventual winner of the tie is one change
            // from here, not two.
            return;
        }
        if (this.leader == null) {
            // First leader of the match. Taking the lead from nobody isn't a change.
            this.leader = soleLeader;
            return;
        }
        if (!this.leader.equals(soleLeader)) {
            this.leader = soleLeader;
            this.leadChanges++;
        }
    }

    public int leadChanges() {
        return this.leadChanges;
    }
}
