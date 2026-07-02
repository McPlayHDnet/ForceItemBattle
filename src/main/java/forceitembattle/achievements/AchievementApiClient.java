package forceitembattle.achievements;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin async client for the FIBService achievement API.
 *
 * <p>Every call is non-blocking ({@link HttpClient#sendAsync}) and returns a
 * {@link CompletableFuture}, so this can be invoked from the main server thread
 * without stalling it. Bukkit-agnostic: callbacks may run on an HttpClient I/O
 * thread, so anything that must run on the main thread is the caller's job.
 *
 * <p>Failures are logged and surfaced as a {@code false}/empty result rather
 * than thrown, so a temporarily unreachable service never breaks gameplay.
 */
public class AchievementApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Logger logger;

    public AchievementApiClient(String baseUrl, Logger logger) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Fetches the set of achievement ids a player has unlocked (across all modes).
     * Resolves to an empty set on any error.
     */
    public CompletableFuture<Set<String>> fetchPlayerAchievements(UUID playerUuid) {
        HttpRequest request = baseRequest("/achievements/" + playerUuid)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("Failed to fetch achievements for " + playerUuid
                                + " (HTTP " + response.statusCode() + ")");
                        return new HashSet<String>();
                    }
                    return parseAchievementIds(response.body());
                })
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Error fetching achievements for " + playerUuid, throwable);
                    return new HashSet<>();
                });
    }

    /**
     * Unlocks (PUT, idempotent) a single achievement for a player in the given
     * mode. For {@code TEAM}, pass the teammate's UUID; for {@code SOLO}, pass null.
     */
    public CompletableFuture<Boolean> unlockAchievement(UUID playerUuid, String achievementId,
                                                        String mode, UUID teammateUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("mode", mode);
        if (teammateUuid != null) {
            body.addProperty("teammateUuid", teammateUuid.toString());
        }

        HttpRequest request = baseRequest("/achievements/" + playerUuid + "/" + achievementId)
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return send(request, "unlock " + achievementId + " (" + mode + ") for " + playerUuid);
    }

    /**
     * Removes a single achievement from a player (all modes).
     */
    public CompletableFuture<Boolean> removeAchievement(UUID playerUuid, String achievementId) {
        HttpRequest request = baseRequest("/achievements/" + playerUuid + "/" + achievementId)
                .DELETE()
                .build();

        return send(request, "remove " + achievementId + " for " + playerUuid);
    }

    /**
     * Deletes all achievements for a player.
     */
    public CompletableFuture<Boolean> resetPlayer(UUID playerUuid) {
        HttpRequest request = baseRequest("/achievements/" + playerUuid)
                .DELETE()
                .build();

        return send(request, "reset achievements for " + playerUuid);
    }

    private CompletableFuture<Boolean> send(HttpRequest request, String description) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    boolean ok = status >= 200 && status < 300;
                    if (!ok) {
                        logger.warning("Failed to " + description + " (HTTP " + status + ")");
                    }
                    return ok;
                })
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Error trying to " + description, throwable);
                    return false;
                });
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
    }

    private Set<String> parseAchievementIds(String body) {
        Set<String> ids = new HashSet<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray achievements = root.getAsJsonArray("achievements");
            if (achievements != null) {
                for (var element : achievements) {
                    JsonObject achievement = element.getAsJsonObject();
                    if (achievement.has("achievementId")) {
                        ids.add(achievement.get("achievementId").getAsString());
                    }
                }
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Could not parse achievements response: " + body, exception);
        }
        return ids;
    }
}