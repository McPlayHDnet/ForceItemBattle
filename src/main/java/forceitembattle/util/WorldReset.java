package forceitembattle.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

/**
 * Wiping the world and restarting the server onto a fresh one.
 *
 * <p>None of this runs inline: the world directory cannot be deleted while the server still holds
 * it open, so {@link #scheduleReset} resets nothing itself — it registers a shutdown hook and asks
 * the server to restart, and the work happens on the way down.
 *
 * <p>Split out of {@code ForceItemBattle} because none of it is composition. It is file and process
 * work that merely happens to be reachable from a command, and it was the largest thing in that
 * class with nothing to do with wiring managers together.
 */
public final class WorldReset {

    /** Where the datapack zip ships, i.e. the plugin's own data folder. */
    private final File dataFolder;

    public WorldReset(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Deletes the world and restarts onto {@code seed}, or onto a random one when it is null.
     *
     * <p>The seed is written before the delete, so a failure there still leaves a server that boots
     * — on the old seed rather than on none.
     */
    public void scheduleReset(Long seed) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writeLevelSeed(seed == null ? "" : Long.toString(seed));
            } catch (IOException e) {
                System.out.println("[FIB] Failed to set level-seed; resetting with existing seed.");
                e.printStackTrace();
            }

            try {
                File world = new File(Bukkit.getWorldContainer(), "world").toPath().normalize().toFile();
                if (world.exists()) {
                    FileUtils.deleteDirectory(world);
                    System.out.println("[FIB] World deleted successfully.");
                }

                world.mkdirs();
                new File(world, "datapacks").mkdirs();
                this.copyDatapack("FIB_Worldgen");
                System.out.println("[FIB] Datapack copied.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        Bukkit.restart();
    }

    private void copyDatapack(String datapackName) {
        File world = new File(Bukkit.getWorldContainer(), "world");

        try {
            Path sourceDirectory = Paths.get(this.dataFolder + "/" + datapackName + ".zip");
            Path destinationDirectory = Paths.get(world + "/datapacks/" + datapackName + ".zip");

            Files.walk(sourceDirectory)
                    .forEach(source -> {
                        try {
                            Path destination = destinationDirectory.resolve(sourceDirectory.relativize(source));
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            System.out.println("Directory copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Rewrites the {@code level-seed} line in place, appending it when the file has none. */
    private static void writeLevelSeed(String value) throws IOException {
        File props = new File("server.properties");
        if (!props.isFile()) {
            System.out.println("[FIB] server.properties not found; cannot set level-seed.");
            return;
        }

        List<String> lines = Files.readAllLines(props.toPath(), StandardCharsets.UTF_8);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("level-seed=")) {
                lines.set(i, "level-seed=" + value);
                found = true;
                break;
            }
        }
        if (!found) {
            lines.add("level-seed=" + value);
        }

        Files.write(props.toPath(), lines, StandardCharsets.UTF_8);
        System.out.println("[FIB] level-seed set to '" + value + "'.");
    }
}
