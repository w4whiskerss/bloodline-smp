package dev.zahen.bloodline.command;

import dev.zahen.bloodline.BloodlinePlugin;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class AbilityCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public AbilityCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        String name = command.getName().toLowerCase();
        switch (name) {
            case "ability1", "primary" -> plugin.getBloodlineManager().triggerPrimary(player);
            case "ability2", "secondary" -> plugin.getBloodlineManager().triggerSecondary(player);
            case "ability3", "special" -> plugin.getBloodlineManager().triggerSpecial(player);
            default -> {
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
