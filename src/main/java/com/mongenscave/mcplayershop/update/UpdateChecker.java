package com.mongenscave.mcplayershop.update;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class UpdateChecker {

    public static final String UPDATE_PERMISSION = "playershop.update-notify";

    private static final String API_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final String RESOURCE_URL = "https://www.spigotmc.org/resources/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 30L;

    private final McPlayerShop plugin = McPlayerShop.getInstance();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final int resourceId;

    public UpdateChecker(int resourceId) {
        this.resourceId = resourceId;
    }

    public void start() {
        if (!ConfigKeys.UPDATE_CHECKER_ENABLED.getBoolean()) return;

        McPlayerShop.getScheduler().runTaskTimerAsynchronously(this::check, 1L, CHECK_INTERVAL_TICKS);
    }

    private void check() {
        String latest = fetchLatestVersion();
        if (latest == null) return;

        String current = plugin.getDescription().getVersion();
        if (current.equalsIgnoreCase(latest)) return;

        notifyUpdate(current, latest);
    }

    private @Nullable String fetchLatestVersion() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + resourceId))
                .timeout(TIMEOUT)
                .header("User-Agent", "mc-PlayerShop-UpdateChecker")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LoggerUtils.warn("Unable to check for updates: HTTP {}", response.statusCode());
                return null;
            }

            String version = response.body().trim();
            return version.isEmpty() ? null : version;
        } catch (IOException exception) {
            LoggerUtils.warn("Unable to check for updates: {}", exception.getMessage());
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void notifyUpdate(String current, String latest) {
        LoggerUtils.warn("A new update is available! Current: {}, Latest: {}", current, latest);
        LoggerUtils.warn("Download it at: {}{}", RESOURCE_URL, resourceId);

        String message = MessageKeys.UPDATE_AVAILABLE.getMessage()
                .replace("{current}", current)
                .replace("{new}", latest);

        McPlayerShop.getScheduler().runTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(UPDATE_PERMISSION)) player.sendMessage(message);
            }
        });
    }
}