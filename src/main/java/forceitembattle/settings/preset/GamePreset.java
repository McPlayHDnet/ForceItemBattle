package forceitembattle.settings.preset;

import forceitembattle.settings.GameSetting;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class GamePreset {

    private String presetName;
    private int countdown, jokers, backpackRows, tradingCooldown;
    private List<GameSetting> gameSettings;

    public GamePreset() {
        this.presetName = "preset-" + UUID.randomUUID();
        this.countdown = 30;
        this.jokers = 3;
        this.backpackRows = 3;
        this.gameSettings = new ArrayList<>();
        for(GameSetting gameSettings : GameSetting.values()) {
            if(gameSettings.defaultValue() instanceof Boolean b) {
                if(b) this.gameSettings.add(gameSettings);
            }
        }
    }

}
