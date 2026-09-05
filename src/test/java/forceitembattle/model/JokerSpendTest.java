package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What a skip costs, and what the button should read afterwards.
 *
 * <p><b>Headless.</b> Nothing here builds an {@code ItemStack}, which is the point: this arithmetic
 * used to live inside a click handler, behind the {@code ItemStack} wall
 * {@code HeadlessBoundaryTest} documents, and had no test at all.
 *
 * <p>The rule it hides is easy to state and easy to get backwards: <b>in a team game the pool is
 * shared, so a member's stack loses only the one they spent; solo, the stack size <em>is</em> the
 * remaining count.</b> Reversed, a team member's button shows the whole team's pool, or a solo
 * player's skips quietly multiply.
 */
class JokerSpendTest {

    /** A roster entry over a mocked player — nothing here touches the player at all. */
    private static ForceItemPlayer solo(int jokers) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return new ForceItemPlayer(player, Material.DIAMOND, jokers, 0);
    }

    /** Two players sharing one Score Owner, so the pool is the team's. */
    private static ForceItemPlayer inATeamOf(int pool) {
        ForceItemPlayer first = solo(0);
        ForceItemPlayer second = solo(0);
        Team team = new Team(1, Material.DIAMOND, 0, pool, first, second);
        first.setCurrentTeam(team);
        second.setCurrentTeam(team);
        return first;
    }

    @Nested
    class TheStackArithmetic {

        @Test
        @DisplayName("solo: the stack becomes the remaining count")
        void soloStackBecomesTheRemainingCount() {
            ForceItemPlayer player = solo(3);

            JokerSpend.Spent spent = assertInstanceOf(JokerSpend.Spent.class,
                    JokerSpend.spend(player, OptionalInt.of(3)));

            assertEquals(2, spent.stackAmount());
            assertEquals(2, player.activeJokers(), "and the pool agrees with the button");
        }

        /**
         * The half that would be wrong if the branch were reversed: the team's pool is 6 but this
         * member is only holding 3 of them, and spending one must leave them holding 2 — not 5.
         */
        @Test
        @DisplayName("team: the stack loses one, it does not become the pool")
        void teamStackLosesExactlyOne() {
            ForceItemPlayer member = inATeamOf(6);

            JokerSpend.Spent spent = assertInstanceOf(JokerSpend.Spent.class,
                    JokerSpend.spend(member, OptionalInt.of(3)));

            assertEquals(2, spent.stackAmount(), "their share, minus the one they spent");
            assertEquals(5, member.activeJokers(), "the team pool drops by one");
        }

        @Test
        void theLastSoloJokerLeavesAnEmptyStack() {
            ForceItemPlayer player = solo(1);

            JokerSpend.Spent spent = assertInstanceOf(JokerSpend.Spent.class,
                    JokerSpend.spend(player, OptionalInt.of(1)));

            assertEquals(0, spent.stackAmount(), "zero means remove the button");
        }

        /** A team member holding their last one, while the team still has more. */
        @Test
        void aTeamMembersLastShareEmptiesTheirStackButNotThePool() {
            ForceItemPlayer member = inATeamOf(4);

            JokerSpend.Spent spent = assertInstanceOf(JokerSpend.Spent.class,
                    JokerSpend.spend(member, OptionalInt.of(1)));

            assertEquals(0, spent.stackAmount());
            assertEquals(3, member.activeJokers(), "the team keeps the rest of the pool");
        }
    }

    @Nested
    class WhatIsHandedOver {

        @Test
        void theItemTheyWereHunting() {
            ForceItemPlayer player = solo(2);

            JokerSpend.Spent spent = assertInstanceOf(JokerSpend.Spent.class,
                    JokerSpend.spend(player, OptionalInt.of(2)));

            assertEquals(Material.DIAMOND, spent.handedOver());
        }
    }

    @Nested
    class Refusals {

        @Test
        void anEmptyPoolIsExhausted() {
            ForceItemPlayer player = solo(0);

            assertInstanceOf(JokerSpend.Exhausted.class,
                    JokerSpend.spend(player, OptionalInt.of(1)));
        }

        @Test
        void andChargesNothing() {
            ForceItemPlayer player = solo(0);

            JokerSpend.spend(player, OptionalInt.of(1));

            assertEquals(0, player.activeJokers());
        }

        @Test
        void notHoldingOneIsNoStackInHand() {
            ForceItemPlayer player = solo(3);

            assertInstanceOf(JokerSpend.NoStackInHand.class,
                    JokerSpend.spend(player, OptionalInt.empty()));
        }

        @Test
        void andThatChargesNothingEither() {
            ForceItemPlayer player = solo(3);

            JokerSpend.spend(player, OptionalInt.empty());

            assertEquals(3, player.activeJokers(), "a refusal must never cost a joker");
        }

        /**
         * The ordering, which used to be an implicit consequence of statement order in a click
         * handler: an empty pool beats an absent stack, so someone with neither is told they are out
         * of skips and has the dead button stripped, rather than being ignored.
         */
        @Test
        @DisplayName("no jokers and no stack: exhausted wins, so the dead button is stripped")
        void anEmptyPoolBeatsAnAbsentStack() {
            ForceItemPlayer player = solo(0);

            assertInstanceOf(JokerSpend.Exhausted.class,
                    JokerSpend.spend(player, OptionalInt.empty()));
        }
    }

    /** What a carried vote costs its initiator: the charge, and nothing handed over. */
    @Nested
    class ChargingForAVote {

        @Test
        void chargesThePoolAndReportsTheNewStack() {
            ForceItemPlayer player = solo(3);

            int stackAmount = JokerSpend.charge(player, OptionalInt.of(3));

            assertEquals(2, stackAmount);
            assertEquals(2, player.activeJokers());
        }

        @Test
        void aTeamInitiatorsStackLosesOne() {
            ForceItemPlayer member = inATeamOf(6);

            int stackAmount = JokerSpend.charge(member, OptionalInt.of(3));

            assertEquals(2, stackAmount);
            assertEquals(5, member.activeJokers());
        }

        /**
         * <b>Recorded, not endorsed.</b> {@code spendJoker} floors at zero, so a vote by someone with
         * no jokers left costs nothing and still succeeds — a vote anyone can spam. Refusing it is a
         * {@code /voteskip} rule and a product decision, not this module's.
         */
        @Test
        void anExhaustedInitiatorIsNotRefused() {
            ForceItemPlayer player = solo(0);

            int stackAmount = JokerSpend.charge(player, OptionalInt.empty());

            assertEquals(0, stackAmount);
            assertEquals(0, player.activeJokers());
        }
    }
}
