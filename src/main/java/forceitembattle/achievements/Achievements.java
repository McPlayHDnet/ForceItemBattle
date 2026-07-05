package forceitembattle.achievements;

import forceitembattle.achievements.handlers.AchievementHandler;
import forceitembattle.achievements.handlers.AntimatterTeleporterUsesAchievementHandler;
import forceitembattle.achievements.handlers.BackToBackAchievementHandler;
import forceitembattle.achievements.handlers.BackToBackCountAchievementHandler;
import forceitembattle.achievements.handlers.BeehiveAchievementHandler;
import forceitembattle.achievements.handlers.CollectionAchievementHandler;
import forceitembattle.achievements.handlers.CompletionistAchievementHandler;
import forceitembattle.achievements.handlers.ConsecutiveStoneAchievementHandler;
import forceitembattle.achievements.handlers.CounterAchievementHandler;
import forceitembattle.achievements.handlers.DeathCounterAchievementHandler;
import forceitembattle.achievements.handlers.EatingAchievementHandler;
import forceitembattle.achievements.handlers.InventoryFullAchievementHandler;
import forceitembattle.achievements.handlers.LootAchievementHandler;
import forceitembattle.achievements.handlers.NoAntimatterAchievementHandler;
import forceitembattle.achievements.handlers.NoBackToBackAchievementHandler;
import forceitembattle.achievements.handlers.NoOverworldExitAchievementHandler;
import forceitembattle.achievements.handlers.RareMobDropAchievementHandler;
import forceitembattle.achievements.handlers.RepeatItemAchievementHandler;
import forceitembattle.achievements.handlers.SkipAchievementHandler;
import forceitembattle.achievements.handlers.TimeBasedAchievementHandler;
import forceitembattle.achievements.handlers.TradingAchievementHandler;
import forceitembattle.achievements.handlers.WheelOfFortuneAchievementHandler;
import forceitembattle.achievements.handlers.WheelOfFortuneUsesAchievementHandler;
import forceitembattle.util.BiomeGroup;
import forceitembattle.util.CustomItem;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Biome;

@Getter
public enum Achievements {

    // OBTAIN_ITEM achievements
    ITEM_COLLECTOR("Item Collector", "Collect 40 items in one round",
            new CounterAchievementHandler(40, false, null)),

    ITEM_GATHERER("Item Gatherer", "Collect 50 items in one round",
            new CounterAchievementHandler(50, false, null)),

    ITEM_HOARDER("Item Hoarder", "Collect 60 items in one round",
            new CounterAchievementHandler(60, false, null)),

    RELENTLESS_COLLECTOR("Relentless Collector", "Collect 70 items in one round",
            new CounterAchievementHandler(70, false, null)),

    ULTIMATE_COLLECTOR("Ultimate Collector", "Collect 85 items in one round",
            new CounterAchievementHandler(85, false, null)),

    ONE_DIMENSION_WONDER("One Dimension Wonder", "Collect 10 overworld items in a row",
            new CounterAchievementHandler(10, true, "world")),

    NEVER_LEFT_HOME("Never Left Home", "Collect 20 overworld items in a row",
            new CounterAchievementHandler(20, true, "world")),

    INFINITE_FIRE("Infinite Fire", "Collect 5 nether items in a row",
            new CounterAchievementHandler(5, true, "world_nether")),

    ITS_SO_EMPTY("It's so empty", "Collect 3 end items in a row",
            new CounterAchievementHandler(3, true, "world_the_end")),

    WAIT_WOOD("Wait Wood?", "Collect at least one item from each wood type in one round",
            CollectionAchievementHandler.woodTypesHandler()),

    THATS_A_ROCK_JIM("That's a Rock, Jim", "Collect 3 stone-type items in a row",
            new ConsecutiveStoneAchievementHandler(3)),

    ONE_IN_A_MILLION("One in a Million", "Collect a very rare mob drop (Trident or Wither Skeleton Skull)",
            new RareMobDropAchievementHandler(1)),

    LUCKY_ROW("Lucky Row", "Collect 10 items in a row without skipping any",
            new CounterAchievementHandler(10, true, null)),

    YOU_GET_WHAT_YOU_GET("You Get What You Get", "Collect 15 items in a row without skipping any",
            new CounterAchievementHandler(15, true, null)),

    THEY_SEE_ME_ROLLIN("They See Me Rollin", "Collect 25 items in a row without skipping any",
            new CounterAchievementHandler(25, true, null)),

    THERE_IS_NO_WAY("There Is No Way", "Get your assigned item from the Wheel of Fortune",
            new WheelOfFortuneAchievementHandler()),

    SEEING_TRIPLE("Seeing Triple", "Get the same item assigned 3 times in a single round",
            new RepeatItemAchievementHandler(3)),

    STATISTICAL_ANOMALY("Statistical Anomaly", "Get the same item assigned 5 times in a single round",
            new RepeatItemAchievementHandler(5)),

    // TIME-BASED achievements
    SPEED_COLLECTOR("Speed Collector", "Collect 7 items within the first 5 minutes of the round",
            new TimeBasedAchievementHandler(7, 5 * 60, 0, 0, 0, true, false, false)),

    QUICK_GRAB("Quick Grab", "Collect an item within the first 30 seconds without skipping",
            new TimeBasedAchievementHandler(1, 30, 0, 0, 0, true, false, false)),

    PROCRASTINATOR("Procrastinator", "Skip an item after keeping it for 10 minutes",
            new TimeBasedAchievementHandler(1, 0, 0, 10 * 60, 0, false, false, false)),

