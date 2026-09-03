package forceitembattle.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.collection.CollectionManager;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The rules that fire when the round ends, and the Completionist tiers.
 *
 * <p>None of this had a test. The manager built its own {@link AchievementStorage}, which built its
 * own HTTP client, so constructing one outside a running server was impossible — and the five
 * end-of-round rules live <em>outside</em> the handler table that {@code EventHandlersTest} covers,
 * so nothing reached them by another route either.
 *
 * <p>Two things keep this headless. The players are offline, so {@code writeUnlock} records the
 * unlock but skips the announcement it would otherwise push through {@code Bukkit}; and the round
 * events are mocked, which is enough because every handler below dispatches on {@code instanceof}.
 * Progress is driven through {@code handleEvent} rather than seeded, so the tests exercise the same
 * path the round does.
 */
class AchievementManagerTest {

    /** The five unlocks {@code checkGameEndAchievements} can produce, in the order it tries them. */
    private static final List<Achievements> END_OF_ROUND = List.of(
            Achievements.CHICOT,
            Achievements.THE_HARD_WAY,
            Achievements.NO_HANDOUTS,
            Achievements.NO_SHORTCUTS,
            Achievements.IT_IS_BEAUTIFUL);

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private RecordingAchievementSink sink;
    private AchievementStorage storage;
    private Roster roster;
    private RoundPhase phase;
    private GameSettings settings;
    private AchievementManager manager;

    @BeforeEach
    void setUp() {
        sink = new RecordingAchievementSink();
        storage = new AchievementStorage(sink);
        roster = new Roster();
        phase = new RoundPhase();
        phase.moveTo(GameState.MID_GAME);

        settings = mock(GameSettings.class);
        when(settings.isSettingEnabled(GameSetting.ACHIEVEMENTS)).thenReturn(true);

        // The stats loader is only reached by evaluateGlobalAchievements, which these tests do not
        // exercise: it needs a service to answer.
        manager = new AchievementManager(roster, phase, settings, mock(CollectionManager.class),
                storage, new GlobalStatsCache(), null, new FakeAchievementWorld());
    }

    /** Adds a participant with the given score to the roster and returns them. */
    private ForceItemPlayer join(String seed, int score) {
        ForceItemPlayer player =
                new ForceItemPlayer(Finds.mockPlayer(seed), Material.DIRT, 0, score);
        roster.add(player.player().getUniqueId(), player);
        return player;
    }

    private ForceItemPlayer join(String seed) {
        return join(seed, 0);
    }

    private static UUID uuidOf(ForceItemPlayer player) {
        return player.player().getUniqueId();
    }

    /** Ends the round, the way the game manager does before asking for the end-of-round unlocks. */
    private void endRound() {
        phase.moveTo(GameState.END_GAME);
        manager.checkGameEndAchievements();
    }

    @Nested
    @DisplayName("the end of a round")
    class GameEnd {

        /**
         * A clean solo round: no deaths, no back-to-back, no teleporter, never left the Overworld,
         * and first place because they are the only one playing.
         */
        @Test
        void aFlawlessSoloRoundUnlocksAllFive() {
            ForceItemPlayer alice = join("a");

            endRound();

            assertEquals(END_OF_ROUND, sink.granted(uuidOf(alice)));
        }

        /** Each of the five is "you did not do X", so one death costs exactly one of them. */
        @Test
        void aDeathCostsOnlyChicot() {
            ForceItemPlayer alice = join("a");
            manager.handleEvent(alice.player(), mock(PlayerDeathEvent.class), Trigger.DYING);

            endRound();

            List<Achievements> granted = sink.granted(uuidOf(alice));
            assertFalse(granted.contains(Achievements.CHICOT));
            assertTrue(granted.contains(Achievements.THE_HARD_WAY));
        }

        /** THE_HARD_WAY and NO_HANDOUTS read the same occurrence, so a back-to-back costs both. */
        @Test
        void aBackToBackCostsBothStreakFreeUnlocks() {
            ForceItemPlayer alice = join("a");
            manager.handleEvent(alice.player(), Finds.backToBack(alice, Material.STONE),
                    Trigger.BACK_TO_BACK);

            endRound();

            List<Achievements> granted = sink.granted(uuidOf(alice));
            assertFalse(granted.contains(Achievements.THE_HARD_WAY));
            assertFalse(granted.contains(Achievements.NO_HANDOUTS));
            assertTrue(granted.contains(Achievements.NO_SHORTCUTS));
        }

        @Test
        void enteringTheTeleporterCostsNoShortcuts() {
            ForceItemPlayer alice = join("a");
            manager.handleEvent(alice.player(), mock(AntimatterTeleporterUseEvent.class),
                    Trigger.ANTIMATTER_TELEPORTER);

            endRound();

            List<Achievements> granted = sink.granted(uuidOf(alice));
            assertFalse(granted.contains(Achievements.NO_SHORTCUTS));
            assertTrue(granted.contains(Achievements.IT_IS_BEAUTIFUL));
        }

        /** NO_HANDOUTS is the only one of the five that also has to be won. */
        @Test
        void aLoserKeepsEverythingButNoHandouts() {
            ForceItemPlayer alice = join("a", 0);
            ForceItemPlayer bob = join("b", 10);

            endRound();

            assertFalse(sink.granted(uuidOf(alice)).contains(Achievements.NO_HANDOUTS));
            assertTrue(sink.granted(uuidOf(alice)).contains(Achievements.CHICOT));
            assertTrue(sink.granted(uuidOf(bob)).contains(Achievements.NO_HANDOUTS));
        }

