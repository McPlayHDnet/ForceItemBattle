package forceitembattle.model;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class FindDetection {

    private final Roster roster;
    private final RoundPhase roundPhase;

    /**
     * @param stack the item involved, which every Bukkit event here permits to be null
     * @return the participant whose force item this is, or empty if this is not a find
     */
    public Optional<ForceItemPlayer> detect(Player player, @Nullable ItemStack stack) {
        if (stack == null || !this.roundPhase.roundRunning()) {
            return Optional.empty();
        }

        // participant(), not get(): a spectator keeps the item they were hunting when they stopped.
        return this.roster.participant(player.getUniqueId())
                .filter(participant -> stack.getType() == participant.activeMaterial())
                // The backpack is a bundle, so it would complete a bundle round. Checked last because reading item
                // meta deep-copies it.
                .filter(participant -> !GameItems.isBackpack(stack));
    }
}
