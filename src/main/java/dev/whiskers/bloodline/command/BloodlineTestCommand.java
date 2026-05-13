package dev.whiskers.bloodline.command;

import dev.whiskers.bloodline.BloodlinePlugin;
import dev.whiskers.bloodline.model.BloodlineType;
import dev.whiskers.bloodline.model.PlayerProfile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BloodlineTestCommand implements TabExecutor {

    private final BloodlinePlugin plugin;

    public BloodlineTestCommand(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getGameplaySettings().testCommandsEnabled()) {
            sender.sendMessage("Test commands are disabled in config.");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getTestItemsGui().open(player);
                return true;
            }
            sender.sendMessage("/bloodlinetest set <player> <bloodline> <level>");
            sender.sendMessage("/bloodlinetest give <player> <bloodline> <level>");
            sender.sendMessage("/bloodlinetest active <player> <bloodline>");
            sender.sendMessage("/bloodlinetest maxall <player>");
            sender.sendMessage("/bloodlinetest reroll <player> [animate]");
            sender.sendMessage("/bloodlinetest rerollall [animate]");
            sender.sendMessage("/bloodlinetest grace <start|stop|set> [duration]");
            sender.sendMessage("/bloodlinetest debug <zero|restore|clear|sync> <player> [true|false]");
            sender.sendMessage("/bloodlinetest mace model <number>");
            sender.sendMessage("/bloodlinetest mace name <text>|reset");
            sender.sendMessage("/bloodlinetest mace lore add <text>");
            sender.sendMessage("/bloodlinetest mace lore set <line1||line2...>");
            sender.sendMessage("/bloodlinetest mace lore clear|reset");
            sender.sendMessage("/bloodlinetest givebloodmace [player]");
            sender.sendMessage("/bloodlinetest giveitem <player> <item> [bloodline] [level] [amount]");
            sender.sendMessage("/bloodlinetest get <item> [bloodline] [level] [amount]");
            sender.sendMessage("/bloodlinetest items");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "items" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can open the test items panel.");
                    return true;
                }
                plugin.getTestItemsGui().open(player);
                return true;
            }
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
            case "debug" -> {
                if (args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "zero" -> {
                        boolean enabled = args.length < 4 || Boolean.parseBoolean(args[3]);
                        plugin.getBloodlineManager().setZeroCooldownMode(target, enabled);
                        sender.sendMessage((enabled ? "Enabled" : "Disabled") + " zero cooldown mode for " + target.getName() + ".");
                    }
                    case "restore" -> {
                        plugin.getBloodlineManager().setZeroCooldownMode(target, false);
                        sender.sendMessage("Restored normal cooldowns for " + target.getName() + ".");
                    }
                    case "clear" -> {
                        plugin.getBloodlineManager().clearCooldowns(target);
                        sender.sendMessage("Cleared cooldowns for " + target.getName() + ".");
                    }
                    case "sync" -> {
                        plugin.getBloodlineManager().pushClientState(target, true);
                        sender.sendMessage("Forced HUD sync for " + target.getName() + ".");
                    }
                    default -> {
                        return true;
                    }
                }
                return true;
            }
            case "mace" -> {
                if (!(sender instanceof Player player) || args.length < 3) {
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "model" -> {
                        try {
                            int model = Integer.parseInt(args[2]);
                            if (plugin.getBloodlineManager().setHeldBloodMaceModel(player, model)) {
                                sender.sendMessage("Updated held Blood Mace model to " + model + ".");
                            } else {
                                sender.sendMessage("Hold a Blood Mace first.");
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    case "name" -> {
                        if ("reset".equalsIgnoreCase(args[2])) {
                            sender.sendMessage(plugin.getBloodlineManager().resetHeldBloodMaceName(player)
                                    ? "Reset held Blood Mace name."
                                    : "Hold a Blood Mace first.");
                            return true;
                        }
                        String name = joinArgs(args, 2);
                        sender.sendMessage(plugin.getBloodlineManager().setHeldBloodMaceName(player, Component.text(name))
                                ? "Updated held Blood Mace name."
                                : "Hold a Blood Mace first.");
                    }
                    case "lore" -> {
                        if (args.length < 3) {
                            return true;
                        }
                        switch (args[2].toLowerCase()) {
                            case "clear" -> sender.sendMessage(plugin.getBloodlineManager().setHeldBloodMaceLore(player, List.of())
                                    ? "Cleared held Blood Mace lore."
                                    : "Hold a Blood Mace first.");
                            case "reset" -> sender.sendMessage(plugin.getBloodlineManager().resetHeldBloodMaceLore(player)
                                    ? "Reset held Blood Mace lore."
                                    : "Hold a Blood Mace first.");
                            case "add" -> {
                                String line = joinArgs(args, 3);
                                ItemStack held = player.getInventory().getItemInMainHand();
                                if (!plugin.getCustomItems().isBloodMace(held) || !held.hasItemMeta()) {
                                    sender.sendMessage("Hold a Blood Mace first.");
                                    return true;
                                }
                                List<Component> lore = held.getItemMeta().lore();
                                java.util.ArrayList<Component> nextLore = new java.util.ArrayList<>(lore == null ? List.of() : lore);
                                nextLore.add(Component.text(line));
                                plugin.getBloodlineManager().setHeldBloodMaceLore(player, nextLore);
                                sender.sendMessage("Added a lore line to the held Blood Mace.");
                            }
                            case "set" -> {
                                String raw = joinArgs(args, 3);
                                List<Component> lore = Arrays.stream(raw.split("\\|\\|", -1))
                                        .map(String::trim)
                                        .filter(line -> !line.isEmpty())
                                        .map(Component::text)
                                        .collect(Collectors.toList());
                                sender.sendMessage(plugin.getBloodlineManager().setHeldBloodMaceLore(player, lore)
                                        ? "Set held Blood Mace lore."
                                        : "Hold a Blood Mace first.");
                            }
                            default -> {
                                return true;
                            }
                        }
                    }
                    default -> {
                        return true;
                    }
                }
                return true;
            }
            case "givebloodmace" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                ItemStack item = plugin.getCustomItems().createBloodMace(1, plugin.getBloodlineManager().craftedBloodMaces(), 0);
                giveItem(target, item);
                sender.sendMessage("Gave Blood Mace to " + target.getName() + ".");
                return true;
            }
            case "giveitem" -> {
                if (args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                ItemStack item = buildTestItem(args, 2);
                if (item == null) {
                    sender.sendMessage("Unknown item or invalid args.");
                    return true;
                }
                giveItem(target, item);
                sender.sendMessage("Gave " + args[2] + " to " + target.getName() + ".");
                return true;
            }
            case "get" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    return true;
                }
                ItemStack item = buildTestItem(args, 1);
                if (item == null) {
                    sender.sendMessage("Unknown item or invalid args.");
                    return true;
                }
                giveItem(player, item);
                sender.sendMessage("Given " + args[1] + ".");
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
            return List.of("items", "set", "give", "active", "maxall", "reroll", "rerollall", "grace", "debug", "mace", "givebloodmace", "giveitem", "get");
        }
        if (args.length == 2 && List.of("set", "give", "active", "maxall", "reroll").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givebloodmace")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            return itemNames();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            return itemNames();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return List.of("zero", "restore", "clear", "sync");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("grace")) {
            return List.of("start", "stop", "set");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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
        if (args.length == 2 && args[0].equalsIgnoreCase("mace")) {
            return List.of("model", "name", "lore");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mace") && args[1].equalsIgnoreCase("model")) {
            return List.of("0", "1", "10", "100");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mace") && args[1].equalsIgnoreCase("name")) {
            return List.of("reset");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mace") && args[1].equalsIgnoreCase("lore")) {
            return List.of("add", "set", "clear", "reset");
        }
        if ((args[0].equalsIgnoreCase("get") && args.length == 3)
                || (args[0].equalsIgnoreCase("giveitem") && args.length == 4)) {
            return Arrays.stream(BloodlineType.values()).map(BloodlineType::key).toList();
        }
        if ((args[0].equalsIgnoreCase("get") && args.length == 4)
                || (args[0].equalsIgnoreCase("giveitem") && args.length == 5)) {
            return List.of("1", "2", "3", "4", "5");
        }
        if ((args[0].equalsIgnoreCase("get") && args.length == 5)
                || (args[0].equalsIgnoreCase("giveitem") && args.length == 6)) {
            return List.of("1", "5", "16", "64");
        }
        if (args.length == 4 && List.of("set", "give").contains(args[0].toLowerCase())) {
            return List.of("1", "2", "3", "4", "5");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("zero")) {
            return List.of("true", "false");
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

    private ItemStack buildTestItem(String[] args, int itemIndex) {
        String itemName = args[itemIndex].toLowerCase();
        BloodlineType type = args.length > itemIndex + 1 ? BloodlineType.fromKey(args[itemIndex + 1]) : BloodlineType.AQUA;
        int level = args.length > itemIndex + 2 ? parseLevelOrDefault(args[itemIndex + 2], 1) : 1;
        int amount = args.length > itemIndex + 3 ? parseLevelOrDefault(args[itemIndex + 3], 1) : 1;

        ItemStack item = switch (itemName) {
            case "bloodmace", "blood_mace", "mace" -> plugin.getCustomItems().createBloodMace(1, plugin.getBloodlineManager().craftedBloodMaces(), 0);
            case "blooddrop", "blood_drop", "blood" -> plugin.getCustomItems().createBloodDrop();
            case "bloodblock", "blood_block" -> plugin.getCustomItems().createBloodBlock();
            case "upgrade", "upgradepotion", "upgrade_potion" -> plugin.getCustomItems().createUpgradePotion();
            case "trait", "traitpotion", "trait_potion" -> type == null ? null : plugin.getCustomItems().createTraitPotion(type, level);
            case "shard", "bloodlineshard", "bloodline_shard" -> type == null ? null : plugin.getCustomItems().createBloodlineShard(type);
            case "omnistar", "omni_star", "universalcore", "universal_core" -> plugin.getCustomItems().createUniversalCore();
            case "omniblade", "omni_blade" -> plugin.getCustomItems().createOmniBlade();
            case "voidelytra", "void_elytra", "voidflightelytra", "void_flight_elytra" -> plugin.getCustomItems().createVoidFlightElytra();
            case "domainwand", "domain_wand" -> plugin.getBloodlineManager().createDomainWand();
            default -> null;
        };
        if (item == null) {
            return null;
        }
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
        return item;
    }

    private int parseLevelOrDefault(String input, int fallback) {
        Integer parsed = parseLevel(input);
        return parsed == null ? fallback : parsed;
    }

    private List<String> itemNames() {
        return List.of(
                "blood_mace",
                "blood_drop",
                "blood_block",
                "upgrade_potion",
                "trait_potion",
                "bloodline_shard",
                "universal_core",
                "omni_blade",
                "void_flight_elytra",
                "domain_wand"
        );
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow));
    }

    private String joinArgs(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length)).trim();
    }
}
