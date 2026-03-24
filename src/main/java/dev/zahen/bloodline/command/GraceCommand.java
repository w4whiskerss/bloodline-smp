package dev.zahen.bloodline.command;

import dev.zahen.bloodline.BloodlinePlugin;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class GraceCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public GraceCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !(sender instanceof Player player && player.isOp())) {
            sender.sendMessage("Operators or console only.");
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "gracestart" -> {
                plugin.getGracePeriodManager().startGracePeriod(sender);
                yield true;
            }
            case "gracestop" -> {
                plugin.getGracePeriodManager().stopGracePeriod(sender, true);
                yield true;
            }
            case "gracetimeset" -> {
                if (args.length != 1) {
                    sender.sendMessage("Usage: /gracetimeset <duration>");
                    sender.sendMessage("Examples: /gracetimeset 300, /gracetimeset 5m, /gracetimeset 90s");
                    yield true;
                }
                Long seconds = parseDurationSeconds(args[0]);
                if (seconds == null || seconds <= 0L) {
                    sender.sendMessage("Invalid duration.");
                    yield true;
                }
                plugin.getGracePeriodManager().setConfiguredSeconds(sender, seconds);
                yield true;
            }
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("gracetimeset") && args.length == 1) {
            return List.of("5m", "10m", "300s", "600");
        }
        return List.of();
    }

    private Long parseDurationSeconds(String input) {
        String lower = input.trim().toLowerCase();
        try {
            if (lower.endsWith("m")) {
                return Long.parseLong(lower.substring(0, lower.length() - 1)) * 60L;
            }
            if (lower.endsWith("s")) {
                return Long.parseLong(lower.substring(0, lower.length() - 1));
            }
            return Long.parseLong(lower);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
