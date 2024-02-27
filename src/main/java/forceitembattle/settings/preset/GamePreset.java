package forceitembattle.settings.preset;

import forceitembattle.settings.GameSetting;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GamePreset {

    private String presetName;
    private int countdown, jokers, backpackSize;
    private List<GameSetting> gameSettings;

    public GamePreset() {
        this.presetName = "preset-" + UUID.randomUUID();
        this.countdown = 30;
        this.jokers = 3;
        this.backpackSize = 27;
        this.gameSettings = new ArrayList<>();
        for(GameSetting gameSettings : GameSetting.values()) {
            if(gameSettings.defaultValue()) {
                this.gameSettings.add(gameSettings);
            }
        }
    }

    public String presetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }

    public int countdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public int jokers() {
        return jokers;
    }

    public void setJokers(int jokers) {
        this.jokers = jokers;
    }

    public int backpackSize() {
        return backpackSize;
    }

    public void setBackpackSize(int backpackSize) {
        this.backpackSize = backpackSize;
    }

    public List<GameSetting> gameSettings() {
        return gameSettings;
    }

    public void setGameSettings(List<GameSetting> gameSettings) {
        this.gameSettings = gameSettings;
    }
}
