package dev.zahen.bloodline.model;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum BloodlineType {
    AQUA("Aqua", Material.HEART_OF_THE_SEA, NamedTextColor.AQUA),
    SPARTAN("Spartan", Material.NETHERITE_SWORD, NamedTextColor.GOLD),
    EARTHIAN("Earthian", Material.MOSS_BLOCK, NamedTextColor.GREEN),
    VOIDER("Voider", Material.ENDER_EYE, NamedTextColor.DARK_PURPLE),
    UNIVERSAL("Omni", Material.NETHER_STAR, NamedTextColor.LIGHT_PURPLE);

    private final String displayName;
    private final Material icon;
    private final NamedTextColor color;

    BloodlineType(String displayName, Material icon, NamedTextColor color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public NamedTextColor color() {
        return color;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isBaseBloodline() {
        return this != UNIVERSAL;
    }

    public static BloodlineType fromKey(String key) {
        for (BloodlineType type : values()) {
            if (type.name().equalsIgnoreCase(key) || type.key().equalsIgnoreCase(key) || type.displayName.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    public static BloodlineType randomBase() {
        List<BloodlineType> base = List.of(AQUA, SPARTAN, EARTHIAN, VOIDER);
        return base.get(ThreadLocalRandom.current().nextInt(base.size()));
    }
}
