package forceitembattle.util;

import lombok.Getter;

@Getter
public enum Prefix {

    LOCATOR("<dark_purple>", "Locator"),
    POSITION("<gold>", "Position"),
    RANDOM_EVENT("<light_purple>", "Event");

    private final String value;

    Prefix(String color, String label) {
        this.value = "<dark_gray>» " + color + label + " <dark_gray>┃ ";
    }

    @Override
    public String toString() {
        return this.value;
    }
}
