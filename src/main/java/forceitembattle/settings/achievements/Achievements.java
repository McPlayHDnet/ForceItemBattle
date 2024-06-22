package forceitembattle.settings.achievements;

import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum Achievements {

    ITEM_COLLECTOR("Item Collector", "Find 30 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(30)),
    ITEM_GATHERER("Item Gatherer", "Find 40 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(40)),
    ITEM_HOARDER("Item Hoarder", "Find 50 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(50)),
    ULTIMATE_COLLECTOR("Ultimate Collector", "Find 60 items in 1 round", new Condition(Trigger.OBTAIN_ITEM).amount(60)),
    ONE_DIMENSION_WONDER("One Dimension Wonder", "Get 15 overworld items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(15).dimension("world").consecutive()),
    INFINITE_FIRE("Infinite FIre", "Get 5 nether items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(5).dimension("world_nether").consecutive()),
    ITS_SO_EMPTY("It's so empty", "Get 3 end items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(3).dimension("world_the_end").consecutive()),
    SPEED_COLLECTOR("Speed Collector", "Collect 7 items within the first 5 minutes of the game", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(7).noSkip().withinSeconds(5 * 60)),
    QUICK_GRAB("Quick Grab", "Collect an item within the first 30 seconds of round start", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).noSkip().withinSeconds(30)),
    WAS_IT_WORTHWHILE("Was it worthwhile?", "Collect an item that took you 15 minutes to find", new Condition(Trigger.OBTAIN_ITEM_IN_TIME).amount(1).noSkip().timeFrameInSeconds(15 * 60)),
    BACK_TO_BACK("Again, UwU?", "Get 1 item that you already have", new Condition(Trigger.BACK_TO_BACK).amount(1)),
    DOUBLE_TROUBLE("Double Trouble", "Get 2 items that you already have", new Condition(Trigger.BACK_TO_BACK).amount(2)),
    OH_BABY_A_TRIPLE("Oh Baby A Triple!", "Get 3 items that you already have", new Condition(Trigger.BACK_TO_BACK).amount(3)),
    FOUR_LEAF_CLOVER("Four-leaf Clover", "Get 4 items that you already have", new Condition(Trigger.BACK_TO_BACK).amount(4)),
    DEJA_VU("Déjà-Vu", "Get the same item b2b", new Condition(Trigger.BACK_TO_BACK).amount(1).sameItem()),
    BIOME_HOPPER("Biome Hopper", "Visit all overworld basic biomes in 1 round", new Condition(Trigger.VISIT).biomeList(BiomeGroup.values())),
    ALWAYS_ON_THE_GO("Always On The Go", "Visit all dimensions in 1 round", new Condition(Trigger.VISIT).dimension("world", "world_nether", "world_the_end")),
    CHICOT("Chicot", "Do not die in 1 round", new Condition(Trigger.DYING).amount(0)),
    UNLUCKY("Unlucky", "Skip 3 times in a row", new Condition(Trigger.SKIP_ITEM).amount(3).consecutive()),
    LUCKY_ROW("Lucky Row", "Don't skip for 15 items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(15).noSkip().consecutive()),
    YOU_GET_WHAT_YOU_GET("You Get What You Get", "Don't skip for 20 items in a row", new Condition(Trigger.OBTAIN_ITEM).amount(20).noSkip().consecutive()),
    CONNOISSEUR("Connoisseur", "Eat Cavendish", new Condition(Trigger.EATING).amount(1).customItem(new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, 1, null))),
    THANK_YOU("Thank You!", "Use the wandering trader for 10 trades in 1 round", new Condition(Trigger.TRADING).amount(10)),
    WHEEL_OF_FORTUNE("Wheel of Fortune", "Loot a Legendary", new Condition(Trigger.LOOT).amount(1).customItem(new CustomItem(null, 0, "[LEGENDARY]"))),
    WILL_IT_BREAK("Will it break?", "Loot Cavendish", new Condition(Trigger.LOOT).amount(1).customItem(new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, 1, "Cavendish"))),
    BELIEVER("Believer", "Find a needed item from loot chest in 1 round", new Condition(Trigger.LOOT)),
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