        /** Ties for first count as a win, so a drawn round hands NO_HANDOUTS to both. */
        @Test
        void aTiedRoundIsAWinForBoth() {
            ForceItemPlayer alice = join("a", 5);
            ForceItemPlayer bob = join("b", 5);

            endRound();

            assertTrue(sink.granted(uuidOf(alice)).contains(Achievements.NO_HANDOUTS));
            assertTrue(sink.granted(uuidOf(bob)).contains(Achievements.NO_HANDOUTS));
        }

        /** A team unlock is recorded as TEAM, naming the other member. */
        @Test
        void aTeamRoundRecordsTheTeammate() {
            ForceItemPlayer alice = join("a");
            ForceItemPlayer bob = join("b");
            Team team = new Team(1, Material.STONE, 0, 0, alice, bob);
            alice.setCurrentTeam(team);
            bob.setCurrentTeam(team);

            endRound();

            RecordingAchievementSink.Unlock first = sink.unlocks.getFirst();
            assertEquals(uuidOf(alice), first.player());
            assertEquals(AchievementMode.TEAM, first.mode());
            assertEquals(uuidOf(bob), first.teammate());
        }

        /** A spectator is not in the round and did not finish it. */
        @Test
        void aSpectatorUnlocksNothing() {
            ForceItemPlayer watcher = join("a");
            watcher.setSpectator(true);

            endRound();

            assertTrue(sink.unlocks.isEmpty());
        }

        /** The gate is the phase, not the call site. */
        @Test
        void nothingUnlocksBeforeTheRoundHasEnded() {
            join("a");

            manager.checkGameEndAchievements();

            assertTrue(sink.unlocks.isEmpty());
        }

        @Test
        void nothingUnlocksWithAchievementsOff() {
            join("a");
            when(settings.isSettingEnabled(GameSetting.ACHIEVEMENTS)).thenReturn(false);

            endRound();

            assertTrue(sink.unlocks.isEmpty());
        }

        /** Re-running the end of a round must not re-announce what the player already holds. */
        @Test
        void anAlreadyHeldUnlockIsNotSentTwice() {
            ForceItemPlayer alice = join("a");
            storage.addAchievement(uuidOf(alice), Achievements.CHICOT);
            sink.unlocks.clear();

            endRound();

            assertFalse(sink.granted(uuidOf(alice)).contains(Achievements.CHICOT));
        }

        /**
         * Recorded, not endorsed: the end-of-round unlocks do not run the Completionist check, where
         * every other grant path does. In a real round the gap closes a moment later, because
         * {@code evaluateGlobalAchievements} runs next and ends with one — but only for a player who
         * is online with their stats loaded.
         */
        @Test
        void theEndOfRoundUnlocksDoNotCheckTheTiers() {
            ForceItemPlayer alice = join("a");
            grantAllBut(uuidOf(alice), Achievements.CHICOT, AchievementScope.ROUND);
            sink.unlocks.clear();

            endRound();

            assertFalse(sink.granted(uuidOf(alice)).contains(Achievements.COMPLETIONIST));
        }
    }

    @Nested
    @DisplayName("the Completionist tiers")
    class MetaTiers {

        /**
         * One pass is enough because {@link CompletionistRule} cannot require a META achievement, so
         * granting one here can never satisfy another. This replaced a {@code while (grantedAny)}
         * fixpoint whose second pass could only ever grant nothing.
         */
        @Test
        void aTierCannotRequireAnotherTier() {
            for (Achievements achievement : Achievements.values()) {
                if (achievement.getScope() != AchievementScope.META) {
                    continue;
                }
                assertFalse(achievement.getCompletionistRule().requiredScopes()
                                .contains(AchievementScope.META),
                        achievement + " requires META, which would need a fixpoint again");
            }
        }

        /** Holding every ROUND achievement is what Completionist+ is. */
        @Test
        void everyRoundAchievementUnlocksTheFirstTier() {
            grantAll(ALICE, AchievementScope.ROUND);
            sink.unlocks.clear();

            manager.checkMetaTiers(ALICE, null);

            assertEquals(List.of(Achievements.COMPLETIONIST), sink.granted(ALICE));
        }

        /** The second tier wants the other two scopes as well, and both are granted in one pass. */
        @Test
        void everyScopeUnlocksBothTiers() {
            grantAll(ALICE, AchievementScope.ROUND, AchievementScope.GLOBAL, AchievementScope.COLLECTION);
            sink.unlocks.clear();

            manager.checkMetaTiers(ALICE, null);

            assertEquals(List.of(Achievements.COMPLETIONIST, Achievements.COMPLETIONIST_PLUS_PLUS),
                    sink.granted(ALICE));
        }

        /** One missing achievement is enough to withhold a tier. */
        @Test
        void oneShortIsNotComplete() {
            grantAllBut(ALICE, Achievements.CHICOT, AchievementScope.ROUND);
            sink.unlocks.clear();

            manager.checkMetaTiers(ALICE, null);

            assertTrue(sink.unlocks.isEmpty());
        }

        /** A tier already held is not re-granted, so it is not re-announced either. */
        @Test
        void aHeldTierIsNotGrantedAgain() {
            grantAll(ALICE, AchievementScope.ROUND);
            manager.checkMetaTiers(ALICE, null);
            sink.unlocks.clear();

            manager.checkMetaTiers(ALICE, null);

            assertTrue(sink.unlocks.isEmpty());
        }
    }

    private void grantAll(UUID player, AchievementScope... scopes) {
        grantAllBut(player, null, scopes);
    }

    private void grantAllBut(UUID player, Achievements withhold, AchievementScope... scopes) {
        List<AchievementScope> wanted = Arrays.asList(scopes);
        for (Achievements achievement : Achievements.values()) {
            if (!wanted.contains(achievement.getScope()) || achievement == withhold) {
                continue;
            }
            storage.addAchievement(player, achievement);
        }
    }
}
