package forceitembattle.ceremony;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.EntityMock;
import org.mockbukkit.mockbukkit.entity.ItemDisplayMock;
import org.mockbukkit.mockbukkit.entity.MannequinMock;
import org.mockbukkit.mockbukkit.entity.TextDisplayMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * A world whose displays remember their billboard and whose mannequins remember their skin.
 * MockBukkit's {@code DisplayMock} throws on {@code setBillboard}, which its extension reports as a
 * <em>skip</em>, so without this every stage test silently skipped rather than ran; its
 * {@code MannequinMock} throws on {@code setProfile}.
 */
public final class StageWorldMock extends WorldMock {

    /** Stands in for cushions, which the 26.2 test API does not have. */
    public static final SeatSpawner SEATS = at -> at.getWorld().spawn(at, ArmorStand.class);

    private final ServerMock server;

    public StageWorldMock(ServerMock server, String name) {
        super(new WorldCreator(name));
        this.server = server;
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> type, Consumer<? super T> function,
                                      CreatureSpawnEvent.SpawnReason reason, boolean randomizeData,
                                      boolean callEvent) {
        EntityMock entity;
        if (type == ItemDisplay.class) {
            entity = new BillboardItemDisplay(this.server);
        } else if (type == TextDisplay.class) {
            entity = new BillboardTextDisplay(this.server);
        } else if (type == Mannequin.class) {
            entity = new SkinnedMannequin(this.server);
        } else {
            return super.spawn(location, type, function, reason, randomizeData, callEvent);
        }

        entity.setLocation(location);
        this.server.registerEntity(entity);
        T spawned = type.cast(entity);
        if (function != null) {
            function.accept(spawned);
        }
        return spawned;
    }

    private static final class BillboardItemDisplay extends ItemDisplayMock {
        private Display.Billboard billboard = Display.Billboard.FIXED;

        BillboardItemDisplay(ServerMock server) {
            super(server, UUID.randomUUID());
        }

        @Override
        public Display.Billboard getBillboard() {
            return this.billboard;
        }

        @Override
        public void setBillboard(Display.Billboard billboard) {
            this.billboard = billboard;
        }
    }

    private static final class SkinnedMannequin extends MannequinMock {
        private ResolvableProfile profile;

        SkinnedMannequin(ServerMock server) {
            super(server, UUID.randomUUID());
        }

        @Override
        public ResolvableProfile getProfile() {
            return this.profile;
        }

        @Override
        public void setProfile(ResolvableProfile profile) {
            this.profile = profile;
        }
    }

    private static final class BillboardTextDisplay extends TextDisplayMock {
        private Display.Billboard billboard = Display.Billboard.FIXED;

        BillboardTextDisplay(ServerMock server) {
            super(server, UUID.randomUUID());
        }

        @Override
        public Display.Billboard getBillboard() {
            return this.billboard;
        }

        @Override
        public void setBillboard(Display.Billboard billboard) {
            this.billboard = billboard;
        }
    }
}
