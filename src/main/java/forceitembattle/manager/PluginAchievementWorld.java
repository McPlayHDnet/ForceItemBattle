package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementWorld;
import forceitembattle.model.Dimension;
import forceitembattle.model.ScoreOwner;
import forceitembattle.settings.GameSetting;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * The live {@link AchievementWorld}: the six questions an achievement rule may ask, answered out of
 * the running managers.
 *
 * <p>This is the only class that knows both that achievements exist and that a plugin does. It
 * takes the plugin rather than its collaborators because it is built inside {@code
 * AchievementManager}'s constructor, and the managers it reads are registered after that one — the
 * construction-order constraint recorded on {@link Manager}. Every lookup is therefore deferred to
 * call time, which is also what keeps this correct across a round reset.
 */
@RequiredArgsConstructor
public final class PluginAchievementWorld implements AchievementWorld {

    private final ForceItemBattle plugin;

    @Override
    public int roundDuration() {
        return this.plugin.getRoundClock().totalSeconds();
    }

    @Override
    public int secondsLeft() {
        return this.plugin.getTimerManager().getTimeLeft();
    }

    @Override
    public Set<Material> itemsIn(Dimension dimension) {
        return this.plugin.getItemDifficultiesManager().getItemsIn(dimension);
    }

    @Override
    public boolean isTrading(UUID playerId) {
        return this.plugin.getWanderingTraderManager().isTrading(playerId);
    }

    @Override
    public boolean backpackEnabled() {
        return this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK);
    }

    @Override
    @Nullable
    public Inventory backpackOf(Player player) {
        return this.plugin.getBackpackManager().getBackpackForPlayer(player);
    }

    @Override
    public List<ScoreOwner> scoreOwners() {
        return this.plugin.getRoster().activeScoreOwners();
    }
}
