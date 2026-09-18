package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.CustomMaterials;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.VillagerMock;

/**
 * The two custom villager offers: the cleric's Eye of Antimatter and the cartographer's Sulfur
 * Locator.
 *
 * <p><b>This module had no test file at all</b>, and could not have had one: every path in it —
 * all three handlers — hands its work to {@code Scheduler.runLaterSync(…, 1L)}, one tick later, so
 * that vanilla has finished writing the offer list before it is replaced. With the scheduler
 * undrivable from a test, calling a handler did nothing observable and there was nothing to assert.
 * {@link ListenerTestBase#tick} is the whole reason this file exists.
 *
 * <p>The cartographer roll is a 30% draw from {@code ThreadLocalRandom}, which a test cannot pin
 * without owning the source of randomness. So {@link Cartographer} asserts the invariants that hold
 * <em>either way</em> — chiefly that the villager is marked as decided win or lose, which is what
 * stops it being re-rolled on every chunk load until it wins.
 */
class VillagerTradeListenerTest extends ListenerTestBase {

    private VillagerTradeListener listener;
    private Plugin plugin;

    @BeforeEach
    void setUpListener() {
        this.plugin = MockBukkit.createMockPlugin("fib");
        this.listener = new VillagerTradeListener(this.plugin);
    }

    /** The marker the cartographer roll writes, namespaced under the plugin the listener holds. */
    private NamespacedKey rolledKey() {
        return new NamespacedKey(this.plugin, "cartographer_trade_rolled");
    }

    private VillagerMock villager(Villager.Profession profession, int level) {
        VillagerMock villager = new VillagerMock(this.server, java.util.UUID.randomUUID());
        villager.setProfession(profession);
        villager.setVillagerLevel(level);
        return villager;
    }

    /** One ordinary offer, so the list is not empty — an empty list means "not generated yet". */
    private static MerchantRecipe someVanillaOffer() {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.MAP), 4);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 1));
        return recipe;
    }

    private static void giveVanillaOffers(VillagerMock villager) {
        List<MerchantRecipe> offers = new ArrayList<>();
        offers.add(someVanillaOffer());
        villager.setRecipes(offers);
    }

    private static boolean offersEyeOfAntimatter(Villager villager) {
        return villager.getRecipes().stream()
                .anyMatch(recipe -> CustomMaterials.EYE_OF_ANTIMATTER.matches(recipe.getResult()));
    }

    @Nested
    class Cleric {

        private void acquireTrade(VillagerMock villager) {
            listener.onAcquireTrade(new VillagerAcquireTradeEvent(villager, someVanillaOffer()));
        }

        @Test
        @DisplayName("an apprentice cleric gains the Eye of Antimatter offer")
        void anApprenticeClericGainsTheOffer() {
            VillagerMock cleric = villager(Villager.Profession.CLERIC, 2);
            giveVanillaOffers(cleric);

            acquireTrade(cleric);
            tick(2L);

            assertTrue(offersEyeOfAntimatter(cleric));
        }

        /** Last in the list, so it reads as the offer the new level just unlocked. */
        @Test
        void andItIsTheLastOffer() {
            VillagerMock cleric = villager(Villager.Profession.CLERIC, 2);
            giveVanillaOffers(cleric);

            acquireTrade(cleric);
            tick(2L);

            List<MerchantRecipe> offers = cleric.getRecipes();
            assertTrue(CustomMaterials.EYE_OF_ANTIMATTER.matches(offers.getLast().getResult()));
        }

        @Test
        void aNoviceClericDoesNot() {
            VillagerMock cleric = villager(Villager.Profession.CLERIC, 1);
            giveVanillaOffers(cleric);

            acquireTrade(cleric);
            tick(2L);

            assertFalse(offersEyeOfAntimatter(cleric), "the offer starts at apprentice");
        }

        /**
         * Whether the offer is already present is read off the offer list rather than a marker, so
         * running twice must not add it twice. A cured or re-professioned villager regenerates its
         * trades, which is why a marker would be wrong here.
         */
        @Test
        void runningTwiceDoesNotDuplicateTheOffer() {
            VillagerMock cleric = villager(Villager.Profession.CLERIC, 2);
            giveVanillaOffers(cleric);

            acquireTrade(cleric);
            tick(2L);
            acquireTrade(cleric);
            tick(2L);

            long count = cleric.getRecipes().stream()
                    .filter(recipe -> CustomMaterials.EYE_OF_ANTIMATTER.matches(recipe.getResult()))
                    .count();
            assertEquals(1, count);
        }

        @Test
        void anotherProfessionIsIgnored() {
            VillagerMock farmer = villager(Villager.Profession.FARMER, 5);
            giveVanillaOffers(farmer);

            acquireTrade(farmer);
            tick(2L);

            assertFalse(offersEyeOfAntimatter(farmer));
        }

        /** The handler schedules; nothing has happened when it returns. */
        @Test
        void nothingHappensUntilTheNextTick() {
            VillagerMock cleric = villager(Villager.Profession.CLERIC, 2);
            giveVanillaOffers(cleric);

            acquireTrade(cleric);

            assertFalse(offersEyeOfAntimatter(cleric),
                    "the offer is appended a tick later, after vanilla has written its list");
        }
    }

    @Nested
    class Cartographer {

        private void changeCareer(VillagerMock villager) {
            listener.onCareerChange(new VillagerCareerChangeEvent(
                    villager, villager.getProfession(), VillagerCareerChangeEvent.ChangeReason.EMPLOYED));
        }

        private boolean rolled(VillagerMock villager) {
            return villager.getPersistentDataContainer().has(rolledKey(), PersistentDataType.BYTE);
        }

        /**
         * The draw is 30%, so whether the offer appears is not assertable — but that the villager is
         * <em>marked as decided</em> is, and it is the load-bearing half: without it a cartographer
         * that lost the roll would be re-rolled on every chunk load until it won.
         */
        @Test
        @DisplayName("is marked as decided whether the roll won or lost")
        void isMarkedDecidedEitherWay() {
            VillagerMock cartographer = villager(Villager.Profession.CARTOGRAPHER, 1);
            giveVanillaOffers(cartographer);

            changeCareer(cartographer);
            tick(2L);

            assertTrue(rolled(cartographer));
        }

        /** No offers yet means vanilla has not generated them; a later load retries. */
        @Test
        void isNotMarkedWhileItStillHasNoOffers() {
            VillagerMock cartographer = villager(Villager.Profession.CARTOGRAPHER, 1);
            cartographer.setRecipes(new ArrayList<>());

            changeCareer(cartographer);
            tick(2L);

            assertFalse(rolled(cartographer), "an undecided villager must stay eligible for a retry");
        }

        @Test
        void anotherProfessionIsNeverRolled() {
            VillagerMock farmer = villager(Villager.Profession.FARMER, 1);
            giveVanillaOffers(farmer);

            changeCareer(farmer);
            tick(2L);

            assertFalse(rolled(farmer));
        }
    }
}
