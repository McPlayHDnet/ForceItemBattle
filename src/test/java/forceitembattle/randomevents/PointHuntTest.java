package forceitembattle.randomevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.util.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

/** The Point Hunt tab-footer block: how long the hunt has left. */
class PointHuntTest {

    private static String rendered(PointHunt hunt) {
        return PlainTextComponentSerializer.plainText().serialize(Text.of(hunt.tabFooterBlock()));
    }

    /** Nothing here reaches a collaborator; the rendering under test is pure. */
    private static EventContext context() {
        return new EventContext(null, null, null, null, null);
    }

    private static PointHunt hunt() {
        return new PointHunt(context());
    }

    @Test
    void theBlockNamesTheEventAndShowsTheFullDurationAtTheStart() {
        assertEquals("\n\nPoint Hunt · 10:00", rendered(hunt()));
    }

    @Test
    void theCountdownTicksDown() {
        PointHunt hunt = hunt();

        for (int i = 0; i < 61; i++) {
            hunt.tick();
        }

        assertTrue(rendered(hunt).endsWith("08:59"), rendered(hunt));
    }

    /** The block is viewer-independent, so the footer builds it once per refresh, not per player. */
    @Test
    void theBlockIsTheSameForEveryone() {
        PointHunt hunt = hunt();

        assertEquals(hunt.tabFooterBlock(), hunt.tabFooterBlock());
    }

    /** Events that have nothing ongoing to report contribute nothing. */
    @Test
    void otherEventsContributeNothingByDefault() {
        assertEquals("", new ItemHunt(context()).tabFooterBlock());
        assertEquals("", new SpecialTrader(context()).tabFooterBlock());
    }
}
