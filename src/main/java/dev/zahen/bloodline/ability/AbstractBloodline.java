package dev.zahen.bloodline.ability;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.manager.BloodlineManager;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public abstract class AbstractBloodline implements Bloodline {

    protected final BloodlinePlugin plugin;
    private final BloodlineType type;

    protected AbstractBloodline(BloodlinePlugin plugin, BloodlineType type) {
        this.plugin = plugin;
        this.type = type;
    }

    protected BloodlineType type() {
        return type;
    }

    protected PlayerProfile profile(Player player) {
        return manager().profile(player);
    }

    protected int level(Player player) {
        return profile(player).level(type);
    }

    protected long configSeconds(String path) {
        return plugin.getConfig().getLong("bloodlines." + type.key() + "." + path);
    }

    protected long remaining(Player player, String key) {
        return manager().remainingCooldown(profile(player), key);
    }

    protected boolean startCooldown(Player player, String key, long baseSeconds, long reductionPerLevel, long minSeconds) {
        return manager().startCooldown(player, key, baseSeconds, reductionPerLevel, minSeconds);
    }

    protected BloodlineManager manager() {
        return plugin.getBloodlineManager();
    }

    protected void activated(Player player, String message) {
        player.sendActionBar(Component.text(message, NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1F, 1.2F);
    }

    protected void cooldown(Player player, String key) {
        player.sendActionBar(Component.text("Ability on cooldown: " + manager().formatCooldown(profile(player), key), NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1F, 0.7F);
    }

    protected List<Component> passiveLines(String... text) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (String line : text) {
            lines.add(Component.text(line, NamedTextColor.GRAY));
        }
        return lines;
    }
}
