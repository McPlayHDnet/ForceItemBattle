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

/**
 * The live {@link AchievementWorld}: the six questions an achievement rule may ask, answered out of
 * the running managers.
 *
 * <p>This is the only class that knows both that achievements exist and that a plugin does.
 * Every collaborator is named. It used to hold a {@code Supplier<TimerManager>} for the seconds
 * left, which cost a cycle — the timer needs the game manager, which needs the achievement
 * manager that owns this — and bought nothing: {@link RoundClock} is what the timer was asked for,
 * it is already held here for the round duration, and it depends on nothing.
 */
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
