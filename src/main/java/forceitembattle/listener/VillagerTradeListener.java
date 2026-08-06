package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

@RequiredArgsConstructor
public class VillagerTradeListener implements Listener {

    private static final double CARTOGRAPHER_TRADE_CHANCE = 0.30D;

    /** The cleric offers the Eye of Antimatter from apprentice onwards, always. */
    private static final int CLERIC_TRADE_LEVEL = 2;
    private static final int CLERIC_TRADE_PRICE = 6;
    private static final int CLERIC_TRADE_MAX_USES = 12;

    private final ForceItemBattle plugin;

    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (event.getProfession() != Villager.Profession.CARTOGRAPHER) return;

        Villager villager = event.getEntity();
        Scheduler.runLaterSync(() -> this.rollCartographerTrade(villager), 1L);
    }

    /**
     * Fires while a villager generates the offers for the level it just reached — the moment to
     * append ours. A tick later, so vanilla is done writing the list we are about to replace.
     */
    @EventHandler
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.getProfession() != Villager.Profession.CLERIC) return;

        Scheduler.runLaterSync(() -> this.addClericTrade(villager), 1L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof Villager villager)) continue;

            if (villager.getProfession() == Villager.Profession.CARTOGRAPHER && !this.hasBeenRolled(villager)) {
                Scheduler.runLaterSync(() -> {
                    if (!this.hasBeenRolled(villager)) this.rollCartographerTrade(villager);
                }, 1L);
            } else if (villager.getProfession() == Villager.Profession.CLERIC) {
                // A cleric that reached apprentice before this listener ever saw it — after a
                // restart, say. addClericTrade is a no-op when the offer is already there.
                Scheduler.runLaterSync(() -> this.addClericTrade(villager), 1L);
            }
        }
    }

    private void rollCartographerTrade(Villager villager) {
        if (!villager.isValid()) return;
        if (villager.getProfession() != Villager.Profession.CARTOGRAPHER) return;

        List<MerchantRecipe> recipes = villager.getRecipes();
        if (recipes.isEmpty()) return; // offers not generated yet; a later load retries

        this.markRolled(villager); // decided for this professioned state, win or lose

        if (ThreadLocalRandom.current().nextDouble() >= CARTOGRAPHER_TRADE_CHANCE) return;

        List<MerchantRecipe> updated = new ArrayList<>(recipes);
        updated.add(0, this.buildCustomTrade()); // front => one of the first offers
        villager.setRecipes(updated);
    }

    /**
     * Appends the Eye of Antimatter offer, once the cleric is apprentice or better. Whether it is
     * already there is read off the offer list rather than a marker on the villager: a cured or
     * re-professioned villager regenerates its trades, and a marker would leave it without the
     * offer forever.
     */
    private void addClericTrade(Villager villager) {
        if (!villager.isValid()) return;
        if (villager.getProfession() != Villager.Profession.CLERIC) return;
        if (villager.getVillagerLevel() < CLERIC_TRADE_LEVEL) return;

        List<MerchantRecipe> recipes = villager.getRecipes();
        if (recipes.stream().anyMatch(recipe -> CustomMaterials.EYE_OF_ANTIMATTER.matches(recipe.getResult()))) {
            return;
        }

        List<MerchantRecipe> updated = new ArrayList<>(recipes);
        updated.add(this.buildClericTrade()); // last => it reads as the offer the new level unlocked
        villager.setRecipes(updated);
    }

    private MerchantRecipe buildClericTrade() {
        MerchantRecipe recipe = new MerchantRecipe(CustomMaterials.EYE_OF_ANTIMATTER.itemStack(), CLERIC_TRADE_MAX_USES);
        recipe.addIngredient(new ItemStack(Material.EMERALD, CLERIC_TRADE_PRICE));
        recipe.setExperienceReward(true);
        recipe.setVillagerExperience(5); // vanilla's apprentice-level reward
        recipe.setPriceMultiplier(0.05F);
        return recipe;
    }

    private MerchantRecipe buildCustomTrade() {
        ItemStack result = CustomMaterials.SULFUR_LOCATOR.itemStack();

        MerchantRecipe recipe = new MerchantRecipe(result, 12); // maxUses
        recipe.addIngredient(new ItemStack(Material.EMERALD, 6));
        recipe.addIngredient(new ItemStack(Material.COMPASS, 1));
        recipe.setExperienceReward(true);
        recipe.setVillagerExperience(2);
        recipe.setPriceMultiplier(0.05F);
        return recipe;
    }

    private boolean hasBeenRolled(Villager villager) {
        return villager.getPersistentDataContainer().has(this.rolledKey(), PersistentDataType.BYTE);
    }

    private void markRolled(Villager villager) {
        villager.getPersistentDataContainer().set(this.rolledKey(), PersistentDataType.BYTE, (byte) 1);
    }

    private NamespacedKey rolledKey() {
        return new NamespacedKey(this.plugin, "cartographer_trade_rolled");
    }
}
