package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ceremony.ResultStage;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.PlayerOutfitter;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameItems;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundPhase;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The backpack a player is handed back when they respawn. */
class RespawnBackpackTest {

    private ServerMock server;
    private Roster roster;
    private GameSettings settings;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roster = new Roster();
        this.settings = mock(GameSettings.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    private static long backpacksIn(PlayerMock player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(GameItems::isBackpack)
                .count();
    }

    @Nested
    class Restoring {

        @Test
        void anEmptySlotEightGetsTheBackpack() {
            ForceItemPlayer entry = join("Understudy1");
            PlayerMock player = (PlayerMock) entry.player();

            PlayerOutfitter.restoreBackpack(player, entry);

            assertTrue(GameItems.isBackpack(player.getInventory().getItem(8)));
        }

        /** keepInventory on: what the player keeps in slot 8 is theirs, not the backpack's. */
        @Test
        void whateverIsInSlotEightIsKept() {
            ForceItemPlayer entry = join("Understudy1");
            PlayerMock player = (PlayerMock) entry.player();
            player.getInventory().setItem(8, new ItemStack(Material.DIAMOND, 5));

            PlayerOutfitter.restoreBackpack(player, entry);

            assertEquals(new ItemStack(Material.DIAMOND, 5), player.getInventory().getItem(8));
            assertEquals(1, backpacksIn(player), "the backpack goes somewhere else");
        }

        /** keepInventory on: the backpack survived the death, so a second one would be a duplicate. */
        @Test
        void aPlayerWhoKeptTheirBackpackIsNotGivenASecond() {
            ForceItemPlayer entry = join("Understudy1");
            PlayerMock player = (PlayerMock) entry.player();
            player.getInventory().setItem(20, GameItems.backpack(entry));

            PlayerOutfitter.restoreBackpack(player, entry);

            assertEquals(1, backpacksIn(player));
        }
    }

    @Nested
    class OnRespawn {

        private PlayerMock respawnWithBackpackSetting(boolean backpackOn) {
            when(settings.isSettingEnabled(GameSetting.BACKPACK)).thenReturn(backpackOn);
            RoundPhase phase = new RoundPhase();
            phase.moveTo(GameState.MID_GAME);

            PlayerLifecycleListener listener = new PlayerLifecycleListener(roster, mock(FIBServiceClient.class),
                    phase, mock(Gamemanager.class), mock(ScoreboardManager.class), settings,
                    mock(TeamsManager.class), mock(TimerManager.class), mock(ResultStage.class));
            server.getPluginManager().registerEvents(listener, MockBukkit.createMockPlugin());

            // respawn() without a death first: MockBukkit's death drops hold nulls for empty slots,
            // which Paper's never do, so the death handler is not what this is about.
            PlayerMock player = (PlayerMock) join("Understudy1").player();
            player.respawn();
            return player;
        }

        @Test
        void withBackpacksOffNoBackpackIsHandedOut() {
            PlayerMock player = respawnWithBackpackSetting(false);

            assertEquals(0, backpacksIn(player));
        }

        @Test
        void withBackpacksOnItIsHandedBack() {
            PlayerMock player = respawnWithBackpackSetting(true);

            assertEquals(1, backpacksIn(player));
        }
    }
}
