package forceitembattle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class PasteRSUploader {

    private static final String PASTE_URL = "https://paste.rs";

    public static CompletableFuture<String> uploadLog() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Read the full log file
                String logContent = Files.readString(Paths.get("logs/latest.log"), StandardCharsets.UTF_8);

                // Upload to paste.rs
                return uploadToPasteRS(logContent);
            } catch (IOException e) {
                return "§cError reading log file: " + e.getMessage();
            }
        });
    }

    private static String uploadToPasteRS(String content) {
        try {
            // Setup the connection
            URL url = new URI(PASTE_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

            // Write the log content to the output stream
            try (OutputStream os = conn.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Read the response (URL of the uploaded content)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return "§aLog uploaded: " + reader.readLine();
            }
        } catch (Exception e) {
            return "§cUpload failed: " + e.getMessage();
        }
    }

}
