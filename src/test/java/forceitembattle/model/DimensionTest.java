package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class DimensionTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void overworldResolvesWhateverLevelNameTheServerUses() {
        World world = this.server.addSimpleWorld("survival");

        assertEquals(world, Dimension.OVERWORLD.world());
    }
}
