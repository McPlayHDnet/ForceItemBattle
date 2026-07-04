package forceitembattle.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FileLogger {

    private static File dataFolder;

    private FileLogger() {
    }

    public static void init(File dataFolder) {
        FileLogger.dataFolder = dataFolder;
    }

    public static void log(String message) {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File saveTo = new File(dataFolder, "logs_plugin.txt");
            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(saveTo, true))) {
                pw.println("[" + timestamp() + "] | " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}