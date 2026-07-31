package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.gui.AchievementCategoryInventory;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.gui.CollectionBookInventory;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.service.PlayerStatsWrite;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.model.Locator;
import forceitembattle.gui.TeleporterInventory;
import forceitembattle.util.Text;
import forceitembattle.gui.VaultInventory;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ClickableItemsListener implements Listener {

    private final ForceItemBattle plugin;

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAfterGame(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isEndGame()) {
            return;
        }
        if (e.getItem() == null) {
            return;
        }
        if (!isRightClick(e.getAction())) {
            return;
        }

        switch (e.getItem().getType()) {
            case LIME_DYE -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                Bukkit.getScheduler().runTask(plugin, () ->
                        new AchievementCategoryInventory(this.plugin, player.getName(), player.getUniqueId()).open(player));
            }
            case WRITTEN_BOOK -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                Bukkit.getScheduler().runTask(plugin, () ->
                        new CollectionBookInventory(this.plugin, player.getName(), player.getUniqueId()).open(player));
            }
            case COMPASS -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                Bukkit.getScheduler().runTask(plugin, () -> new TeleporterInventory(this.plugin).open(player));
            }
            case GRASS_BLOCK -> teleportToDimension(e, player, Dimension.OVERWORLD, this.plugin::getSpawnLocation);
            case NETHERRACK  -> teleportToDimension(e, player, Dimension.NETHER,
                    () -> new Location(Dimension.NETHER.world(), 0, 70, 0));
            case SPYGLASS -> {
                e.setCancelled(true);
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.sendMessage(Text.of("<gray>You are <red>no longer<gray> spectating."));
                    player.setGameMode(GameMode.CREATIVE);
                } else {
                    player.sendMessage(Text.of("<gray>You are <green>now<gray> spectating. Use <dark_aqua>/spectate <gray>to toggle off."));
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
            case ENDER_EYE   -> teleportToDimension(e, player, Dimension.END, () -> {
                World end = Objects.requireNonNull(Dimension.END.world());
                Location location = new Location(end, 0, 0, 0);
                location.setY(end.getHighestBlockYAt(location) + 1);
                return location;
            });
        }
    }

    private void teleportToDimension(PlayerInteractEvent e, Player player, Dimension dimension,
                                     Supplier<Location> destination) {
        e.setCancelled(true);
        if (Dimension.of(player) == dimension) {
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            player.sendMessage(Text.of("<dark_gray>[<dark_red>✖<dark_gray>] <gray>You are already in the "
                    + dimension.coloredName()));
            return;
        }
        player.teleport(destination.get());
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) { // triggered if a joker is used
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            return;
        }
        if (e.getItem() == null) {
            return;
        }
        if (!isRightClick(e.getAction())) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (Gamemanager.isBackpack(e.getItem())) {
            if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                this.plugin.getBackpackManager().openTeamBackpack(forceItemPlayer.currentTeam(), player);
            } else {
                this.plugin.getBackpackManager().openPlayerBackpack(player);
            }
            return;
        }

        if (CustomMaterials.WHEEL_OF_FORTUNE.matches(e.getItem())) {
            new VaultInventory(this.plugin).open(player);
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            if (this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
                FibStatisticsClient helper = this.plugin.getFibService().statistics();
                PlayerStatsWrite.record(helper, player.getUniqueId(), forceItemPlayer,
                        () -> FIBServiceClient.soloUpdate().wheelOfFortuneUsesAdd(1L),
                        () -> FIBServiceClient.memberUpdate().wheelOfFortuneUsesAdd(1L));
            }
            return;
        }

        Locator locator = this.plugin.getLocatorManager().getLocatorByMaterial(e.getItem().getType());
        if (locator != null) {
            e.setCancelled(true);
            this.plugin.getLocatorManager().locate(locator.getStructureId(), forceItemPlayer);
            return;
        }

        if (!Gamemanager.isJoker(e.getItem())) {
            return;
        }
        if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof InventoryHolder) {
            return;
        }
        e.setCancelled(true);

        if (forceItemPlayer.activeJokers() <= 0) {
            player.sendMessage(Text.of("<red>No more skips left."));
            player.getInventory().remove(Gamemanager.getJokerMaterial());
            return;
        }

        int foundSlot = e.getPlayer()
                .getInventory()
                .first(Gamemanager.getJokerMaterial());
        if (foundSlot == -1) {
            return;
        }

        Material mat = forceItemPlayer.activeMaterial();
        int jokersLeft = forceItemPlayer.spendJoker();

        ItemStack stack = player.getInventory().getItem(foundSlot);
        if (stack.getAmount() > 1) {
            // In a team game the pool is shared, so this player's stack only loses the one they
            // just spent; solo, the stack size *is* the remaining count.
            stack.setAmount(forceItemPlayer.isInTeam() ? stack.getAmount() - 1 : jokersLeft);
        } else {
            stack.setType(Material.AIR);
        }

        player.getInventory().setItem(foundSlot, stack);

        player.getInventory().addItem(CustomMaterials.itemStackOf(mat));
        if (!player.getInventory().contains(mat)) {
            player.getWorld().dropItemNaturally(player.getLocation(), CustomMaterials.itemStackOf(mat));
        }
        this.plugin.getTimerManager().sendActionBar();

        FoundItemEvent foundItemEvent = new FoundItemEvent(player);
        foundItemEvent.setFoundItem(new ItemStack(mat));
        foundItemEvent.setSkipped(true);

        Bukkit.getPluginManager().callEvent(foundItemEvent);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreGame(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isPreGame()) {
            return;
        }
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            return;
        }
        if (e.getItem() == null) {
            return;
        }
        if (!isRightClick(e.getAction())) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        switch (e.getItem().getType()) {
            case LIME_DYE -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                Bukkit.getScheduler().runTask(plugin, () ->
                        new AchievementCategoryInventory(this.plugin, player.getName(), player.getUniqueId()).open(player));
            }
            case WRITTEN_BOOK -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                Bukkit.getScheduler().runTask(plugin, () ->
                        new CollectionBookInventory(this.plugin, player.getName(), player.getUniqueId()).open(player));
            }
            case ENDER_PEARL -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1, 1);
                forceItemPlayer.setSpectator(true);
                player.sendMessage(Text.of("<dark_aqua>You will <green>spectate <dark_aqua>this round now."));
                player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_EYE).setDisplayName("<dark_gray>» <gray>Play game").getItemStack());
            }
            case ENDER_EYE -> {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1, 1);
                forceItemPlayer.setSpectator(false);
                player.sendMessage(Text.of("<dark_aqua>You will <green>play <dark_aqua>this round now."));
                player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_PEARL).setDisplayName("<dark_gray>» <gray>Spectate game").getItemStack());
            }
        }
    }
}
