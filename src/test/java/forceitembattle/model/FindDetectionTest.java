package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Whether an item that passed through someone's hands is a find.
 *
 * <p>Real {@link ItemStack}s, so the backpack carve-out runs against the same persistent data the
 * game writes rather than a stub that agrees with itself.
 */
class FindDetectionTest {

    private ServerMock server;
    private Roster roster;
    private RoundPhase phase;
    private FindDetection detection;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roster = new Roster();
        this.phase = new RoundPhase();
        this.phase.moveTo(GameState.MID_GAME);
        this.detection = new FindDetection(this.roster, this.phase);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A player on the roster, hunting {@code hunting}. */
    private PlayerMock join(String name, Material hunting) {
        PlayerMock player = this.server.addPlayer(name);
        this.roster.add(player.getUniqueId(), new ForceItemPlayer(player, hunting, 0, 0));
        return player;
    }

    /** Somebody connected but never added to the roster: they joined after it froze. */
    private PlayerMock lateJoiner(String name) {
        return this.server.addPlayer(name);
    }

    private boolean detects(PlayerMock player, ItemStack stack) {
        return this.detection.detect(player, stack).isPresent();
    }

    @Nested
    @DisplayName("the item")
    class TheItem {

        @Test
        void theHuntedItemIsAFind() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);

            assertTrue(detects(alice, new ItemStack(Material.DIAMOND)));
        }

        @Test
        void anythingElseIsNot() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);

            assertFalse(detects(alice, new ItemStack(Material.DIRT)));
        }

        /** Every one of the eight events permits a null stack — an empty-handed right-click, say. */
        @Test
        void nothingInHandIsNotAFind() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);

            assertFalse(detects(alice, null));
        }

        /**
         * The backpack is a bundle. Without the carve-out, a round whose force item is a bundle is
         * completed by opening the one every player is handed at the start.
         */
        @Test
        void theBackpackIsNeverAFindEvenWhenItMatches() {
            PlayerMock alice = join("Understudy1", Material.BUNDLE);
            ItemStack backpack = GameItems.backpack(roster.get(alice.getUniqueId()), false);

            assertEquals(Material.BUNDLE, backpack.getType(), "the fixture must be the hunted type");
            assertFalse(detects(alice, backpack));
        }

        /** An ordinary bundle of the same material still counts — only the marked one is excluded. */
        @Test
        void aPlainBundleOfTheSameTypeStillCounts() {
            PlayerMock alice = join("Understudy1", Material.BUNDLE);

            assertTrue(detects(alice, new ItemStack(Material.BUNDLE)));
        }

        /** The finder handed back is the one whose item it was, not merely someone playing. */
        @Test
        void theFinderIsTheOneHuntingIt() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);

            assertEquals(alice.getUniqueId(),
                    detection.detect(alice, new ItemStack(Material.DIAMOND))
                            .orElseThrow().player().getUniqueId());
        }
    }

    @Nested
    @DisplayName("who is playing")
    class WhoIsPlaying {

        /**
         * <b>The behaviour this module changed.</b> The copies asked {@code roster.get}, which
         * returns a spectator's entry — and a spectator keeps the force item they were hunting when
         * they stopped playing, so they went on scoring by picking things up.
         */
        @Test
        void aSpectatorScoresNothing() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);
            roster.get(alice.getUniqueId()).setSpectator(true);

            assertFalse(detects(alice, new ItemStack(Material.DIAMOND)));
        }

        /** Someone who connected after the roster froze has no item to match against. */
        @Test
        void aMidRoundJoinerScoresNothing() {
            PlayerMock stranger = lateJoiner("Understudy2");

            assertFalse(detects(stranger, new ItemStack(Material.DIAMOND)));
        }
    }

    @Nested
    @DisplayName("the phase")
    class Phase {

        @Test
        void nothingIsFoundBeforeTheRoundStarts() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);
            phase.moveTo(GameState.PRE_GAME);

            assertFalse(detects(alice, new ItemStack(Material.DIAMOND)));
        }

        /** A pause stops play, so a find during one does not count. */
        @Test
        void nothingIsFoundWhilePaused() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);
            phase.moveTo(GameState.PAUSED_GAME);

            assertFalse(detects(alice, new ItemStack(Material.DIAMOND)));
        }

        @Test
        void nothingIsFoundAfterTheRoundEnds() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);
            phase.moveTo(GameState.END_GAME);

            assertFalse(detects(alice, new ItemStack(Material.DIAMOND)));
        }

        /**
         * The countdown is not play. Force items are already assigned by then, so without this gate
         * the round could be scored before it starts.
         */
        @Test
        void nothingIsFoundDuringTheCountdown() {
            PlayerMock alice = join("Understudy1", Material.DIAMOND);
            phase.moveTo(GameState.STARTING);

            assertFalse(detects(alice, new ItemStack(Material.DIAMOND)));
        }
    }

    /** A team's item is the team's, so either member finding it is a find for the owner. */
    @Nested
    @DisplayName("on a team")
    class OnATeam {

        @Test
        void eitherMemberCanFindTheTeamsItem() {
            PlayerMock alice = join("Understudy1", Material.DIRT);
            PlayerMock bob = join("Understudy2", Material.DIRT);
            ForceItemPlayer aliceEntry = roster.get(alice.getUniqueId());
            ForceItemPlayer bobEntry = roster.get(bob.getUniqueId());
            Team team = new Team(1, Material.DIAMOND, 0, 0, aliceEntry, bobEntry);
            aliceEntry.setCurrentTeam(team);
            bobEntry.setCurrentTeam(team);

            assertTrue(detects(alice, new ItemStack(Material.DIAMOND)));
            assertTrue(detects(bob, new ItemStack(Material.DIAMOND)));
        }

        /** And their own plain material is not it — the team's item is what is hunted. */
        @Test
        void theirOwnItemIsNotTheTeamsItem() {
            PlayerMock alice = join("Understudy1", Material.DIRT);
            ForceItemPlayer aliceEntry = roster.get(alice.getUniqueId());
            Team team = new Team(1, Material.DIAMOND, 0, 0, aliceEntry);
            aliceEntry.setCurrentTeam(team);

            assertFalse(detects(alice, new ItemStack(Material.DIRT)));
        }

        private UUID unused() {
            return UUID.randomUUID();
        }
    }
}
