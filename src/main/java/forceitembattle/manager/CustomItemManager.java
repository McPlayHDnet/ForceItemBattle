package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Scheduler;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

@RequiredArgsConstructor
public class CustomItemManager implements Manager {

    private final ForceItemBattle forceItemBattle;

    @Override
    public void enable() {
        Scheduler.runSync(this::resolvePrototypes);
    }

    @Override
    public void disable() {
        for (CustomMaterials customMaterial : CustomMaterials.values()) {
            customMaterial.setPrototype(null);
        }
    }

    private void resolvePrototypes() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            this.forceItemBattle.getLogger().log(Level.SEVERE,
                    "No world loaded — custom items could not be resolved.");
            return;
        }
        Location context = worlds.getFirst().getSpawnLocation();

        for (CustomMaterials customMaterial : CustomMaterials.values()) {
            if (customMaterial.getItemLootTable() == null) {
                continue;
            }

            LootTable lootTable = Bukkit.getLootTable(customMaterial.getItemLootTable());
            if (lootTable == null) {
                this.forceItemBattle.getLogger().log(Level.WARNING,
                        "Loot table " + customMaterial.getItemLootTable() + " for " + customMaterial.name()
                                + " is not loaded — is the datapack enabled?");
                continue;
            }

            Collection<ItemStack> loot = lootTable.populateLoot(
                    ThreadLocalRandom.current(),
                    new LootContext.Builder(context).build()
            );

            if (loot.isEmpty()) {
                this.forceItemBattle.getLogger().log(Level.WARNING,
                        "Loot table " + customMaterial.getItemLootTable() + " for " + customMaterial.name()
                                + " produced no item.");
                continue;
            }

            customMaterial.setPrototype(loot.iterator().next());
        }
    }
}
