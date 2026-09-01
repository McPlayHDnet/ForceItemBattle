package forceitembattle.listener;

import forceitembattle.gui.InventoryBuilder;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.PlayerOutfitter;
import forceitembattle.model.MenuItem;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.LocatorManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.gui.AchievementCategoryInventory;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.gui.CollectionBookInventory;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.PlayerCounter;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Locator;
import forceitembattle.gui.TeleporterInventory;
import forceitembattle.util.Prefix;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import forceitembattle.gui.VaultInventory;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BrushableBlock;
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

    /**
     * Brushing is a hold-to-use action, and a failed sweep does not use the brush up, so a player
     * leaning on the button could otherwise fire one blocking structure search after another.
     */
    private static final long BRUSH_SWEEP_COOLDOWN_MS = 1500L;

    /**
     * Ground loose enough to sweep for a find. Stone, wood and the like give the brush nothing to
     * read, so the locator only answers on soil — both snows count, since a layer of it is what
     * covers the ground you would be standing on.
     */
    private static final Set<Material> SWEEPABLE_GROUND = Set.of(
            Material.GRASS_BLOCK,
            Material.SAND,
            Material.MUD,
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.SNOW,
            Material.SNOW_BLOCK
    );
    /** Still needed: this listener opens four GUIs, and the GUI layer has not been swept. */
    private final ForceItemBattle plugin;
    private final Roster roster;
    private final BackpackManager backpackManager;
    private final FIBServiceClient fibService;
    private final RoundPhase roundPhase;
    private final LocatorManager locatorManager;
    private final GameSettings settings;
    private final TimerManager timerManager;
    private final Map<UUID, Long> lastBrushSweep = new HashMap<>();

    private boolean isSweepingTooFast(Player player) {
        long now = System.currentTimeMillis();
        Long last = this.lastBrushSweep.put(player.getUniqueId(), now);
        return last != null && now - last < BRUSH_SWEEP_COOLDOWN_MS;
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
    }

    /**
     * Every menu button, in every phase.
     *
     * <p>Was two handlers with identical preambles, one per phase, each switching on raw
     * {@link Material}. Two things came out of that arrangement and both are gone. Anything that
     * looked like a button was one, so a grass block held to build with teleported you and a
     * spyglass dropped you into spectator mode; the identification is now a marker and never
     * consults the material. And {@code ENDER_EYE} meant "play this round" before a round and
     * "teleport to the End" after one, a collision only visible if you read both halves of this
     * file at once; they are two constants now and the marker tells them apart.
     *
     * <p>The phase check is the table's, not this method's: a button that is not live in the
     * current phase is not a button right now.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMenuButton(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!isRightClick(e.getAction())) {
            return;
        }

        MenuItem menuItem = PlayerOutfitter.buttonOf(e.getItem());
        if (menuItem == null || !menuItem.isLiveIn(this.roundPhase.state())) {
            return;
        }

        e.setCancelled(true);

        switch (menuItem) {
            case ACHIEVEMENTS, RESULT_ACHIEVEMENTS -> openMenu(player, () ->
                    new AchievementCategoryInventory(this.plugin, player.getName(), player.getUniqueId()));
            case COLLECTION, RESULT_COLLECTION -> openMenu(player, () ->
                    new CollectionBookInventory(this.plugin, player.getName(), player.getUniqueId()));
            case TELEPORTER -> openMenu(player, () -> new TeleporterInventory(this.plugin));
            case TO_OVERWORLD -> teleportToDimension(e, player, Dimension.OVERWORLD, this.plugin::getSpawnLocation);
            case TO_NETHER -> teleportToDimension(e, player, Dimension.NETHER,
                    () -> new Location(Dimension.NETHER.world(), 0, 70, 0));
            case TO_END -> teleportToDimension(e, player, Dimension.END, () -> {
                World end = Objects.requireNonNull(Dimension.END.world());
                Location location = new Location(end, 0, 0, 0);
                location.setY(end.getHighestBlockYAt(location) + 1);
                return location;
            });
            case SPECTATE_RESULT -> {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.sendMessage(Text.of("<gray>You are <red>no longer<gray> spectating."));
                    player.setGameMode(GameMode.CREATIVE);
                } else {
                    player.sendMessage(Text.of("<gray>You are <green>now<gray> spectating. Use <dark_aqua>/spectate <gray>to toggle off."));
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
            case SPECTATE_ROUND -> setPlaying(player, false);
            case PLAY_ROUND -> setPlaying(player, true);
        }
    }

    /**
     * Opting into or out of the round about to start, and flipping slot 8 to the other button.
     *
     * <p>The roster entry is fetched here rather than gated on by the table: it is the only pair of
     * buttons that needs one, and "you cannot opt in or out of a round you hold no place in" is
     * this button's own rule rather than a property of being a menu item.
     */
    private void setPlaying(Player player, boolean playing) {
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        if (forceItemPlayer == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1, 1);
        forceItemPlayer.setSpectator(!playing);
        player.sendMessage(Text.of(playing
                ? "<dark_aqua>You will <green>play <dark_aqua>this round now."
                : "<dark_aqua>You will <green>spectate <dark_aqua>this round now."));

        PlayerOutfitter.setButton(player, playing ? MenuItem.SPECTATE_ROUND : MenuItem.PLAY_ROUND);
    }

    private void openMenu(Player player, Supplier<InventoryBuilder> menu) {
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
        Scheduler.runSync(() -> menu.get().open(player));
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
        if (!this.roundPhase.roundRunning()) {
            return;
        }
        if (e.getItem() == null) {
            return;
        }
        if (!isRightClick(e.getAction())) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        if (forceItemPlayer == null) {
            return;
        }

        if (Gamemanager.isBackpack(e.getItem())) {
            // isInTeam(), not the setting: with the setting on and no team this passed and then
            // handed a null team to openTeamBackpack.
            if (forceItemPlayer.isInTeam()) {
                this.backpackManager.openTeamBackpack(forceItemPlayer.currentTeam(), player);
            } else {
                this.backpackManager.openPlayerBackpack(player);
            }
            return;
        }

        if (CustomMaterials.WHEEL_OF_FORTUNE.matches(e.getItem())) {
            new VaultInventory(this.plugin).open(player);
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            if (this.settings.isSettingEnabled(GameSetting.STATS)) {
                this.fibService.statistics().recordPlayerCounter(
                        player.getUniqueId(), forceItemPlayer, PlayerCounter.WHEELS_OF_FORTUNE_USED, 1);
            }
            return;
        }

        Locator locator = this.locatorManager.getLocatorByItem(e.getItem());
        if (locator != null) {
            if (locator.getUse() == Locator.Use.BRUSH_GROUND) {
                // A brush is still a brush: on something that can actually be brushed, stay out of
                // the way and let the player dig their sherd out.
                if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof BrushableBlock) {
                    return;
                }
                e.setCancelled(true);
                if (e.getClickedBlock() == null) {
                    player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Brush the ground with it to sweep for <dark_aqua>"
                            + locator.getStructureName() + "<gray>."));
                    return;
                }
                // Guard before the ground check as well, so brushing at a wall cannot spam chat.
                if (this.isSweepingTooFast(player)) {
                    return;
                }
                if (!SWEEPABLE_GROUND.contains(e.getClickedBlock().getType())) {
                    player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Nothing to sweep here — the brush needs "
                            + "loose ground: <dark_aqua>grass<gray>, <dark_aqua>sand<gray>, <dark_aqua>mud<gray>, "
                            + "<dark_aqua>podzol<gray>, <dark_aqua>coarse dirt <gray>or <dark_aqua>snow<gray>."));
                    return;
                }
            } else {
                e.setCancelled(true);
            }
            this.locatorManager.locate(locator.getStructureId(), forceItemPlayer);
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
        if (stack == null) {
            return;
        }

        if (stack.getAmount() > 1) {
            // In a team game the pool is shared, so this player's stack only loses the one they
            // just spent; solo, the stack size *is* the remaining count.
            stack.setAmount(forceItemPlayer.isInTeam() ? stack.getAmount() - 1 : jokersLeft);
            player.getInventory().setItem(foundSlot, stack);
        } else {
            // Was stack.setType(AIR), which Paper deprecated outright -- clearing the slot is
            // what it meant.
            player.getInventory().setItem(foundSlot, null);
        }

        player.getInventory().addItem(CustomMaterials.itemStackOf(mat));
        if (!player.getInventory().contains(mat)) {
            player.getWorld().dropItemNaturally(player.getLocation(), CustomMaterials.itemStackOf(mat));
        }
        this.timerManager.sendActionBar();

        FoundItemEvent foundItemEvent = new FoundItemEvent(player);
        foundItemEvent.setFoundItem(new ItemStack(mat));
        foundItemEvent.setSkipped(true);

        Bukkit.getPluginManager().callEvent(foundItemEvent);
    }

}
