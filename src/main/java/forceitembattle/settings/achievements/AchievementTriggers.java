package forceitembattle.settings.achievements;

import com.destroystokyo.paper.loottable.LootableInventory;
import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.BiomeGroup;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import io.papermc.paper.event.player.PlayerLoomPatternSelectEvent;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.world.inventory.ChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

public class AchievementTriggers {

    public static class ObtainedItemRequirement implements AchievementRequirement {

        @Nullable private final Material requiredMaterial;
        private final int requiredAmount;
        @Nullable private final List<Material> requiredItemPool;
        private final long requiredTimeFrame;
        private final long requiredTimeToFind;
        private final int back2backAmount;
        private final int requiredSkips;
        private final int requiredNoSkips;
        private final boolean dontSkip;
        private final boolean back2backSame;
        private final boolean isConsecutive;

        private final Map<Player, Integer> currentCounts;
        private final Map<Player, Long> lastFoundTimes;
        private final Map<Player, Boolean> isCompleted;
        private final Map<Player, Long> previousItemTimes;
        private final Map<Player, Integer> back2backCounts;
        private final Map<Player, Integer> skipCounts;
        private final Map<Player, Integer> noSkipCounts;

        private static final Logger logger = Logger.getLogger("AchievementLogger");

        public ObtainedItemRequirement(@Nullable Material requiredMaterial, int requiredAmount, @Nullable
                                        List<Material> requiredItemPool, long requiredTimeFrame, long requiredTimeToFind,
                                        int back2backAmount, int requiredSkips, int requiredNoSkips, boolean dontSkip, boolean back2backSame, boolean isConsecutive) {
            this.requiredMaterial = requiredMaterial;
            this.requiredAmount = requiredAmount;
            this.requiredItemPool = requiredItemPool;
            this.requiredTimeFrame = requiredTimeFrame;
            this.requiredTimeToFind = requiredTimeToFind;
            this.back2backAmount = back2backAmount;
            this.requiredSkips = requiredSkips;
            this.requiredNoSkips = requiredNoSkips;
            this.dontSkip = dontSkip;
            this.back2backSame = back2backSame;
            this.isConsecutive = isConsecutive;
            this.currentCounts = new HashMap<>();
            this.lastFoundTimes = new HashMap<>();
            this.isCompleted = new HashMap<>();
            this.previousItemTimes = new HashMap<>();
            this.back2backCounts = new HashMap<>();
            this.skipCounts = new HashMap<>();
            this.noSkipCounts = new HashMap<>();
        }

