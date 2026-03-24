package dev.zahen.bloodline.ability.impl;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.ability.AbstractBloodline;
import dev.zahen.bloodline.model.BloodlineType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class AquaBloodline extends AbstractBloodline {

    private static final String WATER_DASH_KEY = "aqua.water_dash";
    private static final String SUFFOCATION_CURSE_KEY = "aqua.suffocation_curse";
    private static final String TIDAL_SURGE_KEY = "aqua.tidal_surge";

    public AquaBloodline(BloodlinePlugin plugin) {
        super(plugin, BloodlineType.AQUA);
    }

    @Override
    public void applyPassive(Player player) {
        if (player.isInWater()) {
            int strengthAmplifier = Math.max(0, level(player) - 1);
            int dolphinsGraceAmplifier = level(player) >= 5
                    ? plugin.getConfig().getInt("bloodlines.aqua.dolphins-grace-amplifier-at-level-5", 1)
                    : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, strengthAmplifier, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, dolphinsGraceAmplifier, true, false, true));
            player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0.01);
        } else {
            removePassive(player);
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    @Override
    public void handlePrimaryAbility(Player player) {
        if (!startCooldown(player, WATER_DASH_KEY, configSeconds("water-dash.cooldown-seconds"), configSeconds("water-dash.cooldown-reduction-per-level"), 15L)) {
            cooldown(player, WATER_DASH_KEY);
            return;
        }

        double baseStrength = player.isInWater()
                ? plugin.getConfig().getDouble("bloodlines.aqua.water-dash.water-strength", 2.1D)
                : plugin.getConfig().getDouble("bloodlines.aqua.water-dash.land-strength", 1.25D);
        double strength = baseStrength + Math.max(0, level(player) - 1) * plugin.getConfig().getDouble("bloodlines.aqua.water-dash.strength-per-level", 0.12D);
        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(strength);
        direction.setY(player.isInWater() ? direction.getY() * 0.35D : Math.max(0.15D, direction.getY() * 0.18D));

        player.setVelocity(direction);
        manager().runAquaWaterDash(player);
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 40, 0.45, 0.35, 0.45, 0.2);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1F, 1.15F);
        activated(player, "Ability Activated");
    }

    @Override
    public void handleSecondaryAbility(Player player) {
        if (!startCooldown(player, SUFFOCATION_CURSE_KEY, configSeconds("suffocation-curse.cooldown-seconds"), configSeconds("suffocation-curse.cooldown-reduction-per-level"), 40L)) {
            cooldown(player, SUFFOCATION_CURSE_KEY);
            return;
        }
        if (!manager().applyAquaSuffocationCurse(player)) {
            profile(player).clearCooldown(SUFFOCATION_CURSE_KEY);
            player.sendActionBar(Component.text("No player target in sight.", NamedTextColor.RED));
            return;
        }

        player.getWorld().spawnParticle(Particle.BUBBLE_POP, player.getEyeLocation(), 24, 0.2, 0.2, 0.2, 0.04);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.85F, 1.25F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsSpecialAbility() {
        return true;
    }

    @Override
    public void handleSpecialAbility(Player player) {
        if (!startCooldown(player, TIDAL_SURGE_KEY, configSeconds("tidal-surge.cooldown-seconds"), configSeconds("tidal-surge.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, TIDAL_SURGE_KEY);
            return;
        }

        manager().startAquaTidalSurge(player);
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 90, 1.1, 0.6, 1.1, 0.25);
        player.getWorld().spawnParticle(Particle.FALLING_WATER, player.getLocation().add(0, 1, 0), 70, 1.0, 0.5, 1.0, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 1F, 0.9F);
        activated(player, "Ability Activated");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Passive: Water Breathing, Strength, and Dolphin's Grace while submerged", NamedTextColor.GRAY),
                Component.text("Primary: Water Dash", NamedTextColor.GRAY),
                Component.text("Secondary: Suffocation Curse", NamedTextColor.GRAY),
                Component.text("Special: Shift + Double Jump for Tidal Surge", NamedTextColor.GRAY)
        );
    }
}
