package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.BiomeNoteLocator;
import forceitembattle.model.BiomeNote;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Text;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class JournalListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack itemStack = event.getItem();

        if (CustomMaterials.WEATHERED_CAPTAINS_JOURNAL.matches(itemStack)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            this.openJournal(event.getPlayer(), itemStack);
            return;
        }

        BiomeNote note = BiomeNote.fromItem(itemStack);
        if (note != null) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            this.readNote(event.getPlayer(), itemStack, note);
        }
    }

    private void openJournal(Player player, ItemStack journal) {
        BiomeNote note = BiomeNote.random();

        journal.setAmount(journal.getAmount() - 1);
        player.getInventory().addItem(note.itemStack())
                .values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        player.sendMessage(Text.of("<gray><i>The journal’s spine cracks as you pry it open. Most pages are pulped…"));
        player.sendMessage(Text.of("<gray><i>…but a folded note slips loose from the binding."));

        player.getWorld().spawnParticle(Particle.POOF, player.getLocation().add(0, 1.2, 0), 12, 0.25, 0.3, 0.25, 0.01);
        player.playSound(player, Sound.BLOCK_WOOD_BREAK, SoundCategory.PLAYERS, 0.4F, 0.6F);
        player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 0.7F);
    }

    private void readNote(Player player, ItemStack noteStack, BiomeNote note) {
        if (!BiomeNoteLocator.isOverworld(player.getWorld())) {
            player.sendMessage(Text.of("<red>These maps only chart Overworld biomes. Use it in the Overworld."));
            return;
        }

        Location target = BiomeNoteLocator.locate(note, player.getLocation());
        if (target == null) {
            player.sendMessage(Text.of("<red>[Biome Locator] No matching biome within search range. Move to a different area and try again."));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.6F, 0.6F);
            return;
        }

        String direction = BiomeNoteLocator.direction(player.getLocation(), target);
        int distance = BiomeNoteLocator.distance(player.getLocation(), target);

        player.sendMessage(Text.of("<gray><i>You unfold the brittle note. The faded ink reads:"));
        player.sendMessage(Text.of(String.format(
                "  <yellow>“Hold yer heading <gold><b>%s</b><yellow> near on <gold><b>%d</b><yellow> paces, an' ye'll come upon <gold>%s<yellow>. Whether 'tis worth the walk, only ye can say.”",
                direction, distance, note.randomFlavor()
        )));

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05);
        player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 1.0F);

        noteStack.setAmount(noteStack.getAmount() - 1);
    }
}
