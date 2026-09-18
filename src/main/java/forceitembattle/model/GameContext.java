package forceitembattle.model;

import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;

/**
 * The kind of round being played, from the point of view of one player.
 *
 * <p>Built from the settings rather than the plugin: a model has no business knowing what a plugin
 * is, and reading five booleans never needed the other twenty-two managers to be reachable. It took
 * a {@code ForceItemBattle} only because that was the shape every constructor already had.
 */
public record GameContext(boolean teamGame, boolean runMode, boolean eventDisabled,
                          boolean statsEnabled, boolean backpackEnabled) {

    public static GameContext of(GameSettings settings, ForceItemPlayer forceItemPlayer) {
        return new GameContext(
                forceItemPlayer.isInTeam(),
                settings.isSettingEnabled(GameSetting.RUN),
                !settings.isSettingEnabled(GameSetting.EVENT),
                settings.isSettingEnabled(GameSetting.STATS),
                settings.isSettingEnabled(GameSetting.BACKPACK)
        );
    }
}
