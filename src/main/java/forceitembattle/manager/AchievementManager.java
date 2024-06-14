package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.achievements.Achievement;
import forceitembattle.settings.achievements.AchievementTriggers;
import forceitembattle.util.ForceItemPlayerStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AchievementManager {

    private final ForceItemBattle forceItemBattle;

    private List<Achievement> achievementsList;

    public AchievementManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.achievementsList = new ArrayList<>();

        this.loadAchievements();
    }

    private void loadAchievements() {

        final long SECOND = 1000;
        final long MINUTE = 60 * SECOND;
        final long HOUR = 60 * MINUTE;

        Achievement itemCollector = Achievement.builder()
                .title("Item Collector")
                .description(List.of("", "<gray>Find 30 items in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        30,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement itemGatherer = Achievement.builder()
                .title("Item Gatherer")
                .description(List.of("", "<gray>Find 40 items in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        40,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement itemHoarder = Achievement.builder()
                .title("Item Hoarder")
                .description(List.of("", "<gray>Find 50 items in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        50,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement ultimateCollector = Achievement.builder()
                .title("Ultimate Collector")
                .description(List.of("", "<gray>Find 60 items in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        60,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement oneDimensionWonder = Achievement.builder()
                .title("One Dimension Wonder")
                .description(List.of("", "<gray>Get 15 overworld items in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        15,
                                        ForceItemBattle.getInstance().getItemDifficultiesManager().getOverworldItems().stream().toList(),
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        true
                                )
                        )
                ).build();

        Achievement infiniteFire = Achievement.builder()
                .title("Infinite Fire")
                .description(List.of("", "<gray>Get 5 nether items in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        5,
                                        ForceItemBattle.getInstance().getItemDifficultiesManager().getNetherItems(),
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        true
                                )
                        )
                ).build();

        Achievement itsSoEmpty = Achievement.builder()
                .title("It's so empty")
                .description(List.of("", "<gray>Get 3 end items in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        3,
                                        ForceItemBattle.getInstance().getItemDifficultiesManager().getEndItems(),
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        true
                                )
                        )
                ).build();

        Achievement speedCollector = Achievement.builder()
                .title("Speed Collector")
                .description(List.of("", "<gray>Collect 7 items within the first 5 minutes of the game", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        7,
                                        null,
                                        5 * MINUTE,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement quickGrab = Achievement.builder()
                .title("Quick Grab")
                .description(List.of("", "<gray>Collect an item within the first 30 seconds of round start", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        1,
                                        null,
                                        30 * SECOND,
                                        0,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement wasItWorthwhile = Achievement.builder()
                .title("Was it worthwhile?")
                .description(List.of("", "<gray>Collect an item that took you 15 minutes to find", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        1,
                                        null,
                                        Long.MAX_VALUE,
                                        15 * MINUTE,
                                        0,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement backToBack = Achievement.builder()
                .title("Back-to-Back")
                .description(List.of("", "<gray>Get 1 b2b items", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        1,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement doubleTrouble = Achievement.builder()
                .title("Double Trouble")
                .description(List.of("", "<gray>Get 2 b2b items", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        2,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement ohBabyATriple = Achievement.builder()
                .title("Oh Baby A Triple!")
                .description(List.of("", "<gray>Get 3 b2b items", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        3,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement fourLeafClover = Achievement.builder()
                .title("Four-leaf Clover")
                .description(List.of("", "<gray>Get 4 b2b items", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        4,
                                        0,
                                        0,
                                        false,
                                        false,
                                        false
                                )
                        )
                ).build();

        Achievement dejaVu = Achievement.builder()
                .title("Déjà Vu")
                .description(List.of("", "<gray>Get the same item b2b", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        1,
                                        0,
                                        0,
                                        false,
                                        true,
                                        false
                                )
                        )
                ).build();



        Achievement biomeHopper = Achievement.builder()
                .title("Biome Hopper")
                .description(List.of("", "<gray>Visit all overworld basic biomes in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.VisitedRequirement(
                                        true
                                )
                        )
                ).build();

        Achievement alwaysOnTheGo = Achievement.builder()
                .title("Always On The Go")
                .description(List.of("", "<gray>Visit all dimensions in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.VisitedRequirement(
                                        false
                                )
                        )
                ).build();



        Achievement chicot = Achievement.builder()
                .title("Chicot")
                .description(List.of("", "<gray>Do not die in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 0, false, false, false, false, true, false, false)
                        )
                ).build();

        Achievement unlucky = Achievement.builder()
                .title("Unlucky")
                .description(List.of("", "<gray>Skip 3 times in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        3,
                                        0,
                                        false,
                                        false,
                                        true
                            )
                        )
                ).build();

        Achievement luckyRow = Achievement.builder()
                .title("Lucky Row")
                .description(List.of("", "<gray>Don't skip for 15 items in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        15,
                                        true,
                                        false,
                                        true
                                )
                        )
                ).build();

        Achievement youGetWhatYouGet = Achievement.builder()
                .title("You Get What You Get")
                .description(List.of("", "<gray>Don't skip for 20 items in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.ObtainedItemRequirement(
                                        null,
                                        0,
                                        null,
                                        Long.MAX_VALUE,
                                        0,
                                        0,
                                        0,
                                        20,
                                        true,
                                        false,
                                        true
                                )
                        )
                ).build();

        Achievement connoisseur = Achievement.builder()
                .title("Connoisseur")
                .description(List.of("", "<gray>Eat Cavendish", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 0, false, false, false, true, false, false, false)
                        )
                ).build();

        Achievement thankYou = Achievement.builder()
                .title("Thank You")
                .description(List.of("", "<gray>Use the wandering trader for 10 trades in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(10, 0, 0, false, false, false, false, false, false, false)
                        )
                ).build();

        Achievement wheelOfFortune = Achievement.builder()
                .title("Wheel of Fortune")
                .description(List.of("", "<gray>Find a Legendary", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 0, true, false, false, false, false, false, false)
                        )
                ).build();

        Achievement willItBreak = Achievement.builder()
                .title("Will it break?")
                .description(List.of("", "<gray>Find Cavendish", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 0, false, true, false, false, false, false, false)
                        )
                ).build();

        Achievement believer = Achievement.builder()
                .title("Believer")
                .description(List.of("", "<gray>Find a needed item from loot chest in 1 round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 0, false, false, true, false, false, false, false)
                        )
                ).build();

        Achievement homeless = Achievement.builder()
                .title("Homeless")
                .description(List.of("", "<gray>Travel for over 10'000 blocks in one round", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 10000, 0, false, false, false, false, false, true, false)
                        )
                ).build();

        Achievement winnerWinner = Achievement.builder()
                .title("Winner Winner Chicken Dinner!")
                .description(List.of("", "<gray>Win 3 games in a row", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.PlayerActionRequirement(0, 0, 3, false, false, false, false, false, false, true)
                        )
                ).build();

        Achievement completionist = Achievement.builder()
                .title("Completionist++")
                .description(List.of("", "<gray>Complete all Achievements", ""))
                .requirements(
                        List.of(
                                new AchievementTriggers.MiscRequirement()
                        )
                ).build();

        this.achievementsList.addAll(Arrays.asList(
                itemCollector,
                itemGatherer,
                itemHoarder,
                ultimateCollector,
                oneDimensionWonder,
                infiniteFire,
                itsSoEmpty,
                speedCollector,
                quickGrab,
                wasItWorthwhile,
                backToBack,
                doubleTrouble,
                ohBabyATriple,
                fourLeafClover,
                dejaVu,
                biomeHopper,
                alwaysOnTheGo,
                chicot,
                unlucky,
                luckyRow,
                youGetWhatYouGet,
                connoisseur,
                thankYou,
                wheelOfFortune,
                willItBreak,
                believer,
                homeless,
                winnerWinner,
                completionist
        ));
    }

    public void addAchievementDone(ForceItemPlayerStats forceItemPlayer, Achievement achievement) {
        if(forceItemPlayer.achievementsDone().contains(achievement.getTitle())) {
            return;
        }

        List<String> achievementsDone = forceItemPlayer.achievementsDone();
        achievementsDone.add(achievement.getTitle());
        forceItemPlayer.setAchievementsDone(achievementsDone);

        this.forceItemBattle.getStatsManager().saveStats();
    }

    public List<Achievement> achievementsList() {
        return achievementsList;
    }
}
