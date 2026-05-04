package dev.zahen.bloodline.command;

import dev.zahen.bloodline.BloodlinePlugin;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class BloodlineAdminCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public BloodlineAdminCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!plugin.getAdminPanelGui().canAccess(player)) {
            player.sendMessage("You are not allowed to use this admin panel.");
            return true;
        }
        plugin.getAdminPanelGui().openList(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
