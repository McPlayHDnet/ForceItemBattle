package forceitembattle.listener;

import forceitembattle.settings.GameSettings;
import forceitembattle.settings.GameSetting;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

@RequiredArgsConstructor
public class SettingsListener implements Listener {
    private final GameSettings settings;
    @EventHandler
    public void onGliding(EntityToggleGlideEvent entityToggleGlideEvent) {
        if (entityToggleGlideEvent.isGliding()) {
            if (!this.settings.isSettingEnabled(GameSetting.ELYTRA)) {
                entityToggleGlideEvent.setCancelled(true);
            }
        }
    }
}
