package dev.zahen.bloodline.manager;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.util.TimeUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GracePeriodManager {

    private final BloodlinePlugin plugin;
    private BossBar bossBar;
    private long configuredSeconds;
    private long graceEndsAt;
    private boolean fiveSecondCountdownShown;

    public GracePeriodManager(BloodlinePlugin plugin) {
        this.plugin = plugin;
        this.configuredSeconds = plugin.getConfig().getLong("grace.default-seconds", 300L);
    }

    public void start() {
        bossBar = BossBar.bossBar(Component.text("Grace Period", NamedTextColor.AQUA), 1.0F, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 20L);
        applyPvpRule(true);
    }

    public void shutdown() {
        if (bossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(bossBar);
            }
        }
        applyPvpRule(true);
    }

    public boolean isGraceActive() {
        return graceEndsAt > System.currentTimeMillis();
    }

    public void startGracePeriod(CommandSender sender) {
        graceEndsAt = System.currentTimeMillis() + (configuredSeconds * 1000L);
        fiveSecondCountdownShown = false;
        applyPvpRule(false);
        showBossBarToAll();
        Bukkit.broadcast(Component.text("Grace period started. PvP is disabled.", NamedTextColor.GREEN));
        sender.sendMessage("Grace period started for " + configuredSeconds + " seconds.");
    }

    public void stopGracePeriod(CommandSender sender, boolean announce) {
        boolean wasActive = isGraceActive();
        graceEndsAt = 0L;
        fiveSecondCountdownShown = false;
        applyPvpRule(true);
        hideBossBarFromAll();
        if (announce && wasActive) {
            Bukkit.broadcast(Component.text("Grace period ended. PvP is enabled.", NamedTextColor.RED));
        }
        sender.sendMessage("Grace period stopped.");
    }

    public void setConfiguredSeconds(CommandSender sender, long seconds) {
        configuredSeconds = seconds;
        plugin.getConfig().set("grace.default-seconds", seconds);
        plugin.saveConfig();
        sender.sendMessage("Grace time set to " + seconds + " seconds.");
    }

    public void reloadFromConfig() {
        configuredSeconds = plugin.getConfig().getLong("grace.default-seconds", 300L);
    }

    private void tick() {
        if (!isGraceActive()) {
            hideBossBarFromAll();
            return;
        }

        long remaining = graceEndsAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            stopGracePeriod(Bukkit.getConsoleSender(), true);
            return;
        }

        long fiveSecondsMillis = 5_000L;
        if (!fiveSecondCountdownShown && remaining <= fiveSecondsMillis) {
            fiveSecondCountdownShown = true;
            startFiveSecondCountdown();
        }

        float progress = Math.max(0.0F, Math.min(1.0F, remaining / (configuredSeconds * 1000.0F)));
        bossBar.name(Component.text("Grace Period: " + TimeUtil.formatMillis(remaining), NamedTextColor.AQUA));
        bossBar.progress(progress);
        showBossBarToAll();
    }

    private void startFiveSecondCountdown() {
        for (int i = 5; i >= 1; i--) {
            int seconds = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                NamedTextColor color = switch (seconds) {
                    case 5 -> NamedTextColor.GREEN;
                    case 4 -> NamedTextColor.YELLOW;
                    case 3 -> NamedTextColor.GOLD;
                    case 2 -> NamedTextColor.RED;
                    default -> NamedTextColor.DARK_RED;
                };
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(Title.title(
                            Component.text(String.valueOf(seconds), color),
                            Component.text("PvP returning", color),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200))
                    ));
                }
            }, (5 - i) * 20L);
        }
    }

    private void showBossBarToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    private void hideBossBarFromAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    private void applyPvpRule(boolean enabled) {
        String value = enabled ? "true" : "false";
        Bukkit.getWorlds().forEach(world ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey().asString() + " run gamerule pvp " + value)
        );
    }
}
