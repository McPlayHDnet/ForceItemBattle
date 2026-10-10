package forceitembattle.model;

import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;

public record GameContext(boolean teamGame, boolean runMode, boolean eventDisabled,
                          boolean statsEnabled, boolean backpackEnabled, boolean mirrored) {

    public static GameContext of(GameSettings settings, ForceItemPlayer forceItemPlayer) {
        return new GameContext(
                forceItemPlayer.isInTeam(),
                settings.isSettingEnabled(GameSetting.RUN),
                !settings.isSettingEnabled(GameSetting.EVENT),
                settings.isSettingEnabled(GameSetting.STATS),
                settings.isSettingEnabled(GameSetting.BACKPACK),
                settings.isMirrorBattle()
        );
    }
}
