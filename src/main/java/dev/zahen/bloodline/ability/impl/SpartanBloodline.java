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

public final class SpartanBloodline extends AbstractBloodline {

    private static final String FIREBALL_KEY = "spartan.fireball";
    private static final String FLAMING_HANDS_KEY = "spartan.flaming_hands";
    private static final String HELL_DOMINION_KEY = "spartan.hell_dominion";
    private static final String FLAME_BARRIER_KEY = "spartan.flame_barrier";
    private static final String INFERNO_RUSH_KEY = "spartan.inferno_rush";

    public SpartanBloodline(BloodlinePlugin plugin) {
        super(plugin, BloodlineType.SPARTAN);
    }

    @Override
    public void applyPassive(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0, true, false, true));
        int passiveStrength = level(player) >= 5 ? plugin.getConfig().getInt("bloodlines.spartan.passive.strength-amplifier-at-level-5", 1) : 0;
        if (passiveStrength > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, passiveStrength, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    @Override
    public void handlePrimaryAbility(Player player) {
        if (!startCooldown(player, FIREBALL_KEY, configSeconds("fireball.cooldown-seconds"), configSeconds("fireball.cooldown-reduction-per-level"), 15L)) {
            cooldown(player, FIREBALL_KEY);
            return;
        }

        manager().prepareSpartanFireball(player);
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 18, 0.15, 0.15, 0.15, 0.01);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1F, 1F);
        activated(player, "Ability Activated");
    }

    @Override
    public void handleSecondaryAbility(Player player) {
        if (!startCooldown(player, FLAMING_HANDS_KEY, configSeconds("flaming-hands.cooldown-seconds"), configSeconds("flaming-hands.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, FLAMING_HANDS_KEY);
            return;
        }

        long durationSeconds = configSeconds("flaming-hands.duration-seconds")
                + Math.max(0, level(player) - 1) * plugin.getConfig().getLong("bloodlines.spartan.flaming-hands.duration-seconds-per-level", 15L);
        long durationMillis = durationSeconds * 1000L;
        profile(player).setSpartanFlamingHandsUntil(System.currentTimeMillis() + durationMillis);
        manager().markDirty(player);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 45, 0.5, 0.8, 0.5, 0.04);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1F, 0.85F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsSpecialAbility() {
        return true;
    }

    @Override
    public void handleSpecialAbility(Player player) {
        if (!startCooldown(player, HELL_DOMINION_KEY, configSeconds("hell-dominion.cooldown-seconds"), configSeconds("hell-dominion.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, HELL_DOMINION_KEY);
            return;
        }

        if (!manager().startSpartanHellDominion(player)) {
            profile(player).clearCooldown(HELL_DOMINION_KEY);
            manager().showPopup(player, "Not enough space to spawn Hell Domain.", NamedTextColor.RED);
            return;
        }
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 90, 0.8, 1.0, 0.8, 0.06);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1F, 0.8F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsFourthAbility() {
        return true;
    }

    @Override
    public void handleFourthAbility(Player player) {
        if (level(player) < 4) {
            manager().showPopup(player, "Flame Barrier unlocks at level 4.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, FLAME_BARRIER_KEY, 180L, 8L, 60L)) {
            cooldown(player, FLAME_BARRIER_KEY);
            return;
        }
        manager().startSpartanFlameBarrier(player);
        activated(player, "Flame Barrier");
    }

    @Override
    public boolean supportsFifthAbility() {
        return true;
    }

    @Override
    public void handleFifthAbility(Player player) {
        if (level(player) < 5) {
            manager().showPopup(player, "Fifth Spartan ability unlocks at level 5.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, INFERNO_RUSH_KEY, 300L, 10L, 90L)) {
            cooldown(player, INFERNO_RUSH_KEY);
            return;
        }
        manager().startSpartanFinalAbility(player);
        activated(player, plugin.getGameplaySettings().isPublicMode() ? "Inferno Rush" : "Crimson Domain");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Permanent Fire Resistance", NamedTextColor.GRAY),
                Component.text("Late-level passive Strength", NamedTextColor.GRAY),
                Component.text("Primary: Fireball", NamedTextColor.GRAY),
                Component.text("Secondary: Flaming Hands", NamedTextColor.GRAY),
                Component.text("Special: Hell Domain", NamedTextColor.GRAY),
                Component.text("Level 4: Flame Barrier", NamedTextColor.GRAY),
                Component.text("Level 5: Inferno Rush / Crimson Domain", NamedTextColor.GRAY)
        );
    }
}
