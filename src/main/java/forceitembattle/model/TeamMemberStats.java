package forceitembattle.model;

/** One member's contribution to a duo's totals, as {@code /stats duo} lists it. */
public record TeamMemberStats(PlayerIdentity member, long totalItemsFound, long deaths,
                              long blocksTravelled) {
}
