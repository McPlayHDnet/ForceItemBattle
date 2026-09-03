package forceitembattle.model;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Whether an item that just passed through someone's hands is a find.
 *
 * <p>Four questions, in one place: is the round running, is this player actually playing it, is the
 * item something a find can be made of, and is it the item they are hunting. {@link FindOutcome}
 * answers what a find is <em>worth</em>; this answers whether there is one at all.
 *
 * <p>That decision used to be a three-line preamble copy-pasted into eight event handlers — pickup,
 * click, craft, smith, three bucket shapes and consume — and it was the untested half. The tested
 * half only runs after somebody else has decided the find happened.
 *
 * <p><b>It asks {@link Roster#participant} rather than {@code get}</b>, which the copies did not.
 * A spectator keeps their roster entry and their last force item, so the old lookup let someone who
 * had taken the spectate toggle keep scoring by picking things up.
 */
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

        // The backpack is a bundle, and a round whose force item is a bundle would otherwise be
        // completed by opening the one every player is handed at the start.
        if (GameItems.isBackpack(stack)) {
            return Optional.empty();
        }

        return this.roster.participant(player.getUniqueId())
                .filter(participant -> stack.getType() == participant.activeMaterial());
    }
}
