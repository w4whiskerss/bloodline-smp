package dev.zahen.bloodline.update;

import dev.zahen.bloodline.BloodlinePlugin;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginUpdater {

    private static final Pattern ASSET_PATTERN = Pattern.compile(
            "\\{[^{}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"[^{}]*}",
            Pattern.DOTALL
    );

    private final BloodlinePlugin plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PluginUpdater(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdatesAsync() {
        if (!plugin.getConfig().getBoolean("auto-update.enabled", false)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<ReleaseAsset> latestAsset = findLatestReleaseAsset();
                if (latestAsset.isEmpty()) {
                    plugin.getLogger().info("Auto-update: no matching release asset found.");
                    return;
                }

                ReleaseAsset asset = latestAsset.get();
                String currentVersion = plugin.getPluginMeta().getVersion();
                if (compareVersions(asset.version(), currentVersion) <= 0) {
                    plugin.getLogger().info("Auto-update: already on latest version (" + currentVersion + ").");
                    return;
                }

                Path currentJar = resolveCurrentPluginJar();
                downloadReleaseAsset(asset, currentJar);
                cleanupOldPluginJars(currentJar);
                plugin.getLogger().info("Auto-update: downloaded " + asset.version()
                        + " into plugins folder. Restart again to load the new version.");
            } catch (Exception exception) {
                plugin.getLogger().warning("Auto-update failed: " + exception.getMessage());
            }
        });
    }

    private Optional<ReleaseAsset> findLatestReleaseAsset() throws IOException, InterruptedException {
        String apiUrl = plugin.getConfig().getString(
                "auto-update.github-release-api",
                "https://api.github.com/repos/w4whiskerss/bloodline-smp/releases/latest"
        );
        String assetPrefix = plugin.getConfig().getString("auto-update.asset-prefix", "bloodline-smp-");
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "BloodlineSMP-Updater")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub API returned HTTP " + response.statusCode());
        }

        List<ReleaseAsset> assets = parseReleaseAssets(response.body(), assetPrefix);
        return assets.stream().max(Comparator.comparing(ReleaseAsset::version, this::compareVersions));
    }

    private List<ReleaseAsset> parseReleaseAssets(String json, String assetPrefix) {
        List<ReleaseAsset> assets = new ArrayList<>();
        Matcher matcher = ASSET_PATTERN.matcher(json);
        while (matcher.find()) {
            String name = matcher.group(1);
            String url = matcher.group(2).replace("\\/", "/");
            if (!name.startsWith(assetPrefix) || !name.endsWith(".jar")) {
                continue;
            }
            String version = name.substring(assetPrefix.length(), name.length() - 4);
            if (!version.matches("\\d+\\.\\d+\\.\\d+")) {
                continue;
            }
            assets.add(new ReleaseAsset(name, version, url));
        }
        return assets;
    }

    private Path resolveCurrentPluginJar() throws URISyntaxException {
        return Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private void downloadReleaseAsset(ReleaseAsset asset, Path targetJar) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("bloodline-smp-update-", ".jar");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(asset.downloadUrl()))
                    .header("User-Agent", "BloodlineSMP-Updater")
                    .GET()
                    .build();
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Release download returned HTTP " + response.statusCode());
            }
            Files.copy(tempFile, targetJar, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void cleanupOldPluginJars(Path activeJar) throws IOException {
        Path pluginsDir = activeJar.getParent();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return;
        }
        String prefix = plugin.getConfig().getString("auto-update.asset-prefix", "bloodline-smp-").toLowerCase(Locale.ROOT);
        try (var paths = Files.list(pluginsDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(prefix))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .filter(path -> !path.equals(activeJar))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            plugin.getLogger().warning("Auto-update could not remove old jar " + path.getFileName() + ": " + exception.getMessage());
                        }
                    });
        }
    }

    private int compareVersions(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            int leftValue = index < leftParts.length ? leftParts[index] : 0;
            int rightValue = index < rightParts.length ? rightParts[index] : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private int[] parseVersion(String version) {
        return java.util.Arrays.stream(version.split("\\."))
                .mapToInt(part -> {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .toArray();
    }

    private record ReleaseAsset(String name, String version, String downloadUrl) {
    }
}
