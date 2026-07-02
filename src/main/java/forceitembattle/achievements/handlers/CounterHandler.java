package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class CounterHandler implements AchievementHandler<CounterProgress> {

    private final int targetAmount;
    private final boolean requireConsecutive;
    private final String dimension;

    public CounterHandler(int targetAmount, boolean requireConsecutive, String dimension) {
        this.targetAmount = targetAmount;
        this.requireConsecutive = requireConsecutive;
        this.dimension = dimension;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, CounterProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        if (!requireConsecutive && dimension == null) {
            var fib = ForceItemBattle.getInstance();
            int totalItems;
            if (fib.getSettings().isSettingEnabled(GameSetting.TEAM) && forceItemPlayer.currentTeam() != null) {
                totalItems = forceItemPlayer.currentTeam().getPlayers().stream()
                        .mapToInt(teamMember -> teamMember.foundItems().size())
                        .sum();
            } else {
                totalItems = forceItemPlayer.foundItems().size();
            }
            return totalItems >= targetAmount;
        }

        // For CONSECUTIVE achievements, need to track manually

        // Skip events
        if (foundEvent.isSkipped()) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

        Material itemType = foundEvent.getFoundItem().getType();

        // Check dimension if specified
        if (dimension != null && !isItemFromDimension(itemType, dimension)) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

        // Update counters
        if (requireConsecutive) {
            progress.consecutiveCount++;
            return progress.consecutiveCount >= targetAmount;
        } else {
            progress.count++;
            return progress.count >= targetAmount;
        }
    }

    private boolean isItemFromDimension(Material itemType, String dimension) {
        var itemManager = ForceItemBattle.getInstance().getItemDifficultiesManager();
        return switch (dimension) {
            case "world" -> itemManager.getOverworldItems().contains(itemType);
            case "world_nether" -> itemManager.getNetherItems().contains(itemType);
            case "world_the_end" -> itemManager.getEndItems().contains(itemType);
            default -> false;
        };
    }

    @Override
    public CounterProgress createProgress() {
        return new CounterProgress();
    }
}