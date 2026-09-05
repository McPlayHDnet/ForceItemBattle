package forceitembattle.manager;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Locator;
import forceitembattle.util.BiomeSearch;
import forceitembattle.util.CaveScan;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Prefix;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

public class LocatorManager implements Manager {

    private static final int STRUCTURE_SEARCH_RADIUS = 20;

    /** Close enough to start digging for something that is under you. */
    private static final int BURIED_ARRIVAL_RADIUS = 50;    // blocks
    /** Close enough to spot something standing at the surface. */
    private static final int SURFACE_ARRIVAL_RADIUS = 70;   // blocks

    /** Chunks each way around the biome's middle that get generated and read. 5×5 in all. */
    private static final int SOUNDING_CHUNK_RADIUS = 2;

    /** Generating 25 fresh chunks thousands of blocks away is usually seconds, not always. */
    private static final long SOUNDING_TIMEOUT_SECONDS = 20L;

    private final PositionManager positionManager;
    private final Map<String, Locator> locators;
    private final Map<String, Location> locatedStructures;
    private final Map<UUID, Map<String, ActiveLocator>> activeLocators;

    /**
     * Who is mid-sweep. A biome locate now takes a moment to come back, and without this a second
     * right-click during that moment starts a second sweep and spends a second locator.
     */
    private final Set<UUID> sounding;

    public LocatorManager(PositionManager positionManager) {
        this.positionManager = positionManager;
        this.locators = new HashMap<>();
        this.locatedStructures = new HashMap<>();
        this.activeLocators = new HashMap<>();
        this.sounding = ConcurrentHashMap.newKeySet();

        this.addLocator(new Locator("fib:antimatter_depths_portal", "Antimatter", CustomMaterials.ANTIMATTER_LOCATOR, Locator.Type.STRUCTURE,
                Locator.Use.RIGHT_CLICK, SURFACE_ARRIVAL_RADIUS, Color.PURPLE, "#B314A8:#E775C3"));
        this.addLocator(new Locator("trial_chambers", "Trial Chambers", CustomMaterials.TRIAL_LOCATOR, Locator.Type.STRUCTURE,
                Locator.Use.RIGHT_CLICK, BURIED_ARRIVAL_RADIUS, Color.fromRGB(0x4F, 0xB4, 0x93), "#2E7D68:#7FD8BC"));
        this.addLocator(new Locator("sulfur_caves", "Sulfur Cave", CustomMaterials.SULFUR_LOCATOR, Locator.Type.BIOME,
                Locator.Use.RIGHT_CLICK, BURIED_ARRIVAL_RADIUS, Color.YELLOW, "#C7A500:#FFF27E"));
        this.addLocator(new Locator("trail_ruins", "Trail Ruins", CustomMaterials.KILN_FIRED_BRUSH, Locator.Type.STRUCTURE,
                Locator.Use.BRUSH_GROUND, SURFACE_ARRIVAL_RADIUS, Color.fromRGB(0xC7, 0x7B, 0x3E), "#8A4B22:#E0A46B"));
    }

    @Override
    public void disable() {
        synchronized (this.activeLocators) {
            this.activeLocators.forEach((playerId, byStructure) -> {
                Player player = Bukkit.getPlayer(playerId);
                byStructure.values().forEach(active -> active.cancelAndHide(player));
            });
            this.activeLocators.clear();
        }
        this.sounding.clear();
    }

    private void addLocator(Locator locator) {
        this.locators.put(locator.getStructureId(), locator);
    }

