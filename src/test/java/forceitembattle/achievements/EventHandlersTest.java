package forceitembattle.achievements;

import static forceitembattle.achievements.Finds.mockPlayer;
import static forceitembattle.achievements.Finds.participant;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.achievements.handlers.AntimatterTeleporterUsesAchievementHandler;
import forceitembattle.achievements.handlers.DeathCounterAchievementHandler;
import forceitembattle.achievements.handlers.NoAntimatterAchievementHandler;
import forceitembattle.achievements.handlers.RareMobDropAchievementHandler;
import forceitembattle.achievements.handlers.TradingAchievementHandler;
import forceitembattle.achievements.handlers.WheelOfFortuneAchievementHandler;
import forceitembattle.achievements.handlers.WheelOfFortuneUsesAchievementHandler;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The achievement rules driven by something other than a find.
 *
 * <p>Bukkit's own events have awkward constructors, so they are mocked — which works because a
 * handler only ever calls two or three getters on one. That is itself a consequence of the seam: a
 * rule that reaches through a plugin needs the plugin to be real, but a rule handed an event and a
 * narrow world needs neither.
 */
class EventHandlersTest {

    /**
     * Built before any {@code when(...)} that consumes it — Mockito rejects a mock created inside a
     * stubbing argument, which is what "unfinished stubbing" means when it appears three tests later.
     */
    private static ItemStack stack(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }

    @Nested
    class WheelOfFortune {

        /** The win only counts if the wheel handed over the item this player is hunting. */
        @Test
        void winningYourOwnForceItemCounts() {
            ForceItemPlayer alice = participant("a");
            alice.scoreOwner().startRound(Material.DIAMOND, Material.STONE, 0L);
            WheelOfFortuneAchievementHandler handler = new WheelOfFortuneAchievementHandler();

            assertTrue(handler.check(new WheelOfFortuneWinEvent(alice.player(), Material.DIAMOND),
                    handler.createProgress(), alice, new FakeAchievementWorld()));
        }

        @Test
        void winningSomethingElseDoesNot() {
            ForceItemPlayer alice = participant("a");
            alice.scoreOwner().startRound(Material.DIAMOND, Material.STONE, 0L);
            WheelOfFortuneAchievementHandler handler = new WheelOfFortuneAchievementHandler();

            assertFalse(handler.check(new WheelOfFortuneWinEvent(alice.player(), Material.DIRT),
                    handler.createProgress(), alice, new FakeAchievementWorld()));
        }

