package dev.zahen.bloodline.gui;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TestItemsGui {

    public static final String TITLE = "Bloodline Test Items";

    private final BloodlinePlugin plugin;

    public TestItemsGui(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, Component.text(TITLE, NamedTextColor.DARK_RED));

        placeBloodlineRow(inventory, 0, BloodlineType.AQUA);
        placeBloodlineRow(inventory, 9, BloodlineType.SPARTAN);
        placeBloodlineRow(inventory, 18, BloodlineType.EARTHIAN);
        placeBloodlineRow(inventory, 27, BloodlineType.VOIDER);

        inventory.setItem(45, createLabeledItem(Material.EXPERIENCE_BOTTLE, NamedTextColor.GOLD, "Upgrade Potion x1",
                List.of("Click to get 1 Upgrade Potion")));
        inventory.setItem(46, createLabeledItem(Material.EXPERIENCE_BOTTLE, NamedTextColor.GOLD, "Upgrade Potion x5",
                List.of("Click to get 5 Upgrade Potions")));
        inventory.setItem(48, createLabeledItem(Material.NETHER_STAR, NamedTextColor.LIGHT_PURPLE, "Omni Star",
                List.of("Click to get the Omni Star")));
        inventory.setItem(49, createLabeledItem(Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, "OmniBlade",
                List.of("Click to get the OmniBlade")));
        inventory.setItem(53, createLabeledItem(Material.BARRIER, NamedTextColor.RED, "Close",
                List.of("Close this menu")));

        player.openInventory(inventory);
    }

    public boolean isTitle(String title) {
        return TITLE.equals(title);
    }

    public void handleClick(Player player, int slot) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            give(player, plugin.getCustomItems().createUpgradePotion());
            return;
        }
        if (slot == 46) {
            ItemStack stack = plugin.getCustomItems().createUpgradePotion();
            stack.setAmount(5);
            give(player, stack);
            return;
        }
        if (slot == 48) {
            give(player, plugin.getCustomItems().createUniversalCore());
            return;
        }
        if (slot == 49) {
            give(player, plugin.getCustomItems().createOmniBlade());
            return;
        }

        BloodlineType type = switch (slot / 9) {
            case 0 -> BloodlineType.AQUA;
            case 1 -> BloodlineType.SPARTAN;
            case 2 -> BloodlineType.EARTHIAN;
            case 3 -> BloodlineType.VOIDER;
            default -> null;
        };
        if (type == null) {
            return;
        }

        int column = slot % 9;
        if (column >= 1 && column <= 5) {
            give(player, plugin.getCustomItems().createTraitPotion(type, column));
            return;
        }
        if (column == 7) {
            give(player, plugin.getCustomItems().createBloodlineShard(type));
        }
    }

    private void placeBloodlineRow(Inventory inventory, int startSlot, BloodlineType type) {
        inventory.setItem(startSlot, createLabeledItem(type.icon(), type.color(), type.displayName(),
                List.of("Trait Potions levels 1-5", "Shard on the right")));
        for (int level = 1; level <= 5; level++) {
            inventory.setItem(startSlot + level, plugin.getCustomItems().createTraitPotion(type, level));
        }
        inventory.setItem(startSlot + 7, plugin.getCustomItems().createBloodlineShard(type));
    }

    private ItemStack createLabeledItem(Material material, NamedTextColor color, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        meta.lore(loreLines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player, ItemStack item) {
        player.getInventory().addItem(item.clone()).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                : item.getType().name();
        player.sendActionBar(Component.text("Given: " + itemName, NamedTextColor.GREEN));
    }
}
