package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandFixLocate;
import forceitembattle.manager.LocatorManager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Locator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /fixlocate}: getting rid of a locator boss bar that has outlived its usefulness.
 *
 * <p>The interesting part is what happens with no argument, because the answer depends on how many
 * locators are running: exactly one is dismissed outright, and several produce a picker instead of
 * a guess. Dismissing an arbitrary one of several is the failure mode, and it is silent â€” the
 * player sees a locator go away and cannot tell it was the wrong one.
 *
 * <p>The picker's own buttons run {@code /fixlocate <id>}, so the explicit form is the same code
 * path a click takes. That form matches on either the id or the structure's display name, which is
 * what makes a typed {@code /fixlocate Trial Chambers} work alongside the clicked
 * {@code /fixlocate trial_chamber}.
 */
class CommandFixLocateTest {

    private ServerMock server;
    private LocatorManager locators;
    private CommandFixLocate command;
    private final Map<String, Locator> active = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.active.clear();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.locators = mock(LocatorManager.class);
        when(plugin.getLocatorManager()).thenReturn(this.locators);
        when(this.locators.getActiveLocators(any())).thenReturn(this.active);

        this.command = new CommandFixLocate(plugin);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private PlayerMock join(String name) {
        return this.server.addPlayer(name);
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "fixlocate", args);
    }

    /** Registers one running locator under {@code id}, displayed as {@code name}. */
    private void running(String id, String name) {
        this.active.put(id, new Locator(id, name, CustomMaterials.values()[0],
                Locator.Type.STRUCTURE, Locator.Use.values()[0], 16,
                Color.LIME, "<green>"));
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class WithNothingRunning {

        @Test
        void thePlayerIsToldSoAndNothingIsDismissed() {
            PlayerMock player = join("Understudy1");

            run(player);

            assertSaid(player, "no active locators");
            verify(locators, never()).dismiss(any(), any());
            verify(locators, never()).dismissAll(any());
        }

        /** The empty check runs first, so even an explicit name gets the same answer. */
        @Test
        void anExplicitNameGetsTheSameAnswer() {
            PlayerMock player = join("Understudy1");

            run(player, "trial_chamber");

            assertSaid(player, "no active locators");
            verify(locators, never()).dismiss(any(), any());
        }

        @Test
        void soDoesAll() {
            PlayerMock player = join("Understudy1");

            run(player, "all");

            assertSaid(player, "no active locators");
            verify(locators, never()).dismissAll(any());
        }
    }

    @Nested
    class WithNoArgument {

        /** One running locator is unambiguous, so it goes without asking. */
        @Test
        void theOnlyLocatorIsDismissedOutright() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            run(player);

            verify(locators).dismiss(player, "trial_chamber");
            assertSaid(player, "Trial Chambers");
        }

        /** Several is ambiguous, and guessing is the failure mode. */
        @Test
        void severalProduceAPickerRatherThanAGuess() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");
            running("ancient_city", "Ancient City");

            run(player);

            verify(locators, never()).dismiss(any(), any());
            String said = screenOf(player);
            assertTrue(said.contains("Trial Chambers"), said);
            assertTrue(said.contains("Ancient City"), said);
            assertTrue(said.contains("Dismiss all"), "the picker offers the sweep too:\n" + said);
        }
    }

    @Nested
    class WithAName {

        @Test
        void anIdIsDismissed() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");
            running("ancient_city", "Ancient City");

            run(player, "ancient_city");

            verify(locators).dismiss(player, "ancient_city");
        }

        /** The display name works too, which is what makes a typed name usable. */
        @Test
        void aStructureNameIsDismissed() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            run(player, "Trial", "Chambers");

            verify(locators).dismiss(player, "trial_chamber");
        }

        @Test
        void eitherFormIsCaseInsensitive() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            run(player, "TRIAL_CHAMBER");

            verify(locators).dismiss(player, "trial_chamber");
        }

        /** A name that matches nothing running says so and then offers the picker. */
        @Test
        void anUnmatchedNameFallsBackToThePicker() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            run(player, "ancient_city");

            verify(locators, never()).dismiss(any(), any());
            String said = screenOf(player);
            assertTrue(said.contains("no active"), said);
            assertTrue(said.contains("Trial Chambers"), "and shows what there is:\n" + said);
        }
    }

    @Nested
    class All {

        @Test
        void everyLocatorGoes() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");
            running("ancient_city", "Ancient City");
            when(locators.dismissAll(any())).thenReturn(2);

            run(player, "all");

            verify(locators).dismissAll(player);
            // The count is coloured, so it and the noun are separated by a section sign.
            String said = screenOf(player);
            assertTrue(said.contains("2"), said);
            assertTrue(said.contains("locators."), said);
        }

        @Test
        void theCountIsSingularWhenThereWasOne() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");
            when(locators.dismissAll(any())).thenReturn(1);

            run(player, "all");

            String said = screenOf(player);
            assertTrue(said.contains("locator."), said);
            assertFalse(said.contains("locators."), "one locator is not plural:\n" + said);
        }

        @Test
        void itIsCaseInsensitiveToo() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            run(player, "ALL");

            verify(locators).dismissAll(player);
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void theRunningLocatorsAndAllAreOffered() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");

            List<String> offered = command.onTabComplete(player, null, "fixlocate",
                    new String[]{""});

            assertEquals(List.of("trial_chamber", "all"), offered);
        }

        @Test
        void whatIsTypedNarrowsTheOffer() {
            PlayerMock player = join("Understudy1");
            running("trial_chamber", "Trial Chambers");
            running("ancient_city", "Ancient City");

            assertEquals(List.of("ancient_city"),
                    command.onTabComplete(player, null, "fixlocate", new String[]{"anc"}));
        }

        /** The console has no locators, so it is offered nothing rather than everyone's. */
        @Test
        void theConsoleIsOfferedNothing() {
            assertTrue(command.onTabComplete(server.getConsoleSender(), null, "fixlocate",
                    new String[]{""}).isEmpty());
            verifyNoInteractions(locators);
        }
    }
}
