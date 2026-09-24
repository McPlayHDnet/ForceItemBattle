package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import forceitembattle.fairplay.ModDetections;
import forceitembattle.fairplay.ModFinding;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class XaeroMinimapListenerTest extends ListenerTestBase {

    private ModDetections modDetections;
    private XaeroMinimapListener listener;
    private PlayerMock player;
    private PlayerMock bystander;

    @BeforeEach
    void setUpListener() {
        this.modDetections = new ModDetections();
        this.listener = new XaeroMinimapListener(this.modDetections);
        this.player = player("Understudy1");
        this.bystander = player("Understudy2");
    }

    private void register(String channel) {
        this.listener.onRegisterChannel(new PlayerRegisterChannelEvent(this.player, channel));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void assertDisabledAndRecorded() {
        assertEquals(XaeroMinimapListener.DISABLE_MINIMAP, plain(this.player.nextComponentMessage()));
        assertEquals(XaeroMinimapListener.FAIR_PLAY, plain(this.player.nextComponentMessage()));
        assertNull(this.player.nextComponentMessage());
        assertNull(this.bystander.nextComponentMessage());
        assertEquals(List.of(new ModDetections.Detection(this.player.getUniqueId(), "Understudy1", ModFinding.XAERO_MINIMAP)),
                this.modDetections.all());
    }

    @Test
    void aChannelRegisteredAfterJoinDisablesTheMinimapAndRecordsIt() {
        register(XaeroMinimapListener.MINIMAP_CHANNEL);

        assertDisabledAndRecorded();
    }

    @Test
    void aChannelAlreadyRegisteredAtJoinIsCaughtByTheJoin() {
        PlayerMock withChannel = spy(this.player);
        doReturn(Set.of(XaeroMinimapListener.MINIMAP_CHANNEL)).when(withChannel).getListeningPluginChannels();

        this.listener.onJoin(new PlayerJoinEvent(withChannel, Component.empty()));

        assertDisabledAndRecorded();
    }

    @Test
    void otherChannelsAreIgnored() {
        register("fabric:registry/sync");

        assertNull(this.player.nextComponentMessage());
        assertNull(this.bystander.nextComponentMessage());
    }

    @Test
    void theMinimapIsDisabledAgainAfterARejoin() {
        register(XaeroMinimapListener.MINIMAP_CHANNEL);
        assertDisabledAndRecorded();

        register(XaeroMinimapListener.MINIMAP_CHANNEL);
        assertNull(this.player.nextComponentMessage());

        this.listener.onQuit(new PlayerQuitEvent(this.player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));
        register(XaeroMinimapListener.MINIMAP_CHANNEL);
        assertDisabledAndRecorded();
    }
}
