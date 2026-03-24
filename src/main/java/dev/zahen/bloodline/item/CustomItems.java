package dev.zahen.bloodline.item;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CustomItems {

    public static final String TYPE_TRAIT_POTION = "trait_potion";
    public static final String TYPE_UPGRADE_POTION = "upgrade_potion";
    public static final String TYPE_BLOODLINE_SHARD = "bloodline_shard";
    public static final String TYPE_UNIVERSAL_CORE = "universal_core";
    public static final String TYPE_VOID_FLIGHT_ELYTRA = "void_flight_elytra";

    private final NamespacedKey itemTypeKey;
    private final NamespacedKey bloodlineKey;
    private final NamespacedKey levelKey;

    public CustomItems(BloodlinePlugin plugin) {
        this.itemTypeKey = new NamespacedKey(plugin, "item_type");
        this.bloodlineKey = new NamespacedKey(plugin, "bloodline");
        this.levelKey = new NamespacedKey(plugin, "bloodline_level");
    }

    public ItemStack createTraitPotion(BloodlineType type, int level) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.displayName(Component.text(type.displayName() + " Trait Potion", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Drink to switch to " + type.displayName() + ".", NamedTextColor.GRAY),
                Component.text("Stored level: " + Math.max(1, level), NamedTextColor.YELLOW),
                Component.text("Dropped from PvP kills.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_TRAIT_POTION, type, Math.max(1, level));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createUpgradePotion() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.displayName(Component.text("Upgrade Potion", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Drink to increase your active bloodline by +1.", NamedTextColor.GRAY),
                Component.text("Caps at level 5.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_UPGRADE_POTION, null, 0);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodlineShard(BloodlineType type) {
        ItemStack item = new ItemStack(Material.DISC_FRAGMENT_5);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName() + " Bloodline Shard", type.color()));
        meta.lore(List.of(
                Component.text("Forged from 5 upgrade potions", NamedTextColor.GRAY),
                Component.text("and 1 " + type.displayName() + " trait potion.", NamedTextColor.GRAY),
                Component.text("Used to craft the Universal Bloodline.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_BLOODLINE_SHARD, type, PlayerProfile.MAX_LEVEL);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createUniversalCore() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Universal Bloodline", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("Right click to unlock the Universal Bloodline.", NamedTextColor.GRAY),
                Component.text("Requires every base bloodline at level 5.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_UNIVERSAL_CORE, BloodlineType.UNIVERSAL, 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createVoidFlightElytra() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Voider Flight Elytra", NamedTextColor.DARK_PURPLE));
        meta.lore(List.of(
                Component.text("Temporary bloodline flight gear.", NamedTextColor.GRAY),
                Component.text("Removed when Void Flight ends.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_VOID_FLIGHT_ELYTRA, BloodlineType.VOIDER, 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public String getItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
    }

    public BloodlineType getBloodline(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String key = item.getItemMeta().getPersistentDataContainer().get(bloodlineKey, PersistentDataType.STRING);
        return key == null ? null : BloodlineType.fromKey(key);
    }

    public int getStoredLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    public boolean isVoidFlightElytra(ItemStack item) {
        return TYPE_VOID_FLIGHT_ELYTRA.equals(getItemType(item));
    }

    public boolean isBloodlineShard(ItemStack item, BloodlineType type) {
        return TYPE_BLOODLINE_SHARD.equals(getItemType(item)) && getBloodline(item) == type;
    }

    private void tag(PersistentDataContainer container, String type, BloodlineType bloodline, int level) {
        container.set(itemTypeKey, PersistentDataType.STRING, type);
        if (bloodline != null) {
            container.set(bloodlineKey, PersistentDataType.STRING, bloodline.key());
        }
        if (level > 0) {
            container.set(levelKey, PersistentDataType.INTEGER, level);
        }
    }
}