    public void locate(String structureId, ForceItemPlayer forceItemPlayer) {
        Locator locator = this.locators.get(structureId);
        if (locator == null) {
            return;
        }

        Player player = forceItemPlayer.player();

        if (!Dimension.isOverworld(player)) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<red>There is no <dark_aqua>" + locator.getStructureName()
                    + " <red>in the " + Dimension.of(player).coloredName() + "<red>."));
            return;
        }

        switch (locator.getType()) {
            case STRUCTURE -> {
                Location targetLocation = this.locateStructure(locator, player);
                if (targetLocation != null) { // the helper already messaged the player if not
                    this.reveal(locator, player, targetLocation, null);
                }
            }
            // Biome locates finish later, on the chunks they had to generate. See locateBiome.
            case BIOME -> this.locateBiome(locator, player);
        }
    }

    @Nullable
    private Location locateStructure(Locator locator, Player player) {
        @Nullable Structure structure = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.STRUCTURE)
                .get(this.getNamespacedKey(locator.getStructureId()));

        if (structure == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return null;
        }

        StructureSearchResult result = player.getWorld().locateNearestStructure(
                player.getLocation(),
                structure,
                STRUCTURE_SEARCH_RADIUS,
                false
        );

        if (result == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found."));
            return null;
        }

        return result.getLocation();
    }

    /**
     * Three steps, because the biome search alone has never been enough for an underground biome.
     *
     * <ol>
     *   <li>{@code nearest} for a point on the region's rim — cheap, wide, and the only step that
     *       can say "not around here".
     *   <li>{@link BiomeSearch#interior} to walk in from that rim. Still free: noise samples, no
     *       chunks.
     *   <li>{@link #soundOut} to generate what is actually there and look at it, because a cave
     *       biome is paint over the carvers' work and promises no cavity anywhere.
     * </ol>
     *
     * <p>Only the third step costs anything, and it is what turns "somewhere over there, good
     * luck" into a spot someone has looked at.
     */
    private void locateBiome(Locator locator, Player player) {
        Biome biome = BiomeSearch.resolve(this.getNamespacedKey(locator.getStructureId()));

        if (biome == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureId() + " <red>is not loaded or could not be found, Fire fix!"));
            return;
        }

        if (!this.sounding.add(player.getUniqueId())) {
            return;
        }

        Location rim = BiomeSearch.nearest(player.getLocation(), biome);

        if (rim == null) {
            this.sounding.remove(player.getUniqueId());
            player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <red>could not be found nearby."));
            return;
        }

        player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Sounding out the <dark_aqua>"
                + locator.getStructureName() + "<gray>…"));

        this.soundOut(locator, player, biome, BiomeSearch.interior(rim, biome));
    }

    /**
     * Generates the chunks around the middle of the biome and reads them for a dig spot.
     *
     * <p>The threading is the fiddly part and it is deliberate: {@code getChunkAtAsync} completes
     * on the main thread, which is where a snapshot has to be taken, so the snapshot is taken in
     * the future's own callback. Only the scanning — a quarter of a million palette lookups —
     * moves off the main thread, and it reads snapshots precisely because those are safe to read
     * there. Everything that touches a player is hopped back with {@code Scheduler.runSync}.
     */
    private void soundOut(Locator locator, Player player, Biome biome, Location interior) {
        World world = interior.getWorld();
        if (world == null) {
            this.finishSounding(locator, player, interior, null);
            return;
        }

        // Read on the main thread and carried in, so the scan holds no world reference at all.
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        int chunkX = interior.getBlockX() >> 4;
        int chunkZ = interior.getBlockZ() >> 4;

        List<CompletableFuture<ChunkSnapshot>> pending = new ArrayList<>();
        for (int dx = -SOUNDING_CHUNK_RADIUS; dx <= SOUNDING_CHUNK_RADIUS; dx++) {
            for (int dz = -SOUNDING_CHUNK_RADIUS; dz <= SOUNDING_CHUNK_RADIUS; dz++) {
                pending.add(world.getChunkAtAsync(chunkX + dx, chunkZ + dz, true, false)
                        .thenApply(chunk -> chunk.getChunkSnapshot(true, true, false, false)));
            }
        }

        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .orTimeout(SOUNDING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ignored, error) -> {
                    // disable() empties the set, so a sweep whose chunks land after the round has
                    // ended stops here rather than handing work to a plugin that is already gone.
                    if (!this.sounding.contains(player.getUniqueId())) {
                        return;
                    }
                    if (error != null) {
                        // Worth degrading rather than failing: the interior point beats the rim
                        // point the locator used to hand out even with nothing verified about it.
                        Scheduler.runSync(() -> this.finishSounding(locator, player, interior, null));
                        return;
                    }
                    Scheduler.runAsync(() -> {
                        List<ChunkSnapshot> snapshots = pending.stream().map(CompletableFuture::join).toList();
                        CaveScan.Target target = CaveScan.scan(snapshots, biome,
                                interior.getBlockX(), interior.getBlockY(), interior.getBlockZ(),
                                minHeight, maxHeight);
                        if (!this.sounding.contains(player.getUniqueId())) {
                            return; // same guard again: the scan itself takes a moment
                        }
                        Scheduler.runSync(() -> this.finishSounding(locator, player, interior, target));
                    });
                });
    }

    /** Back on the main thread, with whatever the scan found. */
    private void finishSounding(Locator locator, Player player, Location interior, @Nullable CaveScan.Target target) {
        this.sounding.remove(player.getUniqueId());

        if (!player.isOnline()) {
            return;
        }

        if (target == null) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Found no opening near the <dark_aqua>"
                    + locator.getStructureName() + "<gray> — pointing at the middle of the biome instead."));
            this.reveal(locator, player, interior, null);
            return;
        }

        Location located = new Location(interior.getWorld(), target.x(), target.y(), target.z());
        this.reveal(locator, player, located, target.find());
    }

    /**
     * @param find what the chunks turned out to hold, or {@code null} when nothing looked at the
     *             blocks — a structure search, or a biome sweep that came back empty-handed.
     */
    private void reveal(Locator locator, Player player, Location targetLocation, @Nullable CaveScan.Find find) {
        // Resolved here on the main thread; the async session task must not touch the world.
        Location digSpot = this.surfaceDigSpot(targetLocation);

        // Worked out once and handed to both the chat line and the boss bar, so the two cannot
        // disagree about whether this locate knows its depth.
        String coordinates = find == null
                ? LocationFormat.xz(targetLocation)
                : LocationFormat.xyz(targetLocation);

        if (!this.isAlreadyRevealed(locator.getStructureId(), targetLocation)) {
            if (locator.getUse().consumedOnFind()) {
                this.destroyLocator(player, locator);
            }
            player.playSound(player, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2, 1);
            this.startLocatorSession(locator, player, targetLocation, digSpot, coordinates);
        }

        this.showTheWay(locator, player, targetLocation, digSpot);
        player.sendMessage(Text.of(Prefix.LOCATOR + "<dark_aqua>" + locator.getStructureName() + " <gray>located at "
                + coordinates
                + LocationFormat.distance(player.getLocation(), targetLocation)
                + this.digAdvice(find, targetLocation)));
        this.locatedStructures.put(locator.getStructureId(), targetLocation);
    }

    /** What to do on arrival, once the sweep knows something about the ground there. */
    private String digAdvice(@Nullable CaveScan.Find find, Location targetLocation) {
        if (find == null) {
            return "";
        }
        return switch (find) {
            // Deliberately does not say "spring". Sulfur tops a column either because a spring grew
            // its root system up to it or because the cave itself has been cut open there, and the
            // scan cannot tell which — but "dig where the sulfur is" is the right move for both.
            case SURFACE -> "\n" + Prefix.LOCATOR + "<gray>There is <yellow>sulfur <gray>at the surface there. "
                    + "Dig where it breaks through and it takes you into the cave.";
            case CAVE -> "\n" + Prefix.LOCATOR + "<gray>Open cave at <dark_aqua>y=" + targetLocation.getBlockY()
                    + "<gray>. Dig down from the marker.";
        };
    }

    private void showTheWay(Locator locator, Player player, Location targetLocation, Location digSpot) {
        if (locator.getUse().leavesFootprints()) {
            this.positionManager.playFootprintTrail(player, targetLocation, locator.getLineColor());
            return;
        }
        this.positionManager.playParticleLine(player, targetLocation, locator.getLineColor());
        this.positionManager.playSurfaceMarker(player, digSpot, locator.getLineColor());
    }

    /** The surface block above the target, i.e. where the player has to dig down. */
    private Location surfaceDigSpot(Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null) {
            return targetLocation;
        }
        return new Location(world,
                targetLocation.getBlockX() + 0.5,
                world.getHighestBlockYAt(targetLocation.getBlockX(), targetLocation.getBlockZ()) + 1,
                targetLocation.getBlockZ() + 0.5);
    }

    /**
     * @param coordinates the target's position, already formatted — {@code x, ?, z} for a search
     *                    that only resolved a column, {@code x, y, z} once the chunks have been
     *                    read and the depth is a real one. See {@link LocationFormat#xz}.
     */
    private void startLocatorSession(Locator locator, Player player, Location targetLocation, Location digSpot,
                                     String coordinates) {
        UUID playerId = player.getUniqueId();

        this.clearLocator(playerId, locator.getStructureId());

        BossBar bar = BossBar.bossBar(Text.of(""), 1, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);

        BukkitRunnable task = new BukkitRunnable() {
            /** Runs between particle-line replays; the boss bar updates every run. */
            private static final int RUNS_PER_LINE = 30;
            private int runs = 0;

            @Override
            public void run() {
                String bossBarTitle = "<gradient:" + locator.getBossBarGradient() + "><b>" + locator.getStructureName() + " <reset><dark_gray>» "
                        + coordinates
                        + LocationFormat.distance(player.getLocation(), targetLocation);
                bar.name(Text.of(bossBarTitle));
                player.showBossBar(bar);

                // The line is a one-off animation, so it is replayed now and then. Footprints have
                // their own continuous task — replaying them would give the trail a pulse.
                if (!locator.getUse().leavesFootprints() && this.runs++ % RUNS_PER_LINE == 0) {
                    LocatorManager.this.positionManager
                            .playParticleLine(player, targetLocation, locator.getLineColor());
                }

                if (player.getWorld() == targetLocation.getWorld()
                        && player.getLocation().distance(targetLocation) <= locator.getArrivalRadius()) {
                    LocatorManager.this.clearLocator(playerId, locator.getStructureId());
                }
            }
        };

        // The persistent ground visual, cancelled together with the session.
        BukkitRunnable groundTask = locator.getUse().leavesFootprints()
                ? this.positionManager.startFootprintTrail(player, targetLocation, locator.getLineColor())
                : this.positionManager.startSurfaceMarker(player, digSpot, locator.getLineColor());

        synchronized (this.activeLocators) {
            this.activeLocators
                    .computeIfAbsent(playerId, uuid -> new LinkedHashMap<>())
                    .put(locator.getStructureId(), new ActiveLocator(bar, task, groundTask));
        }

        Scheduler.runTimerAsync(task, 0L, 10L);
    }

    /** Removes a session from the tracking map without touching the boss bar. */
    @Nullable
    private ActiveLocator removeSession(UUID playerId, String structureId) {
        synchronized (this.activeLocators) {
            Map<String, ActiveLocator> byStructure = this.activeLocators.get(playerId);
            if (byStructure == null) {
                return null;
            }
            ActiveLocator active = byStructure.remove(structureId);
            if (byStructure.isEmpty()) {
                this.activeLocators.remove(playerId);
            }
            return active;
        }
    }

    private void clearLocator(UUID playerId, String structureId) {
        ActiveLocator active = this.removeSession(playerId, structureId);
        if (active != null) {
            active.cancelAndHide(Bukkit.getPlayer(playerId));
        }
    }

    public boolean dismiss(Player player, String structureId) {
        ActiveLocator active = this.removeSession(player.getUniqueId(), structureId);
        if (active == null) {
            return false;
        }
        active.cancelAndHide(player);
        return true;
    }

    /** Keyed by structure id, in the order they were located. */
    public Map<String, Locator> getActiveLocators(Player player) {
        Map<String, Locator> result = new LinkedHashMap<>();
        synchronized (this.activeLocators) {
            Map<String, ActiveLocator> byStructure = this.activeLocators.get(player.getUniqueId());
            if (byStructure == null) {
                return result;
            }
            for (String structureId : byStructure.keySet()) {
                Locator locator = this.locators.get(structureId);
                if (locator != null) {
                    result.put(structureId, locator);
                }
            }
        }
        return result;
    }

    public int dismissAll(Player player) {
        Map<String, ActiveLocator> byStructure;
        synchronized (this.activeLocators) {
            byStructure = this.activeLocators.remove(player.getUniqueId());
        }
        if (byStructure == null || byStructure.isEmpty()) {
            return 0;
        }
        byStructure.values().forEach(active -> active.cancelAndHide(player));
        return byStructure.size();
    }

    /**
     * The locator this stack is the item for, or null. Matching on the stack rather than the material
     * is what keeps a plain brush from working as the Trail Ruins locator.
     */
    @Nullable
    public Locator getLocatorByItem(ItemStack itemStack) {
        return this.locators.values().stream()
                .filter(locator -> locator.matches(itemStack))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public Locator randomLocator() {
        List<Locator> all = List.copyOf(this.locators.values());
        return all.isEmpty() ? null : all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    private NamespacedKey getNamespacedKey(String structureId) {
        return structureId.contains("fib:") ? NamespacedKey.fromString(structureId) : NamespacedKey.minecraft(structureId);
    }

    private boolean isAlreadyRevealed(String structureId, Location location) {
        return location.equals(locatedStructures.get(structureId));
    }

    private void destroyLocator(Player player, Locator locator) {
        if (!locator.matches(player.getInventory().getItemInMainHand())) return;
        player.getInventory().setItemInMainHand(null);
    }

    private record ActiveLocator(BossBar bossBar, BukkitRunnable task, @Nullable BukkitRunnable groundTask) {

        void cancelAndHide(@Nullable Player player) {
            this.task.cancel();
            if (this.groundTask != null && !this.groundTask.isCancelled()) {
                this.groundTask.cancel();
            }
            if (player != null) {
                player.hideBossBar(this.bossBar);
            }
        }
    }
}