        /** The uses counter does not care what was won, only that the wheel was spun. */
        @Test
        void theUsesCounterCountsEverySpin() {
            ForceItemPlayer alice = participant("a");
            WheelOfFortuneUsesAchievementHandler handler = new WheelOfFortuneUsesAchievementHandler(2);
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(new WheelOfFortuneWinEvent(alice.player(), Material.DIRT),
                    progress, alice, world));
            assertTrue(handler.check(new WheelOfFortuneWinEvent(alice.player(), Material.STONE),
                    progress, alice, world));
        }
    }

    @Nested
    class AntimatterTeleporter {

        /** Only a teleporter the player has not used before counts toward the total. */
        @Test
        void onlyANewTeleporterCounts() {
            ForceItemPlayer alice = participant("a");
            AntimatterTeleporterUsesAchievementHandler handler =
                    new AntimatterTeleporterUsesAchievementHandler(1);
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(new AntimatterTeleporterUseEvent(alice.player(), false),
                    handler.createProgress(), alice, world));
            assertTrue(handler.check(new AntimatterTeleporterUseEvent(alice.player(), true),
                    handler.createProgress(), alice, world));
        }

        /** The "never used one" achievement counts every use, new or not, and grants nothing. */
        @Test
        void theAbstinenceCounterCountsEveryUseAndNeverGrants() {
            ForceItemPlayer alice = participant("a");
            NoAntimatterAchievementHandler handler = new NoAntimatterAchievementHandler();
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(new AntimatterTeleporterUseEvent(alice.player(), false),
                    progress, alice, world));
            assertTrue(progress.count >= 1, "the tally is what the game-end pass reads");
        }
    }

    @Nested
    class RareMobDrops {

        @Test
        void aWitherSkeletonThatDroppedItsSkullCounts() {
            ForceItemPlayer alice = participant("a");
            EntityDeathEvent event = mock(EntityDeathEvent.class);
            when(event.getEntityType()).thenReturn(EntityType.WITHER_SKELETON);
            List<ItemStack> drops = List.of(stack(Material.WITHER_SKELETON_SKULL));
            when(event.getDrops()).thenReturn(drops);

            RareMobDropAchievementHandler handler = new RareMobDropAchievementHandler(1);
            assertTrue(handler.check(event, handler.createProgress(), alice, new FakeAchievementWorld()));
        }

        /** Killing the right mob is not enough — it has to have rolled the drop. */
        @Test
        void aWitherSkeletonWithNoSkullDoesNot() {
            ForceItemPlayer alice = participant("a");
            EntityDeathEvent event = mock(EntityDeathEvent.class);
            when(event.getEntityType()).thenReturn(EntityType.WITHER_SKELETON);
            List<ItemStack> drops = List.of(stack(Material.BONE));
            when(event.getDrops()).thenReturn(drops);

            RareMobDropAchievementHandler handler = new RareMobDropAchievementHandler(1);
            assertFalse(handler.check(event, handler.createProgress(), alice, new FakeAchievementWorld()));
        }

        @Test
        void aDrownedThatDroppedATridentCounts() {
            ForceItemPlayer alice = participant("a");
            EntityDeathEvent event = mock(EntityDeathEvent.class);
            when(event.getEntityType()).thenReturn(EntityType.DROWNED);
            List<ItemStack> drops = List.of(stack(Material.TRIDENT));
            when(event.getDrops()).thenReturn(drops);

            RareMobDropAchievementHandler handler = new RareMobDropAchievementHandler(1);
            assertTrue(handler.check(event, handler.createProgress(), alice, new FakeAchievementWorld()));
        }

        /** Any other mob is ignored before its drops are even looked at. */
        @Test
        void anyOtherMobIsIgnored() {
            ForceItemPlayer alice = participant("a");
            EntityDeathEvent event = mock(EntityDeathEvent.class);
            when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);

            RareMobDropAchievementHandler handler = new RareMobDropAchievementHandler(1);
            assertFalse(handler.check(event, handler.createProgress(), alice, new FakeAchievementWorld()));
        }
    }

    @Nested
    class Deaths {

        @Test
        void deathsAccumulateAndNeverGrantMidRound() {
            ForceItemPlayer alice = participant("a");
            DeathCounterAchievementHandler handler = new DeathCounterAchievementHandler(0);
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(mock(PlayerDeathEvent.class), progress, alice, world));
            assertFalse(handler.check(mock(PlayerDeathEvent.class), progress, alice, world));
            assertTrue(progress.deathCount >= 2,
                    "CHICOT reads this at game end; the handler itself never grants");
        }
    }

    @Nested
    class Trading {

        /** A purchase only counts against one of the round's own wandering traders. */
        @Test
        void aPurchaseFromARoundTraderCounts() {
            ForceItemPlayer alice = participant("a");
            Player player = alice.player();
            PlayerPurchaseEvent event = mock(PlayerPurchaseEvent.class);
            when(event.getPlayer()).thenReturn(player);

            TradingAchievementHandler handler = new TradingAchievementHandler(1);
            FakeAchievementWorld world = new FakeAchievementWorld().trading(player.getUniqueId());

            assertTrue(handler.check(event, handler.createProgress(), alice, world));
        }

        @Test
        void aPurchaseFromAnOrdinaryVillagerDoesNot() {
            ForceItemPlayer alice = participant("a");
            PlayerPurchaseEvent event = mock(PlayerPurchaseEvent.class);
            when(event.getPlayer()).thenReturn(alice.player());

            TradingAchievementHandler handler = new TradingAchievementHandler(1);

            assertFalse(handler.check(event, handler.createProgress(), alice, new FakeAchievementWorld()),
                    "the world says nobody is mid-trade with a round trader");
        }
    }

    /**
     * Every handler ignores an event it was not written for. That guard is 22 hand-written
     * {@code instanceof} checks — the follow-up this pass deliberately did not take, since typing
     * the event on the handler would make a wrong pairing a compile error instead.
     */
    @Test
    void aHandlerIgnoresAnEventItWasNotWrittenFor() {
        ForceItemPlayer alice = participant("a");
        FakeAchievementWorld world = new FakeAchievementWorld();
        WheelOfFortuneWinEvent wrongEvent = new WheelOfFortuneWinEvent(mockPlayer("b"), Material.DIRT);

        RareMobDropAchievementHandler rareDrop = new RareMobDropAchievementHandler(1);
        TradingAchievementHandler trading = new TradingAchievementHandler(1);

        assertFalse(rareDrop.check(wrongEvent, rareDrop.createProgress(), alice, world));
        assertFalse(trading.check(wrongEvent, trading.createProgress(), alice, world));
    }
}
