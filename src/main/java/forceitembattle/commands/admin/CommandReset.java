package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.util.SeedPool;
import forceitembattle.util.Text;
import forceitembattle.util.WorldReset;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandReset extends CustomCommand implements CustomTabCompleter {

    private final SeedPool seedPool;
    private final WorldReset worldReset;

    public CommandReset(SeedPool seedPool, WorldReset worldReset) {
        super("reset");
        this.seedPool = seedPool;
        this.worldReset = worldReset;

        setUsage("[biome]");
        setDescription("Restart server with a new seed (optionally forcing a start biome)");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        // Resolve the forced seed BEFORE kicking anyone, so an invalid biome
        // aborts cleanly without disrupting the server. null => random world.
        Long seed = null;
        String forcedBiome = null;
        if (args.length >= 1) {
            String biome = args[0].toLowerCase(Locale.ROOT);
            SeedPool pool = this.seedPool;

            if (pool == null || !pool.isAvailable()) {
                player.sendMessage(Text.of("<red>Biome-specific reset is unavailable (seed pool not loaded)."));
                return;
            }
            if (!pool.has(biome)) {
                player.sendMessage(Text.of("<red>Unknown biome '" + biome + "'. Use tab-completion to see the options."));
                return;
            }
            try {
                seed = pool.randomSeed(biome);
            } catch (IOException e) {
                player.sendMessage(Text.of("<red>Failed to pick a seed for '" + biome + "': " + e.getMessage()));
                return;
            }
            forcedBiome = biome;
        }

        String forcedLine = forcedBiome == null
                ? ""
                : "\n<gray>Forced Biome: <yellow>" + prettify(forcedBiome);

        String kickMessage =
                "<dark_gray>» <gold><b>ForceItemBattle</b> <dark_gray>«" +
                        "\n" +
                        "<red>The world is being reset!" +
                        forcedLine +
                        "\n";

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kick(Text.of(kickMessage)));

        this.worldReset.scheduleReset(seed);
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();

        SeedPool pool = this.seedPool;
        if (pool == null || !pool.isAvailable()) return Collections.emptyList();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return pool.groups().stream().filter(group -> group.startsWith(prefix)).toList();
    }

    /** "old_growth_pine_taiga" -> "Old Growth Pine Taiga". */
    private static String prettify(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
