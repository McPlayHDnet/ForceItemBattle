package forceitembattle.achievements;

import lombok.Getter;

@Getter
public enum Trigger {
    OBTAIN_ITEM(true),
    OBTAIN_ITEM_IN_TIME(true),
    BACK_TO_BACK(true),
    VISIT(false),
    SKIP_ITEM(true),
    DYING(false),
    TRADING(false),
    EATING(false),
    LOOT(false),
    BEEHIVE_HARVEST(false),
    INVENTORY_FULL(false),
    WHEEL_OF_FORTUNE(false),
    ANTIMATTER_TELEPORTER(false),
    ACHIEVEMENT(false);

    private final boolean achieveableInTeams;

    Trigger(boolean achieveableInTeams) {
        this.achieveableInTeams = achieveableInTeams;
    }
}
