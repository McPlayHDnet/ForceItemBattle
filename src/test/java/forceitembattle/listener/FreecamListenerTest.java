package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import forceitembattle.fairplay.ModDetections;
import forceitembattle.fairplay.ModFinding;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.sign.Side;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class FreecamListenerTest extends ListenerTestBase {

    private ModDetections modDetections;
    private FreecamListener listener;
    private PlayerMock player;
    private PlayerMock bystander;
    private BlockPosition probeSign;

    @BeforeEach
    void setUpListener() {
        this.modDetections = new ModDetections();
        this.listener = new FreecamListener(this.modDetections);
        this.player = spy(player("Understudy1"));
        doNothing().when(this.player).sendBlockChange(any(), any(org.bukkit.block.data.BlockData.class));
        doNothing().when(this.player).sendBlockUpdate(any(), any());
        doNothing().when(this.player).openVirtualSign(any(), any());
        this.player.teleport(at(10.5, 70, -3.5));
        this.bystander = player("Understudy2");
        this.probeSign = Position.block(10, this.world.getMinHeight(), -4);
    }

    private void loadWorld() {
        this.listener.onClientLoaded(new PlayerClientLoadedWorldEvent(this.player, false));
    }

    private UncheckedSignChangeEvent respond(BlockPosition position, String line) {
        UncheckedSignChangeEvent event = new UncheckedSignChangeEvent(this.player, position, Side.FRONT,
                List.of(Component.text(line), Component.empty(), Component.empty(), Component.empty()));
        this.listener.onSignResponse(event);
        return event;
    }

    private List<ModFinding> findings() {
        return this.modDetections.all().stream().map(ModDetections.Detection::finding).toList();
    }

    private String nextBroadcast() {
        Component message = this.bystander.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void theProbeOpensAVirtualSignBelowThePlayerAndClosesItShortlyAfter() {
        loadWorld();

        verify(this.player).openVirtualSign(this.probeSign, Side.FRONT);
        verify(this.player, never()).closeInventory();
        tick(2);
        verify(this.player).closeInventory();
    }

    @Test
    void aResolvedKeyIsRecordedWithoutAnnouncingIt() {
        loadWorld();

        UncheckedSignChangeEvent event = respond(this.probeSign, "Toggle Freecam");

        assertTrue(event.isCancelled());
        assertEquals(List.of(ModFinding.FREECAM_INSTALLED), findings());
        assertNull(nextBroadcast());
    }

    @Test
    void theRawKeyIsAVanillaClient() {
        loadWorld();

        UncheckedSignChangeEvent event = respond(this.probeSign, FreecamListener.PROBE_KEY);

        assertTrue(event.isCancelled());
        assertEquals(List.of(), findings());
    }

    @Test
    void aSignEditElsewhereIsNotTheProbe() {
        loadWorld();

        UncheckedSignChangeEvent event = respond(Position.block(0, 64, 0), "Toggle Freecam");

        assertFalse(event.isCancelled());
        assertEquals(List.of(), findings());
    }

    @Test
    void aResponseAfterTheTimeoutIsIgnored() {
        loadWorld();
        tick(100);

        UncheckedSignChangeEvent event = respond(this.probeSign, "Toggle Freecam");

        assertFalse(event.isCancelled());
        assertEquals(List.of(), findings());
    }

    @Test
    void thePlayerIsProbedOncePerSession() {
        loadWorld();
        loadWorld();
        verify(this.player, times(1)).openVirtualSign(any(), any());

        this.listener.onQuit(new PlayerQuitEvent(this.player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));
        loadWorld();
        verify(this.player, times(2)).openVirtualSign(any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-420, -421, -429})
    void usingTheCameraEntityIsAnnouncedAsFreecamInUse(int entityId) {
        this.listener.onUseUnknownEntity(new PlayerUseUnknownEntityEvent(this.player, entityId, true, EquipmentSlot.HAND, null));

        assertEquals("» Fair Play ┃ Freecam in use by Understudy1.", nextBroadcast());
        assertEquals(List.of(ModFinding.FREECAM_IN_USE), findings());
    }

    @Test
    void useIsAnnouncedOnce() {
        PlayerUseUnknownEntityEvent use = new PlayerUseUnknownEntityEvent(this.player, -420, true, EquipmentSlot.HAND, null);
        this.listener.onUseUnknownEntity(use);
        nextBroadcast();

        this.listener.onUseUnknownEntity(use);

        assertNull(nextBroadcast());
    }

    @ParameterizedTest
    @ValueSource(ints = {-419, -430, 1234})
    void otherUnknownEntitiesAreIgnored(int entityId) {
        this.listener.onUseUnknownEntity(new PlayerUseUnknownEntityEvent(this.player, entityId, false, EquipmentSlot.HAND, null));

        assertNull(nextBroadcast());
        assertEquals(List.of(), findings());
    }
}
