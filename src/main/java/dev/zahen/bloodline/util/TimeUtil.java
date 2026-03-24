package dev.zahen.bloodline.util;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String formatMillis(long millis) {
        if (millis <= 0L) {
            return "READY";
        }

        long totalSeconds = Math.max(1L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return "%dh %02dm".formatted(hours, minutes);
        }
        return "%d:%02d".formatted(minutes, seconds);
    }
}
