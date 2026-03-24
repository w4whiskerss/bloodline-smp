package dev.zahen.bloodline.gui;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import dev.zahen.bloodline.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class BloodlineGui {

    public static final String TITLE = "Bloodline SMP";

    private final BloodlinePlugin plugin;

    public BloodlineGui(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerProfile profile = plugin.getBloodlineManager().profile(player);
        Inventory inventory = Bukkit.createInventory(player, 27, Component.text(TITLE, NamedTextColor.DARK_AQUA));

        inventory.setItem(4, createStatusItem(profile, player));
        inventory.setItem(10, createBloodlineItem(profile, BloodlineType.AQUA));
        inventory.setItem(12, createBloodlineItem(profile, BloodlineType.SPARTAN));
        inventory.setItem(14, createBloodlineItem(profile, BloodlineType.EARTHIAN));
        inventory.setItem(16, createBloodlineItem(profile, BloodlineType.VOIDER));
        inventory.setItem(22, createBloodlineItem(profile, BloodlineType.UNIVERSAL));

        player.openInventory(inventory);
    }

    private ItemStack createStatusItem(PlayerProfile profile, Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Current Bloodline", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Active: " + profile.activeBloodline().displayName(), NamedTextColor.YELLOW));
        lore.add(Component.text("Level: " + profile.activeLevel(), NamedTextColor.YELLOW));
        lore.add(Component.empty());
        lore.add(Component.text("Cooldowns", NamedTextColor.AQUA));
        lore.add(Component.text("Primary: " + plugin.getBloodlineManager().cooldownLine(player, "primary"), NamedTextColor.GRAY));
        lore.add(Component.text("Secondary: " + plugin.getBloodlineManager().cooldownLine(player, "secondary"), NamedTextColor.GRAY));
        lore.add(Component.text("Special: " + plugin.getBloodlineManager().cooldownLine(player, "special"), NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBloodlineItem(PlayerProfile profile, BloodlineType type) {
        ItemStack item = new ItemStack(type.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName(), type == profile.activeBloodline() ? NamedTextColor.GREEN : NamedTextColor.AQUA));

        List<Component> lore = new ArrayList<>();
        int level = profile.level(type);
        lore.add(Component.text("Owned: " + (level > 0 ? "Yes" : "No"), level > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Component.text("Level: " + level + "/" + PlayerProfile.MAX_LEVEL, NamedTextColor.YELLOW));

        if (type == BloodlineType.UNIVERSAL && !profile.owns(BloodlineType.UNIVERSAL)) {
            lore.add(Component.text("Craft the Universal Bloodline to unlock it.", NamedTextColor.LIGHT_PURPLE));
        } else if (level > 0) {
            lore.add(Component.text("Click to switch active bloodline.", NamedTextColor.GRAY));
        }

        if (type == BloodlineType.UNIVERSAL) {
            lore.add(Component.text("Requirement: all four base bloodlines at level 5.", NamedTextColor.DARK_GRAY));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
