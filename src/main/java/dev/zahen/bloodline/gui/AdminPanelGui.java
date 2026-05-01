package dev.zahen.bloodline.gui;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.data.PlayerDataManager.KnownPlayerRecord;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class AdminPanelGui {

    public static final String LIST_TITLE = "Bloodline Admin";
    public static final String EDIT_TITLE = "Edit Bloodline";
    public static final String DEBUG_TITLE = "Debug Bloodline";
    private static final int PAGE_SIZE = 45;

    private final BloodlinePlugin plugin;
    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> sessions = new ConcurrentHashMap<>();

    public AdminPanelGui(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canAccess(Player player) {
        if (!plugin.getConfig().getBoolean("admin-panel.enabled", true)) {
            return false;
        }
        List<String> allowed = plugin.getConfig().getStringList("admin-panel.allowed-usernames");
        return allowed.stream().anyMatch(name -> name.equalsIgnoreCase(player.getName()));
    }

    public void openList(Player player) {
        int page = Math.max(0, pages.getOrDefault(player.getUniqueId(), 0));
        List<KnownPlayerRecord> knownPlayers = plugin.getPlayerDataManager().knownPlayers();
        int maxPage = Math.max(0, (knownPlayers.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);
        pages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(player, 54, Component.text(LIST_TITLE, NamedTextColor.DARK_RED));
        int start = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < knownPlayers.size(); slot++) {
            inventory.setItem(slot, createPlayerHead(knownPlayers.get(start + slot)));
        }

        inventory.setItem(45, createControl(Material.ARROW, NamedTextColor.YELLOW, "Previous Page", page > 0 ? "Go to the previous page" : "No previous page"));
        inventory.setItem(49, createControl(Material.BOOK, NamedTextColor.GOLD, "Player List", "Page " + (page + 1) + "/" + (maxPage + 1)));
        inventory.setItem(53, createControl(Material.ARROW, NamedTextColor.YELLOW, "Next Page", page < maxPage ? "Go to the next page" : "No next page"));
        player.openInventory(inventory);
    }

    public void openEditor(Player player, UUID targetUuid) {
        PlayerProfile profile = plugin.getPlayerDataManager().getOrCreate(targetUuid);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        EditSession session = new EditSession(targetUuid, displayName(target, targetUuid), profile.activeBloodline(), Math.max(1, profile.activeLevel()));
        sessions.put(player.getUniqueId(), session);
        openEditor(player, session);
    }

    public boolean handleClick(Player player, Inventory inventory, int slot) {
        if (inventory == null) {
            return true;
        }
        if (inventory.getSize() == 54) {
            return handleListClick(player, slot);
        }
        if (inventory.getSize() == 27) {
            String title = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
            if (isDebugTitle(title)) {
                return handleDebugClick(player, slot);
            }
            return handleEditorClick(player, slot);
        }
        return false;
    }

    public boolean isListTitle(String title) {
        return LIST_TITLE.equals(title);
    }

    public boolean isEditTitle(String title) {
        return title.startsWith(EDIT_TITLE);
    }

    public boolean isDebugTitle(String title) {
        return title.startsWith(DEBUG_TITLE);
    }

    private boolean handleListClick(Player player, int slot) {
        List<KnownPlayerRecord> knownPlayers = plugin.getPlayerDataManager().knownPlayers();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int maxPage = Math.max(0, (knownPlayers.size() - 1) / PAGE_SIZE);

        if (slot == 45 && page > 0) {
            pages.put(player.getUniqueId(), page - 1);
            openList(player);
            return true;
        }
        if (slot == 53 && page < maxPage) {
            pages.put(player.getUniqueId(), page + 1);
            openList(player);
            return true;
        }
        if (slot < 0 || slot >= PAGE_SIZE) {
            return true;
        }

        int index = page * PAGE_SIZE + slot;
        if (index >= knownPlayers.size()) {
            return true;
        }
        openEditor(player, knownPlayers.get(index).uuid());
        return true;
    }

    private boolean handleEditorClick(Player player, int slot) {
        EditSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            openList(player);
            return true;
        }

        switch (slot) {
            case 10 -> session.setBloodline(BloodlineType.AQUA);
            case 11 -> session.setBloodline(BloodlineType.SPARTAN);
            case 12 -> session.setBloodline(BloodlineType.EARTHIAN);
            case 13 -> session.setBloodline(BloodlineType.VOIDER);
            case 14 -> session.setBloodline(BloodlineType.UNIVERSAL);
            case 15 -> session.setBloodline(BloodlineType.randomBase());
            case 21 -> session.setLevel(Math.max(1, session.level() - 1));
            case 22 -> session.setLevel(1);
            case 23 -> session.setLevel(Math.min(PlayerProfile.MAX_LEVEL, session.level() + 1));
            case 24 -> session.setLevel(PlayerProfile.MAX_LEVEL);
            case 16 -> {
                openDebug(player, session);
                return true;
            }
            case 25 -> {
                plugin.getBloodlineManager().adminSetBloodline(session.targetUuid(), session.bloodline(), session.level());
                sessions.remove(player.getUniqueId());
                player.sendMessage("Saved " + session.targetName() + " as " + session.bloodline().displayName() + " level " + session.level() + ".");
                openList(player);
                return true;
            }
            case 26 -> {
                sessions.remove(player.getUniqueId());
                openList(player);
                return true;
            }
            default -> {
            }
        }

        openEditor(player, session);
        return true;
    }

    private boolean handleDebugClick(Player player, int slot) {
        EditSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            openList(player);
            return true;
        }
        Player target = Bukkit.getPlayer(session.targetUuid());
        switch (slot) {
            case 10 -> {
                if (target != null) {
                    plugin.getBloodlineManager().setZeroCooldownMode(target, true);
                }
            }
            case 11 -> {
                if (target != null) {
                    plugin.getBloodlineManager().setZeroCooldownMode(target, false);
                }
            }
            case 12 -> {
                if (target != null) {
                    plugin.getBloodlineManager().clearCooldowns(target);
                }
            }
            case 13 -> {
                if (target != null) {
                    plugin.getBloodlineManager().pushClientState(target, true);
                }
            }
            case 14 -> {
                if (target != null) {
                    plugin.getBloodlineManager().rerollInitialBloodline(target, false);
                }
            }
            case 26 -> {
                openEditor(player, session);
                return true;
            }
            default -> {
            }
        }
        openDebug(player, session);
        return true;
    }

    private void openEditor(Player player, EditSession session) {
        Inventory inventory = Bukkit.createInventory(player, 27, Component.text(EDIT_TITLE + " - " + session.targetName(), NamedTextColor.DARK_RED));
        inventory.setItem(4, createSummary(session));
        inventory.setItem(10, createBloodlineChoice(BloodlineType.AQUA, session.bloodline()));
        inventory.setItem(11, createBloodlineChoice(BloodlineType.SPARTAN, session.bloodline()));
        inventory.setItem(12, createBloodlineChoice(BloodlineType.EARTHIAN, session.bloodline()));
        inventory.setItem(13, createBloodlineChoice(BloodlineType.VOIDER, session.bloodline()));
        inventory.setItem(14, createBloodlineChoice(BloodlineType.UNIVERSAL, session.bloodline()));
        inventory.setItem(15, createControl(Material.AMETHYST_SHARD, NamedTextColor.LIGHT_PURPLE, "Random Base", "Pick a random base bloodline"));
        inventory.setItem(21, createControl(Material.RED_STAINED_GLASS_PANE, NamedTextColor.RED, "Level -1", "Decrease level"));
        inventory.setItem(22, createLevelDisplay(session.level()));
        inventory.setItem(23, createControl(Material.LIME_STAINED_GLASS_PANE, NamedTextColor.GREEN, "Level +1", "Increase level"));
        inventory.setItem(24, createControl(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, NamedTextColor.GOLD, "Set Max", "Set level to 5"));
        inventory.setItem(16, createControl(Material.REDSTONE, NamedTextColor.LIGHT_PURPLE, "Debug", "Open debug actions"));
        inventory.setItem(25, createControl(Material.EMERALD_BLOCK, NamedTextColor.GREEN, "Save", "Apply these edits"));
        inventory.setItem(26, createControl(Material.BARRIER, NamedTextColor.RED, "Cancel", "Discard these edits"));
        player.openInventory(inventory);
    }

    private void openDebug(Player player, EditSession session) {
        Inventory inventory = Bukkit.createInventory(player, 27, Component.text(DEBUG_TITLE + " - " + session.targetName(), NamedTextColor.DARK_RED));
        inventory.setItem(4, createSummary(session));
        inventory.setItem(10, createControl(Material.CLOCK, NamedTextColor.LIGHT_PURPLE, "Zero Cooldown", "Enable zero cooldown mode"));
        inventory.setItem(11, createControl(Material.BARRIER, NamedTextColor.YELLOW, "Restore Cooldowns", "Disable zero cooldown mode"));
        inventory.setItem(12, createControl(Material.MILK_BUCKET, NamedTextColor.AQUA, "Clear Cooldowns", "Clear all active cooldowns"));
        inventory.setItem(13, createControl(Material.ENDER_EYE, NamedTextColor.GREEN, "Force Sync", "Resend HUD state to the client"));
        inventory.setItem(14, createControl(Material.AMETHYST_CLUSTER, NamedTextColor.GOLD, "Reroll", "Reroll this player's bloodline"));
        inventory.setItem(26, createControl(Material.ARROW, NamedTextColor.RED, "Back", "Return to the editor"));
        player.openInventory(inventory);
    }

    private ItemStack createPlayerHead(KnownPlayerRecord knownPlayer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(knownPlayer.uuid());
        meta.setOwningPlayer(offlinePlayer);
        meta.displayName(Component.text(knownPlayer.name(), NamedTextColor.AQUA));

        PlayerProfile profile = plugin.getPlayerDataManager().getOrCreate(knownPlayer.uuid());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Bloodline: " + profile.activeBloodline().displayName(), profile.activeBloodline().color()));
        lore.add(Component.text("Level: " + profile.activeLevel(), NamedTextColor.YELLOW));
        lore.add(Component.text("Click to edit", NamedTextColor.GRAY));
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createSummary(EditSession session) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(session.targetName(), NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Selected Bloodline: " + session.bloodline().displayName(), session.bloodline().color()),
                Component.text("Selected Level: " + session.level(), NamedTextColor.YELLOW),
                Component.text("Save to apply or Cancel to discard", NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBloodlineChoice(BloodlineType type, BloodlineType selected) {
        ItemStack item = new ItemStack(type.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName(), type == selected ? NamedTextColor.GREEN : type.color()));
        meta.lore(List.of(Component.text(type == selected ? "Selected" : "Click to select", type == selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLevelDisplay(int level) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Level " + level, NamedTextColor.YELLOW));
        meta.lore(List.of(Component.text("Range: 1-" + PlayerProfile.MAX_LEVEL, NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createControl(Material material, NamedTextColor color, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        meta.lore(List.of(Component.text(description, NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private String displayName(OfflinePlayer offlinePlayer, UUID targetUuid) {
        return offlinePlayer.getName() == null || offlinePlayer.getName().isBlank() ? targetUuid.toString() : offlinePlayer.getName();
    }

    private static final class EditSession {
        private final UUID targetUuid;
        private final String targetName;
        private BloodlineType bloodline;
        private int level;

        private EditSession(UUID targetUuid, String targetName, BloodlineType bloodline, int level) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.bloodline = bloodline;
            this.level = level;
        }

        public UUID targetUuid() {
            return targetUuid;
        }

        public String targetName() {
            return targetName;
        }

        public BloodlineType bloodline() {
            return bloodline;
        }

        public void setBloodline(BloodlineType bloodline) {
            this.bloodline = bloodline;
        }

        public int level() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }
    }
}
