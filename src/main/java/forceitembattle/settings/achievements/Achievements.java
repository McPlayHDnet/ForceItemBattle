package forceitembattle.settings.achievements;

import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum Achievements {

    // OBTAIN_ITEMS Achievements (Team-based except ONE_IN_A_MILLION)
    ITEM_COLLECTOR("Item Collector", "Find 40 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(40)),
    ITEM_GATHERER("Item Gatherer", "Find 50 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(50)),
    ITEM_HOARDER("Item Hoarder", "Find 60 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(60)),
    RELENTLESS_COLLECTOR("Relentless Collector", "Find 70 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(70)),
    ULTIMATE_COLLECTOR("Ultimate Collector", "Find 85 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(85)),
    ONE_DIMENSION_WONDER("One Dimension Wonder", "Get 10 overworld items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(10).dimension("world").consecutive()),
    INFINITE_FIRE("Infinite Fire", "Get 5 nether items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(5).dimension("world_nether").consecutive()),
    ITS_SO_EMPTY("It's so empty", "Get 3 end items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(3).dimension("world_the_end").consecutive()),
    WAIT_WOOD("Wait Wood?", "Get at least 1 item for each wood type in 1 round", new Condition(Trigger.OBTAIN_ITEM).woodTypes()),
    THATS_A_ROCK_JIM("That's a Rock, Jim", "Get 3 stone type items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(3).stoneTypes().consecutive()),
    ONE_IN_A_MILLION("One in a Million", "Get a very rare mob drop (Trident or Wither Skeleton Skull)", new Condition(Trigger.OBTAIN_ITEM).rareMobDrop().playerBased()),

    // OBTAIN_ITEMS_TIME_FRAME Achievements
    SPEED_COLLECTOR("Speed Collector", "Collect 7 items within the first 5 minutes of the game", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(7).noSkip().withinSeconds(5 * 60)),
    QUICK_GRAB("Quick Grab", "Collect an item within the first 30 seconds of round start", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).noSkip().withinSeconds(30).playerBased()),
    PROCRASTINATOR("Procrastinator", "Skip an item 10 minutes after receiving it", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).skipAfterSeconds(10 * 60)),
    WAS_IT_WORTHWHILE("Was It Worthwhile?", "Collect an item that took you 15 minutes to find", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).noSkip().timeFrameInSeconds(15 * 60)),
    CLOSE_CALL("Close Call", "Collect an item within the last 5 seconds of the round (no skip)", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).noSkip().closeCall(5).playerBased()),
    EARLY_BIRD("Early Bird", "Be the first player to collect an item", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).firstPlayer().playerBased()),

    // BACK_TO_BACK Achievements
    BACK_TO_BACK("Back-to-Back", "Get 1 b2b item", new Condition(Trigger.BACK_TO_BACK).amount(1)),
    DOUBLE_TROUBLE("Double Trouble", "Get 2 b2b items", new Condition(Trigger.BACK_TO_BACK).amount(2)),
    OH_BABY_A_TRIPLE("Oh Baby A Triple!", "Get 3 b2b items", new Condition(Trigger.BACK_TO_BACK).amount(3)),
    FOUR_LEAF_CLOVER("Four-leaf Clover", "Get 4 b2b items", new Condition(Trigger.BACK_TO_BACK).amount(4)),
    DEJA_VU("Déjà Vu", "Get the same item b2b", new Condition(Trigger.BACK_TO_BACK).amount(1).sameItem()),
    ACCIDENTAL_GENIUS("Accidental Genius", "Skip an item and get it again", new Condition(Trigger.BACK_TO_BACK).amount(1).skippedThenGot()),

    // VISIT Achievements
    BIOME_HOPPER("Biome Hopper", "Visit all overworld basic biomes in 1 round", new Condition(Trigger.VISIT).biomeList(BiomeGroup.values())),
    ALWAYS_ON_THE_GO("Always On The Go", "Visit all dimensions in 1 round", new Condition(Trigger.VISIT).dimension("world", "world_nether", "world_the_end")),

    // PLAYER_ACTION Achievements
    CHICOT("Chicot", "Do not die in 1 round", new Condition(Trigger.DYING).amount(0)),
    UNLUCKY("Unlucky", "Skip 3 times in a row", new Condition(Trigger.SKIP_ITEM).amount(3).consecutive()),
    LUCKY_ROW("Lucky Row", "Don't skip for 10 items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(10).noSkip().consecutive()),
    YOU_GET_WHAT_YOU_GET("You Get What You Get", "Don't skip for 15 items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(15).noSkip().consecutive()),
    CONNOISSEUR("Connoisseur", "Eat Cavendish", new Condition(Trigger.EATING).amount(1).customItem(new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, "cavendish", null))),
    THANK_YOU("Thank you", "Use the wandering trader for 10 trades in 1 round", new Condition(Trigger.TRADING).amount(10)),
    FUCK_THIS("Fuck this", "Skip an item within 3 seconds of receiving it", new Condition(Trigger.SKIP_ITEM).amount(1).withinSeconds(3)),

    // PLAYER Achievements
    A_BALANCED_INVENTORY("A Balanced Inventory", "Have every inventory + backpack slots filled", new Condition(Trigger.INVENTORY_FULL)),

    // PLAYER_LOOTED Achievements
    WHEEL_OF_FORTUNE("Wheel of Fortune", "Loot a Legendary", new Condition(Trigger.LOOT).amount(1).customItem(new CustomItem(null, "[LEGENDARY]"))),
    WILL_IT_BREAK("Will it break?", "Loot Cavendish", new Condition(Trigger.LOOT).amount(1).customItem(new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, 1, "Cavendish"))),
    HONEY_HONEY("Honey, honey, how you thrill me, aha, honey honey", "Harvest 2 beehives in 1 round", new Condition(Trigger.BEEHIVE_HARVEST).amount(2)),
    BELIEVER("Believer", "Find a needed item from loot chest in 1 round", new Condition(Trigger.LOOT).neededItem()),

    // COMPLETED_ACHIEVEMENTS
    COMPLETIONIST("Completionist++", "Complete all Achievements", new Condition(Trigger.ACHIEVEMENT));

    private final String title;
    private final String description;
    private final Condition condition;

    Achievements(String title, String description, Condition condition) {
        this.title = title;
        this.description = description;
        this.condition = condition;
    }

}