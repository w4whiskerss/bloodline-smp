package dev.zahen.bloodline.config;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public final class GameplaySettings {

    private final GameplayMode gameplayMode;
    private final boolean worldsEnabled;
    private final Set<String> disabledWorlds;
    private final boolean testCommandsEnabled;
    private final boolean adminMode;

    private GameplaySettings(
            GameplayMode gameplayMode,
            boolean worldsEnabled,
            Set<String> disabledWorlds,
            boolean testCommandsEnabled,
            boolean adminMode
    ) {
        this.gameplayMode = gameplayMode;
        this.worldsEnabled = worldsEnabled;
        this.disabledWorlds = Set.copyOf(disabledWorlds);
        this.testCommandsEnabled = testCommandsEnabled;
        this.adminMode = adminMode;
    }

    public static GameplaySettings fromConfig(FileConfiguration config) {
        Set<String> disabledWorlds = new HashSet<>();
        for (String worldName : config.getStringList("worlds.disabled")) {
            if (worldName != null && !worldName.isBlank()) {
                disabledWorlds.add(normalize(worldName));
            }
        }
        return new GameplaySettings(
                GameplayMode.fromConfig(config.getString("mode", "scripted")),
                config.getBoolean("worlds.enabled", false),
                disabledWorlds,
                config.getBoolean("testing.enable-test-commands", true),
                config.getBoolean("admin-mode", false)
        );
    }

    public GameplayMode gameplayMode() {
        return gameplayMode;
    }

    public boolean isPublicMode() {
        return gameplayMode == GameplayMode.PUBLIC;
    }

    public boolean isScriptedMode() {
        return gameplayMode == GameplayMode.SCRIPTED;
    }

    public boolean testCommandsEnabled() {
        return testCommandsEnabled;
    }

    public boolean adminMode() {
        return adminMode;
    }

    public boolean isBloodlineGameplayDisabled(World world) {
        if (!worldsEnabled || world == null) {
            return false;
        }
        return disabledWorlds.contains(normalize(world.getName()));
    }

    public boolean isBloodlineGameplayDisabled(String worldName) {
        if (!worldsEnabled || worldName == null || worldName.isBlank()) {
            return false;
        }
        return disabledWorlds.contains(normalize(worldName));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
