package dev.zahen.bloodline.ability.impl;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class UniversalBloodline extends VoiderBloodline {

    private static final String UNIVERSAL_PRIMARY_KEY = "universal.rift_dash";
    private static final String UNIVERSAL_SECONDARY_KEY = "universal.blood_fusion";
    private static final String UNIVERSAL_SPECIAL_KEY = "universal.ascension";

    public UniversalBloodline(BloodlinePlugin plugin) {
        super(plugin, BloodlineType.UNIVERSAL);
    }

    @Override
    public void applyPassive(Player player) {
        super.applyPassive(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0, true, false, true));
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        }
    }

    @Override
    public void removePassive(Player player) {
        super.removePassive(player);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    @Override
    public void handlePrimaryAbility(Player player) {
        if (!startCooldown(player, UNIVERSAL_PRIMARY_KEY, 45L, 3L, 15L)) {
            cooldown(player, UNIVERSAL_PRIMARY_KEY);
            return;
        }
        manager().startUniversalRiftDash(player);
        activated(player, "Ability Activated");
    }

    @Override
    public void handleSecondaryAbility(Player player) {
        if (!startCooldown(player, UNIVERSAL_SECONDARY_KEY, 240L, 10L, 60L)) {
            cooldown(player, UNIVERSAL_SECONDARY_KEY);
            return;
        }
        manager().startUniversalBloodFusion(player);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsSpecialAbility() {
        return true;
    }

    @Override
    public void handleSpecialAbility(Player player) {
        if (!startCooldown(player, UNIVERSAL_SPECIAL_KEY, 360L, 15L, 90L)) {
            cooldown(player, UNIVERSAL_SPECIAL_KEY);
            return;
        }
        manager().startUniversalAscension(player);
        activated(player, "Ability Activated");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Nerfed combination of Aqua, Spartan, Earthian, and Voider passives", NamedTextColor.GRAY),
                Component.text("Primary: Rift Dash", NamedTextColor.GRAY),
                Component.text("Secondary: Blood Fusion", NamedTextColor.GRAY),
                Component.text("Special: Ascension", NamedTextColor.GRAY)
        );
    }
}
