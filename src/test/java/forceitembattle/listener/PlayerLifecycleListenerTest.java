package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.Admission;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import java.util.HashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * That an arriving player is put into the state their {@link Admission} says they are in.
 * {@code RosterTest} pins the admission table itself; this pins the half that acts on the answer.
 *
 * <p>Menu items are deliberately not asserted: they are handed out through a collaborator that is a
 * mock here. {@code PlayerOutfitterTest} owns the contents of each state; this owns which is entered.
 */
class PlayerLifecycleListenerTest extends ListenerTestBase {

    private Roster roster;
    private Gamemanager gamemanager;
    private PlayerLifecycleListener listener;

    @BeforeEach
    void setUpListener() {
        this.roster = new Roster();
        this.gamemanager = mock(Gamemanager.class);

        TimerManager timerManager = mock(TimerManager.class);
        when(timerManager.getBossBar()).thenReturn(new HashMap<>());

        this.listener = new PlayerLifecycleListener(
                this.roster,
                mock(FIBServiceClient.class),
                this.roundPhase,
                this.gamemanager,
                mock(ScoreboardManager.class),
                mock(GameSettings.class),
                mock(TeamsManager.class),
                timerManager);
    }

    /**
     * Someone arriving in the state a round leaves you in: hurt, mounted, wearing the coloured tab
     * name the scoreboard gave them, and standing wherever they logged out. Every one of those is
     * something {@code finishGame} clears.
     */
    private PlayerMock arrivingMidRoundShaped(String name) {
        PlayerMock player = player(name);
        player.teleport(at(400, 72, -400));
        player.setHealth(6);
        player.setLevel(30);
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIRT));
        player.playerListName(Component.text(name, NamedTextColor.RED));

        Entity mount = this.world.spawnEntity(player.getLocation(), EntityType.PIG);
        player.addPassenger(mount);
        return player;
    }

    /** Puts them on the roster, so {@code admit} sees a returning player rather than a stranger. */
    private void onRoster(PlayerMock player) {
        this.roster.add(player.getUniqueId(), new ForceItemPlayer(player, Material.DIRT, 0, 0));
    }

    private void join(PlayerMock player) {
        this.listener.onPlayerJoin(new PlayerJoinEvent(player, Component.empty()));
    }

    private static String tabNameOf(PlayerMock player) {
        return PlainTextComponentSerializer.plainText().serialize(player.playerListName());
    }

    private void assertAtWorldSpawn(PlayerMock player) {
        Location spawn = this.world.getSpawnLocation();
        Location actual = player.getLocation();
        assertEquals(spawn.getBlockX(), actual.getBlockX(), "x");
        assertEquals(spawn.getBlockY(), actual.getBlockY(), "y");
        assertEquals(spawn.getBlockZ(), actual.getBlockZ(), "z");
    }

    /**
     * A player reaching {@code RESULT_SCREEN} is by definition someone {@code finishGame} never
     * touched — quitting at {@code END_GAME} releases your roster spot, so the only way here is to
     * have quit <em>during</em> the round. Every reset {@code finishGame} performs is still owed.
     */
    @Nested
    class ResultScreen {

        @BeforeEach
        void aRoundThatEndedWhileTheyWereGone() {
            phase(GameState.END_GAME);
        }

        @Test
        void theyArriveAtFullHealth() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertEquals(20.0, player.getHealth(),
                    "a result screen is not somewhere you arrive on three hearts");
        }

        @Test
        void whateverTheyWereRidingIsGone() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertTrue(player.getPassengers().isEmpty(),
                    "finishGame removes passengers; the rejoin path must too");
        }

        /** The scoreboard's team colour outlives the round unless the tab name is reset. */
        @Test
        void theirTabNameIsPlainAgain() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertEquals("Understudy1", tabNameOf(player));
            assertEquals(Component.text("Understudy1"), player.playerListName(),
                    "the colour has to go, not just the letters match");
        }

        @Test
        void theyAreBroughtToTheResultSpawn() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertAtWorldSpawn(player);
        }

        /**
         * Bukkit will not teleport an entity carrying a passenger — {@code teleport} returns false
         * and does nothing — so dismount has to come first. The fixture mounts a pig for exactly this
         * reason; without one the assertion above passes either way.
         */
        @Test
        void beingMountedDoesNotStrandThemWhereTheRoundEnded() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);
            assertFalse(player.getPassengers().isEmpty(), "the fixture must actually be mounted");

            join(player);

            assertAtWorldSpawn(player);
        }

        /** Clearing the inventory does not clear the level bar above it. */
        @Test
        void theirLevelIsReset() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertEquals(0, player.getLevel());
        }

        @Test
        void theyAreInCreative() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertEquals(GameMode.CREATIVE, player.getGameMode());
        }

        /** Cleared, not left empty: the result screen's own buttons go in straight after. */
        @Test
        void theRoundsLootDoesNotComeWithThem() {
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertFalse(player.getInventory().contains(Material.DIRT),
                    "what they were carrying mid-round is gone");
        }
    }

    @Nested
    class TheOtherAdmissions {

        @Test
        void aStrangerDuringARoundBecomesASpectator() {
            phase(GameState.MID_GAME);
            PlayerMock player = arrivingMidRoundShaped("Latecomer");

            join(player);

            assertEquals(GameMode.SPECTATOR, player.getGameMode());
            assertEquals(0, player.getLevel(), "and loses the level they walked in with");
        }

        @Test
        void aStrangerDuringTheCountdownBecomesASpectatorToo() {
            phase(GameState.STARTING);
            PlayerMock player = arrivingMidRoundShaped("Latecomer");

            join(player);

            assertEquals(GameMode.SPECTATOR, player.getGameMode());
        }

        @Test
        void aStrangerBeforeARoundIsALobbyPlayer() {
            phase(GameState.PRE_GAME);
            PlayerMock player = arrivingMidRoundShaped("Newcomer");

            join(player);

            assertEquals(GameMode.ADVENTURE, player.getGameMode());
            assertEquals(20.0, player.getHealth());
        }

        /**
         * {@code END_GAME} shares the {@code LOBBY} arm with {@code PRE_GAME}, so someone who never
         * played this round gets lobby treatment while the result screen is still up.
         */
        @Test
        void andSoIsAStrangerArrivingOnTheResultScreen() {
            phase(GameState.END_GAME);
            PlayerMock player = arrivingMidRoundShaped("Newcomer");

            join(player);

            assertEquals(GameMode.ADVENTURE, player.getGameMode());
        }

        /** Deliberately writes nothing: {@code applyStartSetup} outfits them when the countdown ends. */
        @Test
        void reconnectingDuringTheCountdownIsLeftAlone() {
            phase(GameState.STARTING);
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            assertEquals(30, player.getLevel(), "nothing about their state is written");
            assertEquals(6.0, player.getHealth());
        }

        @Test
        void aParticipantRejoiningARunningRoundIsRestored() {
            phase(GameState.MID_GAME);
            PlayerMock player = arrivingMidRoundShaped("Understudy1");
            onRoster(player);

            join(player);

            org.mockito.Mockito.verify(gamemanager).applyStartSetup(player);
        }
    }
}