        @Override
        public boolean isMet(Player player, Event event) {
            if(this.isCompleted.getOrDefault(player, false)) {
                return false;
            }
            if(event instanceof FoundItemEvent foundItemEvent) {
                ForceItemPlayer forceItemPlayer = ForceItemBattle.getInstance().getGamemanager().getForceItemPlayer(player.getUniqueId());
                long currentTime = System.currentTimeMillis();

                if (this.requiredTimeFrame > 0) {
                    long gameStartTime = ForceItemBattle.getInstance().getGamemanager().getGameStartTime();
                    if (currentTime - gameStartTime > this.requiredTimeFrame) {
                        return false;
                    }
                }

                if(this.requiredMaterial != null && foundItemEvent.getFoundItem().getType() != this.requiredMaterial) {
                    if(this.isConsecutive) {
                        this.reset(player);
                    }
                    return false;
                }

                if(this.requiredItemPool != null && !this.requiredItemPool.contains(foundItemEvent.getFoundItem().getType())) {
                    if(this.isConsecutive) {
                        this.reset(player);
                    }
                    return false;
                }

                if(this.requiredNoSkips > 0 || this.requiredSkips > 0) {
                    if(foundItemEvent.isSkipped()) {
                        this.skipCounts.put(player, this.skipCounts.getOrDefault(player, 0) + 1);
                        this.noSkipCounts.put(player, 0);
                        if(this.skipCounts.getOrDefault(player, 0) == this.requiredSkips) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    } else {
                        this.noSkipCounts.put(player, this.noSkipCounts.getOrDefault(player, 0) + 1);
                        this.skipCounts.put(player, 0);
                        if(this.dontSkip && this.noSkipCounts.getOrDefault(player, 0) == this.requiredNoSkips) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    }
                }


                if(this.isConsecutive) {
                    if(currentTime - this.lastFoundTimes.getOrDefault(player, 0L) > this.requiredTimeFrame) {
                        this.reset(player);
                        this.currentCounts.put(player, 1);
                    } else {
                        this.currentCounts.put(player, this.currentCounts.getOrDefault(player, 0) + 1);
                    }
                } else {
                    this.currentCounts.put(player, this.currentCounts.getOrDefault(player, 0) + 1);
                }

                this.lastFoundTimes.put(player, currentTime);


                if(this.requiredTimeToFind > 0) {
                    Long prevItemTime = this.previousItemTimes.get(player);
                    if(prevItemTime != null) {
                        if(currentTime - prevItemTime >= this.requiredTimeToFind) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    }
                    this.previousItemTimes.put(player, currentTime);
                    return false;
                }

                if(this.back2backAmount > 0) {
                    if(this.back2backSame) {
                        Material prevMaterial = foundItemEvent.getFoundItem().getType();
                        if(prevMaterial == forceItemPlayer.currentMaterial()) {
                            this.isCompleted.put(player, true);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    if(forceItemPlayer.backToBackStreak() == this.back2backAmount) {
                        this.isCompleted.put(player, true);
                        return true;
                    }
                    return false;
                }

                if(this.currentCounts.getOrDefault(player, 0) == this.requiredAmount) {
                    this.isCompleted.put(player, true);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void reset(Player player) {
            this.currentCounts.put(player, 0);
            this.lastFoundTimes.put(player, 0L);
            this.isCompleted.put(player, false);
            this.previousItemTimes.remove(player);
            this.back2backCounts.put(player, 0);
            this.skipCounts.put(player, 0);
            this.noSkipCounts.put(player, 0);
        }
    }

    public static class VisitedRequirement implements AchievementRequirement {

        private final Set<String> DIMENSIONS = Set.of("world", "world_nether", "world_the_end");

        private final boolean visitBiome;

        private final Map<Player, Boolean> isCompleted;

        private final Map<Player, Set<BiomeGroup>> visitedBiomes;
        private final Map<Player, Set<String>> visitedDimensions;

        public VisitedRequirement(boolean visitBiome) {
            this.visitBiome = visitBiome;

            this.isCompleted = new HashMap<>();
            this.visitedBiomes = new HashMap<>();
            this.visitedDimensions = new HashMap<>();
        }

        @Override
        public boolean isMet(Player player, Event event) {
            if (this.isCompleted.getOrDefault(player, false)) {
                return false;
            }
            if(this.visitBiome) {
                if(event instanceof PlayerMoveEvent playerMoveEvent) {

                    Biome toBiome = playerMoveEvent.getTo().getBlock().getBiome();
                    BiomeGroup enteredBiomeGroup = this.getBiomeGroup(toBiome);

                    if(enteredBiomeGroup != null) {
                        this.visitedBiomes.putIfAbsent(player, new HashSet<>());
                        Set<BiomeGroup> biomeGroups = this.visitedBiomes.get(player);
                        biomeGroups.add(enteredBiomeGroup);

                        if(biomeGroups.containsAll(Arrays.stream(BiomeGroup.values()).toList())) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    }
                }
            } else {
                if(event instanceof PlayerChangedWorldEvent playerChangedWorldEvent) {
                    String currentWorld = player.getWorld().getName();
                    this.visitedDimensions.putIfAbsent(player, new HashSet<>());
                    Set<String> dimensions = this.visitedDimensions.get(player);
                    dimensions.add(currentWorld);

                    if(dimensions.containsAll(this.DIMENSIONS)) {
                        this.isCompleted.put(player, true);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void reset(Player player) {
            this.visitedBiomes.remove(player);
            this.visitedDimensions.remove(player);
            this.isCompleted.put(player, false);
        }

        private BiomeGroup getBiomeGroup(Biome biome) {
            for(BiomeGroup biomeGroup : BiomeGroup.values()) {
                if(biomeGroup.getBiomes().contains(biome)) {
                    return biomeGroup;
                }
            }
            return null;
        }
    }

    public static class PlayerActionRequirement implements AchievementRequirement {

        private final int requireTrades;
        private final boolean lootLegendary;
        private final boolean lootCavendish;
        private final boolean lootNeededItem;
        private final boolean eatSomething;
        private final boolean dontDie;

        private final Map<Player, Integer> tradesCount;
        private final Map<Player, Boolean> hasDied;
        private final Map<Player, Boolean> isCompleted;

        public PlayerActionRequirement(int requireTrades, boolean lootLegendary, boolean lootCavendish, boolean lootNeededItem, boolean eatSomething, boolean dontDie) {
            this.requireTrades = requireTrades;
            this.lootLegendary = lootLegendary;
            this.lootCavendish = lootCavendish;
            this.lootNeededItem = lootNeededItem;
            this.eatSomething = eatSomething;
            this.dontDie = dontDie;

            this.tradesCount = new HashMap<>();
            this.hasDied = new HashMap<>();
            this.isCompleted = new HashMap<>();
        }

        @Override
        public boolean isMet(Player player, Event event) {
            if (this.isCompleted.getOrDefault(player, false)) {
                return false;
            }

            if(event instanceof PlayerDeathEvent) {
                this.hasDied.put(player, true);
            }

            if(this.eatSomething) {
                if (event instanceof PlayerItemConsumeEvent playerItemConsumeEvent) {
                    ItemMeta itemMeta = playerItemConsumeEvent.getItem().getItemMeta();
                    // cavendish custom model data
                    if (playerItemConsumeEvent.getItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                        if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 1) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    }
                }
            }

            if(this.requireTrades > 0) {
                if(event instanceof PlayerTradeEvent playerTradeEvent) {
                    if(playerTradeEvent.getVillager() instanceof WanderingTrader) {
                        this.tradesCount.put(player, this.tradesCount.getOrDefault(player, 0) + 1);
                        if(this.tradesCount.getOrDefault(player, 0) == this.requireTrades) {
                            this.isCompleted.put(player, true);
                            return true;
                        }
                    }
                }
            }

            if(this.lootNeededItem || this.lootCavendish || this.lootLegendary) {
                if(event instanceof PlayerInteractEvent playerInteractEvent) {
                    ForceItemPlayer forceItemPlayer = ForceItemBattle.getInstance().getGamemanager().getForceItemPlayer(playerInteractEvent.getPlayer().getUniqueId());
                    Block block = playerInteractEvent.getClickedBlock();
                    assert block != null;
                    BlockState blockState = block.getState();
                    if(blockState instanceof Chest chest) {
                        if(chest.hasLootTable()) {
                            for(ItemStack contents : chest.getInventory().getContents()) {
                                if(contents == null) continue;
                                String plainDisplayName = PlainTextComponentSerializer.plainText().serialize(contents.displayName());
                                if(this.lootLegendary) {
                                    if(plainDisplayName.contains("[LEGENDARY]")) {
                                        this.isCompleted.put(player, true);
                                        return true;
                                    }
                                }

                                if(this.lootCavendish) {
                                    if(contents.getItemMeta().hasCustomModelData() && contents.getItemMeta().getCustomModelData() == 1) {
                                        this.isCompleted.put(player, true);
                                        return true;
                                    }
                                }

                                if(this.lootNeededItem) {
                                    if(contents.getType() == forceItemPlayer.currentMaterial()) {
                                        this.isCompleted.put(player, true);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if(this.dontDie) {
                if(ForceItemBattle.getInstance().getGamemanager().isEndGame() && !this.hasDied.getOrDefault(player, false)) {
                    this.isCompleted.put(player, true);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void reset(Player player) {
            this.hasDied.put(player, false);
            this.isCompleted.put(player, false);
        }
    }

    public static class MiscRequirement implements AchievementRequirement {

        private final Map<Player, Boolean> isCompleted;

        public MiscRequirement() {

            this.isCompleted = new HashMap<>();
        }

        @Override
        public boolean isMet(Player player, Event event) {
            if (this.isCompleted.getOrDefault(player, false)) {
                return false;
            }

            ForceItemPlayerStats forceItemPlayerStats = ForceItemBattle.getInstance().getStatsManager().playerStats(player.getName());

            if(forceItemPlayerStats.achievementsDone().size() == ForceItemBattle.getInstance().getAchievementManager().achievementsList().size()) {
                this.isCompleted.put(player, true);
                return true;
            }

            return false;
        }

        @Override
        public void reset(Player player) {
            this.isCompleted.put(player, false);
        }
    }
}
