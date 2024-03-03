package forceitembattle.settings.preset;

import forceitembattle.settings.GameSetting;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class GamePreset {

    private String presetName;
    private int countdown;
    private int jokers;
    private int backpackSize;
    private List<GameSetting> gameSettings;

    public GamePreset() {
        this.presetName = "preset-" + UUID.randomUUID();
        this.countdown = 30;
        this.jokers = 3;
        this.backpackSize = 27;
        this.gameSettings = new ArrayList<>();
        for (GameSetting gameSettings : GameSetting.values()) {
            if (gameSettings.defaultValue()) {
                this.gameSettings.add(gameSettings);
            }
        }
    }
}
