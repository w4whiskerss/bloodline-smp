package dev.zahen.bloodline.command;

import dev.zahen.bloodline.BloodlinePlugin;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public final class BloodlineReloadCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public BloodlineReloadCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadConfig();
        plugin.reloadGameplaySettings();
        plugin.initializeSpecialWorlds();
        plugin.getGracePeriodManager().reloadFromConfig();
        sender.sendMessage("BloodlineSMP config reloaded.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
