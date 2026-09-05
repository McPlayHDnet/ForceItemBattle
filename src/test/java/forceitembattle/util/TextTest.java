package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    @Test
    void tagArgumentEscapesApostrophesAndBackslashes() {
        assertEquals("That\\'s a Rock, Jim", Text.tagArgument("That's a Rock, Jim"));
        assertEquals("a\\\\b", Text.tagArgument("a\\b"));
        // Backslash handled before quotes, or the quote's new backslash gets doubled in turn.
        assertEquals("\\\\\\'", Text.tagArgument("\\'"));
        assertEquals("nothing to do", Text.tagArgument("nothing to do"));
    }

    /**
     * The bug this guards: an unescaped apostrophe closes the tag argument early, MiniMessage then
     * fails to parse the tag at all and falls back to emitting the raw markup as literal text, so
     * the whole line lands in chat as {@code <hover:show_text:'<dark_aqua>That's a Rock...}.
     */
    @Test
    void anUnescapedApostropheLeaksTheRawMarkupIntoTheMessage() {
        String rendered = plain("<hover:show_text:'<dark_aqua>That's a Rock, Jim'>[That's a Rock, Jim]</hover>");

        assertTrue(rendered.contains("<hover:show_text:"),
                "expected the unescaped form to leak markup, but got: " + rendered);
    }

    @Test
    void anEscapedApostropheRendersOnlyTheVisibleText() {
        String title = "That's a Rock, Jim";
        String rendered = plain("<hover:show_text:'<dark_aqua>" + Text.tagArgument(title) + "'>["
                + title + "]</hover>");

        assertEquals("[That's a Rock, Jim]", rendered);
    }

    /** The other affected title, and the one whose apostrophe sits mid-word. */
    @Test
    void theOtherApostropheTitleAlsoSurvives() {
        String title = "It's so empty";
        String rendered = plain("<hover:show_text:'<dark_aqua>" + Text.tagArgument(title)
                + "<newline><gray>Collect 3 end items in a row'><dark_aqua>[" + title + "]</hover>");

        assertEquals("[It's so empty]", rendered);
    }

    private static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(Text.of(miniMessage));
    }
}
