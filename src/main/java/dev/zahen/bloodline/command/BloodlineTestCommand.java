package dev.zahen.bloodline.command;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class BloodlineTestCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public BloodlineTestCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("testing.enable-test-commands", true)) {
            sender.sendMessage("Test commands are disabled in config.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/bloodlinetest set <player> <bloodline> <level>");
            sender.sendMessage("/bloodlinetest give <player> <bloodline> <level>");
            sender.sendMessage("/bloodlinetest active <player> <bloodline>");
            sender.sendMessage("/bloodlinetest maxall <player>");
            sender.sendMessage("/bloodlinetest reroll <player> [animate]");
            sender.sendMessage("/bloodlinetest rerollall [animate]");
            sender.sendMessage("/bloodlinetest grace <start|stop|set> [duration]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 4) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                BloodlineType type = BloodlineType.fromKey(args[2]);
                Integer level = parseLevel(args[3]);
                if (target == null || type == null || level == null) {
                    return true;
                }
                plugin.getBloodlineManager().forceActiveBloodline(target, type, level);
                sender.sendMessage("Set " + target.getName() + " to " + type.displayName() + " level " + level + ".");
                return true;
            }
            case "give" -> {
                if (args.length < 4) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                BloodlineType type = BloodlineType.fromKey(args[2]);
                Integer level = parseLevel(args[3]);
                if (target == null || type == null || level == null) {
                    return true;
                }
                plugin.getBloodlineManager().grantBloodline(target, type, level);
                sender.sendMessage("Granted " + type.displayName() + " level " + level + " to " + target.getName() + ".");
                return true;
            }
            case "active" -> {
                if (args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                BloodlineType type = BloodlineType.fromKey(args[2]);
                if (target == null || type == null) {
                    return true;
                }
                PlayerProfile profile = plugin.getBloodlineManager().profile(target);
                int level = Math.max(1, profile.level(type));
                plugin.getBloodlineManager().forceActiveBloodline(target, type, level);
                sender.sendMessage("Switched " + target.getName() + " to " + type.displayName() + ".");
                return true;
            }
            case "maxall" -> {
                if (args.length < 2) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    return true;
                }
                plugin.getBloodlineManager().maxAllBaseBloodlines(target);
                sender.sendMessage("Maxed all base bloodlines for " + target.getName() + ".");
                return true;
            }
            case "reroll" -> {
                if (args.length < 2) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    return true;
                }
                boolean animate = args.length < 3 || Boolean.parseBoolean(args[2]);
                BloodlineType rolled = plugin.getBloodlineManager().rerollInitialBloodline(target, animate);
                sender.sendMessage("Rerolled " + target.getName() + " to " + rolled.displayName() + ".");
                return true;
            }
            case "rerollall" -> {
                boolean animate = args.length < 2 || Boolean.parseBoolean(args[1]);
                for (Player target : Bukkit.getOnlinePlayers()) {
                    BloodlineType rolled = plugin.getBloodlineManager().rerollInitialBloodline(target, animate);
                    sender.sendMessage("Rerolled " + target.getName() + " to " + rolled.displayName() + ".");
                }
                return true;
            }
            case "grace" -> {
                if (args.length < 2) {
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "start" -> plugin.getGracePeriodManager().startGracePeriod(sender);
                    case "stop" -> plugin.getGracePeriodManager().stopGracePeriod(sender, true);
                    case "set" -> {
                        if (args.length < 3) {
                            return true;
                        }
                        Long seconds = parseDurationSeconds(args[2]);
                        if (seconds != null) {
                            plugin.getGracePeriodManager().setConfiguredSeconds(sender, seconds);
                        }
                    }
                    default -> {
                    }
                }
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("set", "give", "active", "maxall", "reroll", "rerollall", "grace");
        }
        if (args.length == 2 && List.of("set", "give", "active", "maxall", "reroll").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("grace")) {
            return List.of("start", "stop", "set");
        }
        if (args.length == 3 && List.of("set", "give", "active").contains(args[0].toLowerCase())) {
            return Arrays.stream(BloodlineType.values()).map(BloodlineType::key).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reroll")) {
            return List.of("true", "false");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rerollall")) {
            return List.of("true", "false");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grace") && args[1].equalsIgnoreCase("set")) {
            return List.of("5m", "10m", "300s");
        }
        if (args.length == 4 && List.of("set", "give").contains(args[0].toLowerCase())) {
            return List.of("1", "2", "3", "4", "5");
        }
        return List.of();
    }

    private Integer parseLevel(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return null;
        }
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
