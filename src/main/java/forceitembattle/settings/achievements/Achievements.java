package forceitembattle.settings.achievements;

import forceitembattle.settings.achievements.handlers.*;
import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Set;

@Getter
public enum Achievements {

    // OBTAIN_ITEM achievements
    ITEM_COLLECTOR("Item Collector", "Collect 40 items in one round",
            new CounterHandler(40, false, null)),

    ITEM_GATHERER("Item Gatherer", "Collect 50 items in one round",
            new CounterHandler(50, false, null)),

    ITEM_HOARDER("Item Hoarder", "Collect 60 items in one round",
            new CounterHandler(60, false, null)),

    RELENTLESS_COLLECTOR("Relentless Collector", "Collect 70 items in one round",
            new CounterHandler(70, false, null)),

    ULTIMATE_COLLECTOR("Ultimate Collector", "Collect 85 items in one round",
            new CounterHandler(85, false, null)),

    ONE_DIMENSION_WONDER("One Dimension Wonder", "Collect 10 overworld items in a row",
            new CounterHandler(10, true, "world")),

    INFINITE_FIRE("Infinite Fire", "Collect 5 nether items in a row",
            new CounterHandler(5, true, "world_nether")),

    ITS_SO_EMPTY("It's so empty", "Collect 3 end items in a row",
            new CounterHandler(3, true, "world_the_end")),

    WAIT_WOOD("Wait Wood?", "Collect at least one item from each wood type in one round",
            CollectionHandler.woodTypesHandler()),

    THATS_A_ROCK_JIM("That's a Rock, Jim", "Collect 3 stone-type items in a row",
            new ConsecutiveStoneHandler(3)),

    ONE_IN_A_MILLION("One in a Million", "Collect a very rare mob drop (Trident or Wither Skeleton Skull)",
            new RareMobDropHandler(1)),

    LUCKY_ROW("Lucky Row", "Collect 10 items in a row without skipping any",
            new CounterHandler(10, true, null)),

    YOU_GET_WHAT_YOU_GET("You Get What You Get", "Collect 15 items in a row without skipping any",
            new CounterHandler(15, true, null)),

    // TIME-BASED achievements
    SPEED_COLLECTOR("Speed Collector", "Collect 7 items within the first 5 minutes of the round",
            new TimeBasedHandler(7, 5 * 60, 0, 0, 0, true, false, false)),

    QUICK_GRAB("Quick Grab", "Collect an item within the first 30 seconds without skipping",
            new TimeBasedHandler(1, 30, 0, 0, 0, true, false, true)),

    PROCRASTINATOR("Procrastinator", "Skip an item after keeping it for 10 minutes",
            new TimeBasedHandler(1, 0, 0, 10 * 60, 0, false, false, false)),

    WAS_IT_WORTHWHILE("Was It Worthwhile?", "Collect an item that took you at least 15 minutes to find",
            new TimeBasedHandler(1, 0, 15 * 60, 0, 0, true, false, false)),

    CLOSE_CALL("Close Call", "Collect an item within the last 5 seconds of the round",
            new TimeBasedHandler(1, 0, 0, 0, 5, true, false, true)),

    EARLY_BIRD("Early Bird", "Be the first player to collect any item in the round",
            new TimeBasedHandler(1, 0, 0, 0, 0, false, true, true)),

    // BACK-TO-BACK achievements
    BACK_TO_BACK("Back-to-Back", "Get your next required item immediately",
            new BackToBackHandler(1, false, false)),

    DOUBLE_TROUBLE("Double Trouble", "Get 2 back-to-back items in a row",
            new BackToBackHandler(2, false, false)),

    OH_BABY_A_TRIPLE("Oh Baby A Triple!", "Get 3 back-to-back items in a row",
            new BackToBackHandler(3, false, false)),

    FOUR_LEAF_CLOVER("Four-leaf Clover", "Get 4 back-to-back items in a row",
            new BackToBackHandler(4, false, false)),

    DEJA_VU("Déjà Vu", "Get the same item type twice in a row as back-to-back",
            new BackToBackHandler(1, true, false)),

    ACCIDENTAL_GENIUS("Accidental Genius", "Skip an item, then get it again as back-to-back",
            new BackToBackHandler(1, false, true)),

    // VISIT achievements
    BIOME_HOPPER("Biome Hopper", "Visit all basic overworld biomes in one round",
            CollectionHandler.biomeHandler(Set.of(BiomeGroup.values()))),

    ALWAYS_ON_THE_GO("Always On The Go", "Visit all three dimensions in one round",
            CollectionHandler.dimensionHandler(Set.of("world", "world_nether", "world_the_end"))),

    // SKIP achievements
    UNLUCKY("Unlucky", "Skip 3 items in a row",
            new SkipHandler(3, true, 0)),

    FUCK_THIS("Fuck this", "Skip an item within 3 seconds of receiving it",
            new SkipHandler(1, false, 3)),

    // ACTION achievements
    CHICOT("Chicot", "Complete a round without dying",
            new DeathCounterHandler(0)),

    CONNOISSEUR("Connoisseur", "Eat Cavendish",
            new EatingHandler(1, new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, "cavendish", null))),

    THANK_YOU("Thank you", "Trade with the wandering trader 10 times in one round",
            new TradingHandler(10)),

    A_BALANCED_INVENTORY("A Balanced Inventory", "Fill every slot in your inventory (including backpack if enabled)",
            new InventoryFullHandler()),

    HONEY_HONEY("Honey, honey, how you thrill me, aha, honey honey", "Harvest 2 full beehives with shears",
            new BeehiveHandler(2)),

    // LOOT achievements
    WHEEL_OF_FORTUNE("Wheel of Fortune", "Find a Legendary item in a loot chest",
            new LootHandler(1, new CustomItem(null, "[LEGENDARY]"), false)),

    WILL_IT_BREAK("Will it break?", "Find Cavendish in a loot chest",
            new LootHandler(1, new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, 1, "Cavendish"), false)),

    BELIEVER("Believer", "Find your currently needed item in a loot chest",
            new LootHandler(1, null, true)),

    // META achievement
    COMPLETIONIST("Completionist++", "Complete all achievements",
            new CompletionistHandler());

    private final String title;
    private final String description;
    private final AchievementHandler<?> handler;

    Achievements(String title, String description, AchievementHandler<?> handler) {
        this.title = title;
        this.description = description;
        this.handler = handler;
    }
}