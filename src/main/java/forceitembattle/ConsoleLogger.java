package forceitembattle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ConsoleLogger {
    private static final String LOG_FILE_PATH = "logs/latest.log";  // Adjust this path if needed

    public static void startCapturing() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) logFile.createNewFile();

            PrintStream logStream = new PrintStream(new FileOutputStream(logFile, true), true, StandardCharsets.UTF_8);

            // Redirect both System.out and System.err
            System.setOut(new PrintStream(new MultiOutputStream(System.out, logStream), true));
            System.setErr(new PrintStream(new MultiOutputStream(System.err, logStream), true));

            System.out.println("[ConsoleLogger] Capturing all logs...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MultiOutputStream extends OutputStream {
        private final OutputStream[] streams;

        public MultiOutputStream(OutputStream... streams) {
            this.streams = streams;
        }

        @Override
        public void write(int b) throws IOException {
            for (OutputStream stream : streams) {
                stream.write(b);
                stream.flush();  // Ensure immediate logging
            }
        }
    }
}
