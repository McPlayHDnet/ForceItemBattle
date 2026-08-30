package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void thePodiumGetsGoldSilverAndBronze() {
        assertEquals("<gold>", Text.placeColor(1));
        assertEquals("<gray>", Text.placeColor(2));
        assertEquals("<red>", Text.placeColor(3));
    }

    @Test
    void everyOtherPlaceIsPlainWhite() {
        assertEquals("<white>", Text.placeColor(4));
        assertEquals("<white>", Text.placeColor(99));
        assertEquals("<white>", Text.placeColor(0));
    }

    @Test
    void miniMessageDeserialisesWithoutSpecialSetup() {
        assertNotNull(Text.of("<gold>hello"));
    }
}
