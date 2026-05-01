package dev.zahen.bloodline.config;

public enum GameplayMode {
    SCRIPTED,
    PUBLIC;

    public static GameplayMode fromConfig(String rawValue) {
        if (rawValue == null) {
            return SCRIPTED;
        }
        return switch (rawValue.trim().toLowerCase()) {
            case "public", "public-smp", "public_smp" -> PUBLIC;
            case "scripted" -> SCRIPTED;
            default -> SCRIPTED;
        };
    }
}
