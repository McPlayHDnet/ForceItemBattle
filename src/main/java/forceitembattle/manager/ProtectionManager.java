package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.Bed;
import org.bukkit.block.Chest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProtectionManager {

    private final ForceItemBattle plugin;

    private final Map<UUID, Bed> bedMap;
    private final Map<UUID, Chest> chestMap;
    private final Map<UUID, Barrel> barrelMap;

    public ProtectionManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.bedMap = new HashMap<>();
        this.chestMap = new HashMap<>();
        this.barrelMap = new HashMap<>();
    }

    public ForceItemPlayer getOwnerOfProtectedBed(Bed bed) {
        for (UUID uuid : this.plugin.getProtectionManager().bedMap.keySet()) {
            Bed protectedBed = this.plugin.getProtectionManager().getProtectedBed(uuid);
            if (protectedBed != null && protectedBed.getLocation().equals(bed.getLocation())) {
                return this.plugin.getGamemanager().getForceItemPlayer(uuid);
            }
        }
        return null;
    }

    public ForceItemPlayer getOwnerOfProtectedChest(Chest chest) {
        for (UUID uuid : this.plugin.getProtectionManager().chestMap.keySet()) {
            Chest protectedChest = this.plugin.getProtectionManager().getProtectedChest(uuid);
            if (protectedChest != null && protectedChest.getLocation().equals(chest.getLocation())) {
                return this.plugin.getGamemanager().getForceItemPlayer(uuid);
            }
        }
        return null;
    }

    public ForceItemPlayer getOwnerOfProtectedBarrel(Barrel barrel) {
        for (UUID uuid : this.plugin.getProtectionManager().barrelMap.keySet()) {
            Barrel protectedBarrel = this.plugin.getProtectionManager().getProtectedBarrel(uuid);
            if (protectedBarrel != null && protectedBarrel.getLocation().equals(barrel.getLocation())) {
                return this.plugin.getGamemanager().getForceItemPlayer(uuid);
            }
        }
        return null;
    }

    public boolean canBreakBlock(ForceItemPlayer breaker, ForceItemPlayer owner) {
        if (breaker.equals(owner)) {
            return true;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return breaker.currentTeam().getPlayers().contains(owner);
        }

        return false;
    }

    public boolean canOpenInventory(ForceItemPlayer opener, ForceItemPlayer owner) {
        if (opener.equals(owner)) {
            return true;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return opener.currentTeam().getPlayers().contains(owner);
        }

        return false;
    }

    public void protectChest(ForceItemPlayer forceItemPlayer, Chest chest) {
        this.chestMap.put(forceItemPlayer.player().getUniqueId(), chest);
    }

    public void protectBarrel(ForceItemPlayer forceItemPlayer, Barrel barrel) {
        this.barrelMap.put(forceItemPlayer.player().getUniqueId(), barrel);
    }

    public void protectBed(ForceItemPlayer forceItemPlayer, Bed bed) {
        this.bedMap.put(forceItemPlayer.player().getUniqueId(), bed);
    }

    public Chest getProtectedChest(UUID uuid) {
        return this.chestMap.get(uuid);
    }

    public Barrel getProtectedBarrel(UUID uuid) {
        return this.barrelMap.get(uuid);
    }

    public Bed getProtectedBed(UUID uuid) {
        return this.bedMap.get(uuid);
    }
}
