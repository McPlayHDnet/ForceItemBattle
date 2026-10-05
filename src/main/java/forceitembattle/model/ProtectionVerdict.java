package forceitembattle.model;

public enum ProtectionVerdict {

    ALLOWED,

    /** Too close to another player's respawn point. */
    NEAR_BED,

    /** The container belongs to someone who is not a teammate — or to nobody breakable at all. */
    CONTAINER_OWNED;

    public boolean denied() {
        return this != ALLOWED;
    }
}
