package forceitembattle.model;

/**
 * Whether an action on a block is allowed, and if not, which rule stopped it.
 *
 * <p>The reason travels back to the caller because the listener needs it to say what happened —
 * breaking near a bed and breaking someone's chest are refused for different reasons and read as
 * different messages. What the reason is remains the protection rules' business; how it is worded
 * is the listener's.
 */
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
