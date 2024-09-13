package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.achievements.AchievementInventory;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ItemBuilder;
import forceitembattle.util.Locator;
import forceitembattle.util.TeleporterInventory;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ClickableItemsListener implements Listener {

    private final ForceItemBattle plugin;

    @EventHandler
    public void onAfterGame(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!this.plugin.getGamemanager().isEndGame()) {
            return;
        }
        if (e.getItem() == null) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.LIME_DYE) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                new AchievementInventory(this.plugin, forceItemPlayer.player().getName()).open(player);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.COMPASS) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                new TeleporterInventory(this.plugin).open(player);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.GRASS_BLOCK) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if (player.getWorld().getName().equals("world")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<dark_red>✖<dark_gray>] <gray>You are already in the <green>overworld"));
                    return;
                }
                player.teleport(this.plugin.getSpawnLocation());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.NETHERRACK) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if (player.getWorld().getName().equals("world_nether")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<dark_red>✖<dark_gray>] <gray>You are already in the <red>nether"));
                    return;
                }
                player.teleport(new Location(Bukkit.getWorld("world_nether"), 0, 70, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.SPYGLASS) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>You are <red>no longer<gray> spectating."));
                    player.setGameMode(GameMode.CREATIVE);
                } else {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>You are <green>now<gray> spectating. Use <dark_aqua>/spectate <gray>to toggle off."));
                    player.setGameMode(GameMode.SPECTATOR);
                }
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.ENDER_EYE) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                if (player.getWorld().getName().equals("world_the_end")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>You are already in the <dark_purple>end"));
                    return;
                }
                World end = Bukkit.getWorld("world_the_end");
                Location location = new Location(end, 0, 0, 0);
                assert end != null;
                location.setY(end.getHighestBlockYAt(location) + 1);

                player.teleport(location);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            return;
        }
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

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.BUNDLE) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                    this.plugin.getBackpack().openTeamBackpack(forceItemPlayer.currentTeam(), player);
                } else {
                    this.plugin.getBackpack().openPlayerBackpack(player);
                }
                return;
            }
        }

        Locator locator = this.plugin.getLocatorManager().getLocatorByMaterial(e.getItem().getType());
        if(locator != null) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                this.plugin.getLocatorManager().locate(locator.getStructureId(), forceItemPlayer);
                return;
            }
        }

        if (!Gamemanager.isJoker(e.getItem())) {
            return;
        }
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }

        int jokers = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getRemainingJokers() : forceItemPlayer.remainingJokers());
        if (jokers <= 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>No more skips left."));
            player.getInventory().remove(Gamemanager.getJokerMaterial());
            return;
        }

        jokers--;

        int foundSlot = e.getPlayer()
                .getInventory()
                .first(Gamemanager.getJokerMaterial());

        ItemStack stack = player.getInventory().getItem(foundSlot);
        assert stack != null;
        if (stack.getAmount() > 1) {
            stack.setAmount((this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? stack.getAmount() - 1 : jokers));
        } else {
            stack.setType(Material.AIR);
        }
        Material mat = forceItemPlayer.getCurrentMaterial();

        player.getInventory().setItem(foundSlot, stack);

        player.getInventory().addItem(new ItemStack(mat));
        if (!player.getInventory().contains(mat)) {
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(mat));
        }
        this.plugin.getTimer().sendActionBar();

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            forceItemPlayer.currentTeam().setRemainingJokers(jokers);
        } else {
            forceItemPlayer.setRemainingJokers(jokers);
        }

        FoundItemEvent foundItemEvent = new FoundItemEvent(player);
        foundItemEvent.setFoundItem(new ItemStack(mat));
        foundItemEvent.setSkipped(true);

        Bukkit.getPluginManager().callEvent(foundItemEvent);
    }

    @EventHandler
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

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (e.getItem().getType() == Material.LIME_DYE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
                new AchievementInventory(this.plugin, player.getName()).open(player);
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.ENDER_PEARL) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1, 1);
                forceItemPlayer.setSpectator(true);
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You will <green>spectate <dark_aqua>this round now."));
                player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_EYE).setDisplayName("<dark_gray>» <gray>Play game").getItemStack());
                return;
            }
            return;
        }

        if (e.getItem().getType() == Material.ENDER_EYE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                e.setCancelled(true);

                player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1, 1);
                forceItemPlayer.setSpectator(false);
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You will <green>play <dark_aqua>this round now."));
                player.getInventory().setItem(8, new ItemBuilder(Material.ENDER_PEARL).setDisplayName("<dark_gray>» <gray>Spectate game").getItemStack());
                return;
            }
        }
    }
}