    WAS_IT_WORTHWHILE("Was It Worthwhile?", "Collect an item that took you at least 15 minutes to find",
            new TimeBasedAchievementHandler(1, 0, 15 * 60, 0, 0, true, false, false)),

    CLOSE_CALL("Close Call", "Collect an item within the last 5 seconds of the round",
            new TimeBasedAchievementHandler(1, 0, 0, 0, 5, false, false, false)),

    BUZZER_BEATER("Buzzer Beater", "Collect 3 items in the final minute",
            new TimeBasedAchievementHandler(3, 0, 0, 0, 60, false, false, false)),

    EARLY_BIRD("Early Bird", "Be the first player to collect any item in the round",
            new TimeBasedAchievementHandler(1, 0, 0, 0, 0, false, true, true)),

    // BACK-TO-BACK achievements
    BACK_TO_BACK("Back-to-Back", "Get your next required item immediately",
            new BackToBackAchievementHandler(1, false, false)),

    DOUBLE_TROUBLE("Double Trouble", "Get 2 back-to-back items in a row",
            new BackToBackAchievementHandler(2, false, false)),

    OH_BABY_A_TRIPLE("Oh Baby A Triple!", "Get 3 back-to-back items in a row",
            new BackToBackAchievementHandler(3, false, false)),

    FOUR_LEAF_CLOVER("Four-leaf Clover", "Get 4 back-to-back items in a row",
            new BackToBackAchievementHandler(4, false, false)),

    DEJA_VU("Déjà Vu", "Get the same item type twice in a row as back-to-back",
            new BackToBackAchievementHandler(1, true, false)),

    ACCIDENTAL_GENIUS("Accidental Genius", "Skip an item, then get it again as back-to-back",
            new BackToBackAchievementHandler(1, false, true)),

    THE_HARD_WAY("The Hard Way", "Finish a game without a single back-to-back",
            new NoBackToBackAchievementHandler()),

    NO_HANDOUTS("No Handouts", "Win a game without a single back-to-back",
            new NoBackToBackAchievementHandler()),

    HIGH_ROLLER("High Roller", "Get 5 or more back-to-backs in a single game",
            new BackToBackCountAchievementHandler(5)),

    JACKPOT("Jackpot", "Get 10 or more back-to-backs in a single game",
            new BackToBackCountAchievementHandler(10)),

    // VISIT achievements
    BIOME_HOPPER("Biome Hopper", "Visit all basic overworld biomes in one round",
            CollectionAchievementHandler.biomeHandler(Set.of(BiomeGroup.values()))),

    CAVE_HOPPER("Cave Hopper", "Visit all cave biomes in one round",
            CollectionAchievementHandler.caveBiomeHandler(Set.of(
                    Biome.DEEP_DARK, Biome.DRIPSTONE_CAVES, Biome.LUSH_CAVES, Biome.SULFUR_CAVES))),

    ALWAYS_ON_THE_GO("Always On The Go", "Visit all three dimensions in one round",
            CollectionAchievementHandler.dimensionHandler(Set.of("world", "world_nether", "world_the_end"))),

    PALE_PLEASE("Pale Please", "Use 10 different antimatter teleporters in one game",
            new AntimatterTeleporterUsesAchievementHandler(10)),

    NO_SHORTCUTS("No Shortcuts", "Finish a game without entering the Antimatter Teleporter",
            new NoAntimatterAchievementHandler()),

    IT_IS_BEAUTIFUL("It is beautiful", "Finish a game without leaving the Overworld",
            new NoOverworldExitAchievementHandler()),

    // SKIP achievements
    UNLUCKY("Unlucky", "Skip 3 items in a row",
            new SkipAchievementHandler(3, true, 0)),

    FUCK_THIS("Fuck this", "Skip an item within 3 seconds of receiving it",
            new SkipAchievementHandler(1, false, 3)),

    // ACTION achievements
    CHICOT("Chicot", "Complete a round without dying",
            new DeathCounterAchievementHandler(0)),

    CONNOISSEUR("Connoisseur", "Eat Cavendish",
            new EatingAchievementHandler(1, new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, "cavendish", null))),

    THANK_YOU("Thank you", "Trade with the wandering trader 10 times in one round",
            new TradingAchievementHandler(10)),

    A_BALANCED_INVENTORY("A Balanced Inventory", "Fill every slot in your inventory (including backpack if enabled)",
            new InventoryFullAchievementHandler()),

    HONEY_HONEY("Honey, honey, how you thrill me, aha, honey honey", "Harvest 2 full beehives with shears",
            new BeehiveAchievementHandler(2)),

    GOLD_GOLD_GOLD("Gold Gold Gold", "Use the Wheel of Fortune 15 times in one game",
            new WheelOfFortuneUsesAchievementHandler(15)),

    // LOOT achievements
    LEGENDARY("Legendary", "Find a Legendary item in the Antimatter Depths",
            new LootAchievementHandler(1, CustomItem.customData("fib:fib_item", "legendary_template"), false)),

    WILL_IT_BREAK("Will it break?", "Find Cavendish in a loot chest",
            new LootAchievementHandler(1, new CustomItem(Material.ENCHANTED_GOLDEN_APPLE, "cavendish", null), false)),

    BELIEVER("Believer", "Find your currently needed item in a loot chest",
            new LootAchievementHandler(1, null, true)),

    // META achievement
    COMPLETIONIST("Completionist++", "Complete all achievements",
            new CompletionistAchievementHandler());

    private final String title;
    private final String description;
    private final AchievementHandler<?> handler;

    Achievements(String title, String description, AchievementHandler<?> handler) {
        this.title = title;
        this.description = description;
        this.handler = handler;
    }
}