package dev.whiskers.bloodline.command;

import dev.whiskers.bloodline.BloodlinePlugin;
import dev.whiskers.bloodline.model.BloodlineType;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class BloodDomainWandCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public BloodDomainWandCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.isOp() && !plugin.getAdminPanelGui().canAccess(player)) {
            player.sendMessage("You are not allowed to use the domain wand.");
            return true;
        }

        if (args.length == 0) {
            player.getInventory().addItem(plugin.getBloodlineManager().createDomainWand());
            player.sendMessage("Blood Domain Wand granted. Left click = Pos 1, right click = Pos 2, then /blooddomainwand save <bloodline>.");
            return true;
        }

        if (args[0].equalsIgnoreCase("save") && args.length >= 2) {
            BloodlineType type = BloodlineType.fromKey(args[1]);
            if (type == null) {
                player.sendMessage("Unknown bloodline.");
                return true;
            }
            if (plugin.getBloodlineManager().saveSelectedDomain(player, type)) {
                player.sendMessage("Saved custom domain for " + type.displayName() + ".");
            } else {
                player.sendMessage("You need two positions in the same world first.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("clear") && args.length >= 2) {
            BloodlineType type = BloodlineType.fromKey(args[1]);
            if (type == null) {
                player.sendMessage("Unknown bloodline.");
                return true;
            }
            if (plugin.getBloodlineManager().clearCustomDomain(type)) {
                player.sendMessage("Cleared custom domain for " + type.displayName() + ".");
            } else {
                player.sendMessage("No custom domain saved for that bloodline.");
            }
            return true;
        }

        player.sendMessage("/blooddomainwand");
        player.sendMessage("/blooddomainwand save <bloodline>");
        player.sendMessage("/blooddomainwand clear <bloodline>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("save", "clear");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("clear"))) {
            return java.util.Arrays.stream(BloodlineType.values()).map(BloodlineType::key).toList();
        }
        return List.of();
    }
}
