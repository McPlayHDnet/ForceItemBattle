package forceitembattle.model;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;

public record GameContext(boolean teamGame, boolean runMode, boolean eventDisabled,
                          boolean statsEnabled, boolean backpackEnabled) {

    public static GameContext of(ForceItemBattle plugin, ForceItemPlayer forceItemPlayer) {
        return new GameContext(
                forceItemPlayer.currentTeam() != null,
                plugin.getSettings().isSettingEnabled(GameSetting.RUN),
                !plugin.getSettings().isSettingEnabled(GameSetting.EVENT),
                plugin.getSettings().isSettingEnabled(GameSetting.STATS),
                plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)
        );
    }
}
