package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class GameBroadcastTest {

    private static final Component FULL = Component.text("a found Dirt");
    private static final Component REDACTED = Component.text("a found an item");

    private PlayerMock finder;
    private PlayerMock rival;
    private ForceItemPlayer owner;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        this.finder = server.addPlayer("a");
        this.rival = server.addPlayer("b");
        this.owner = new ForceItemPlayer(this.finder, Material.DIRT, 0, 0);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void mirrorBattleHidesTheItemFromEveryoneButTheSquad() {
        GameBroadcast.announce(FULL, REDACTED, this.owner, new GameContext(false, false, true, false, false, true));

        assertEquals(FULL, this.finder.nextComponentMessage());
        assertEquals(REDACTED, this.rival.nextComponentMessage());
    }

    @Test
    void outsideMirrorBattleEveryoneSeesTheItem() {
        GameBroadcast.announce(FULL, REDACTED, this.owner, new GameContext(false, false, true, false, false, false));

        assertEquals(FULL, this.rival.nextComponentMessage());
    }

    @Test
    void eventModeTellsOnlyTheSquad() {
        GameBroadcast.announce(FULL, REDACTED, this.owner, new GameContext(false, false, false, false, false, true));

        assertEquals(FULL, this.finder.nextComponentMessage());
        assertEquals(null, this.rival.nextComponentMessage());
    }
}
