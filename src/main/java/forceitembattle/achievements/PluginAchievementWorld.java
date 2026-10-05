package forceitembattle.achievements;

import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.model.Dimension;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.ScoreOwner;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@RequiredArgsConstructor
public final class PluginAchievementWorld implements AchievementWorld {

    private final Roster roster;
    private final RoundClock roundClock;
    private final GameSettings settings;
    private final ItemDifficultiesManager items;
    private final BackpackManager backpacks;
    private final WanderingTraderManager traders;

    @Override
    public int roundDuration() {
        return this.roundClock.totalSeconds();
    }

    @Override
    public int secondsLeft() {
        return this.roundClock.secondsLeft();
    }

    @Override
    public Set<Material> itemsIn(Dimension dimension) {
        return this.items.getItemsIn(dimension);
    }

    @Override
    public boolean isTrading(UUID playerId) {
        return this.traders.isTrading(playerId);
    }

    @Override
    public boolean backpackEnabled() {
        return this.settings.isSettingEnabled(GameSetting.BACKPACK);
    }

    @Override
    @Nullable
    public Inventory backpackOf(Player player) {
        return this.backpacks.getBackpackForPlayer(player);
    }

    @Override
    public List<ScoreOwner> scoreOwners() {
        return this.roster.activeScoreOwners();
    }
}
