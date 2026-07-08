package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.gui.ItemBuilder;
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
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

@RequiredArgsConstructor
public class VillagerTradeListener implements Listener {

    private static final double CARTOGRAPHER_TRADE_CHANCE = 0.30D;

    private final ForceItemBattle plugin;

    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (event.getProfession() != Villager.Profession.CARTOGRAPHER) return;

        Villager villager = event.getEntity();
        Scheduler.runLaterSync(() -> this.rollCartographerTrade(villager), 1L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Villager villager
                    && villager.getProfession() == Villager.Profession.CARTOGRAPHER
                    && !this.hasBeenRolled(villager)) {
                Scheduler.runLaterSync(() -> {
                    if (!this.hasBeenRolled(villager)) this.rollCartographerTrade(villager);
                }, 1L);
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

    private MerchantRecipe buildCustomTrade() {
        ItemStack result = new ItemBuilder(Material.MUSIC_DISC_CHIRP)
                .setAmount(1)
                .setDisplayName("<yellow>Sulfur Tracker")
                .getItemStack();

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
