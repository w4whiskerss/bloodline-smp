package dev.whiskers.bloodline.item;

import dev.whiskers.bloodline.BloodlinePlugin;
import dev.whiskers.bloodline.model.BloodlineType;
import dev.whiskers.bloodline.model.PlayerProfile;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
    public static final String TYPE_OMNI_BLADE = "omni_blade";
    public static final String TYPE_VOID_FLIGHT_ELYTRA = "void_flight_elytra";
    public static final String TYPE_BLOOD_DROP = "blood_drop";
    public static final String TYPE_BLOOD_BLOCK = "blood_block";
    public static final String TYPE_BLOOD_MACE = "blood_mace";

    private final NamespacedKey itemTypeKey;
    private final NamespacedKey bloodlineKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey bloodMaceIndexKey;
    private final NamespacedKey bloodMaceCreatedCountKey;
    private final NamespacedKey bloodMaceModelKey;

    public CustomItems(BloodlinePlugin plugin) {
        this.itemTypeKey = new NamespacedKey(plugin, "item_type");
        this.bloodlineKey = new NamespacedKey(plugin, "bloodline");
        this.levelKey = new NamespacedKey(plugin, "bloodline_level");
        this.bloodMaceIndexKey = new NamespacedKey(plugin, "blood_mace_index");
        this.bloodMaceCreatedCountKey = new NamespacedKey(plugin, "blood_mace_created_count");
        this.bloodMaceModelKey = new NamespacedKey(plugin, "blood_mace_model");
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
                Component.text("Used to craft the Omni bloodline.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_BLOODLINE_SHARD, type, PlayerProfile.MAX_LEVEL);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createUniversalCore() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Omni Star", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("Right click to unlock Omni.", NamedTextColor.GRAY),
                Component.text("Can also be used to craft the OmniBlade.", NamedTextColor.GRAY),
                Component.text("Crafted from the four bloodline shards.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_UNIVERSAL_CORE, BloodlineType.UNIVERSAL, 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createOmniBlade() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("OmniBlade", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("A kill with this blade seals the victim.", NamedTextColor.GRAY),
                Component.text("Victims are kicked and rejoin in spectator mode.", NamedTextColor.DARK_GRAY)
        ));
        meta.setCustomModelData(220829);
        tag(meta.getPersistentDataContainer(), TYPE_OMNI_BLADE, BloodlineType.UNIVERSAL, 1);
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

    public ItemStack createBloodDrop() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood", NamedTextColor.DARK_RED));
        meta.lore(List.of(
                Component.text("Rare bloodline residue.", NamedTextColor.GRAY),
                Component.text("Use 9 to craft a Blood Block.", NamedTextColor.DARK_GRAY),
                Component.text("Blood stops dropping once all 10 Blood Maces exist.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_BLOOD_DROP, null, 0);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodBlock() {
        ItemStack item = new ItemStack(Material.NETHER_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood Block", NamedTextColor.DARK_RED));
        meta.lore(List.of(
                Component.text("Condensed blood for Blood Mace forging.", NamedTextColor.GRAY),
                Component.text("Crafted from 9 Blood.", NamedTextColor.DARK_GRAY)
        ));
        tag(meta.getPersistentDataContainer(), TYPE_BLOOD_BLOCK, null, 0);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBloodMace(int maceIndex, int createdCount, int customModelData) {
        ItemStack item = new ItemStack(bloodMaceMaterial());
        ItemMeta meta = item.getItemMeta();
        applyBloodMaceDefaults(meta, maceIndex, createdCount);
        applyBloodMaceTags(meta, maceIndex, createdCount, customModelData);
        item.setItemMeta(meta);
        return item;
    }

    public void updateBloodMace(ItemStack item, int maceIndex, int createdCount, int customModelData) {
        if (!isBloodMace(item) || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        applyBloodMaceTags(meta, maceIndex, createdCount, customModelData);
        item.setItemMeta(meta);
    }

    public Component defaultBloodMaceName() {
        return bloodMaceName();
    }

    public List<Component> defaultBloodMaceLore(int maceIndex, int createdCount) {
        return List.of(
                Component.text("Stasis Strike: Freeze a target for 8s and double all damage dealt to them.", NamedTextColor.GRAY),
                Component.text("Bloodlust: Sneak-right-click while low to drain the nearest living target.", NamedTextColor.GRAY),
                Component.text("Forged: " + createdCount + "/10", NamedTextColor.RED),
                Component.text("Mace: " + maceIndex + "/10", NamedTextColor.DARK_RED)
        );
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

    public boolean isOmniBlade(ItemStack item) {
        return TYPE_OMNI_BLADE.equals(getItemType(item));
    }

    public boolean isBloodDrop(ItemStack item) {
        return TYPE_BLOOD_DROP.equals(getItemType(item));
    }

    public boolean isBloodBlock(ItemStack item) {
        return TYPE_BLOOD_BLOCK.equals(getItemType(item));
    }

    public boolean isBloodMace(ItemStack item) {
        return TYPE_BLOOD_MACE.equals(getItemType(item));
    }

    public int getBloodMaceIndex(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(bloodMaceIndexKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public int getBloodMaceCreatedCount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(bloodMaceCreatedCountKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public int getBloodMaceModel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(bloodMaceModelKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
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

    private void applyBloodMaceDefaults(ItemMeta meta, int maceIndex, int createdCount) {
        meta.displayName(defaultBloodMaceName());
        meta.lore(defaultBloodMaceLore(maceIndex, createdCount));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private void applyBloodMaceTags(ItemMeta meta, int maceIndex, int createdCount, int customModelData) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        tag(container, TYPE_BLOOD_MACE, null, 0);
        container.set(bloodMaceIndexKey, PersistentDataType.INTEGER, maceIndex);
        container.set(bloodMaceCreatedCountKey, PersistentDataType.INTEGER, createdCount);
        container.set(bloodMaceModelKey, PersistentDataType.INTEGER, customModelData);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        } else {
            meta.setCustomModelData(null);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    public Material bloodMaceMaterial() {
        Material mace = Material.matchMaterial("MACE");
        return mace == null ? Material.NETHERITE_AXE : mace;
    }

    private Component bloodMaceName() {
        TextComponent.Builder builder = Component.text();
        String text = "BLOOD MACE";
        NamedTextColor[] gradient = new NamedTextColor[]{
                NamedTextColor.RED,
                NamedTextColor.RED,
                NamedTextColor.RED,
                NamedTextColor.DARK_RED,
                NamedTextColor.DARK_RED,
                NamedTextColor.DARK_RED,
                NamedTextColor.DARK_RED,
                NamedTextColor.DARK_RED,
                NamedTextColor.RED,
                NamedTextColor.RED
        };
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            NamedTextColor color = gradient[Math.min(index, gradient.length - 1)];
            builder.append(Component.text(character, color));
        }
        return builder.build();
    }
}
