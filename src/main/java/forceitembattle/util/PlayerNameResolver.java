package forceitembattle.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlayerNameResolver {

    private static final Map<UUID, String> CACHE = new ConcurrentHashMap<>();

    private PlayerNameResolver() {
    }

    private static String localName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        String cached = CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        return Bukkit.getOfflinePlayer(uuid).getName(); // usercache hit, or null
    }

    public static void resolveAll(List<UUID> uuids, Consumer<Map<UUID, String>> callback) {
        Map<UUID, String> known = new HashMap<>();
        List<UUID> missing = new ArrayList<>();

        for (UUID uuid : uuids) {
            if (known.containsKey(uuid) || missing.contains(uuid)) {
                continue; // same uuid twice in one board (e.g. duo) — handle once
            }

            String local = localName(uuid);
            if (local != null) {
                known.put(uuid, local);
            } else {
                missing.add(uuid);
            }
        }

        // Everything already known: no thread hop, render on the current tick.
        if (missing.isEmpty()) {
            callback.accept(known);
            return;
        }

        Scheduler.runAsync(() -> {
            for (UUID uuid : missing) {
                try {
                    // update() rather than complete(): complete() is known to leave the name null
                    // when built from a bare uuid (PaperMC/Paper#8927). update() fills it reliably.
                    String name = Bukkit.createProfile(uuid).update().get().getName();
                    if (name != null && !name.isBlank()) {
                        CACHE.put(uuid, name);
                        known.put(uuid, name);
                    }
                } catch (Exception exception) {
                    // Rate-limited, offline, or a deleted account — leave absent, caller falls back.
                }
            }

            Scheduler.runSync(() -> callback.accept(known));
        });
    }
}
