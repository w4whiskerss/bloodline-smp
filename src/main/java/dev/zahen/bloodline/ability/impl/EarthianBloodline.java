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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class EarthianBloodline extends AbstractBloodline {

    private static final String GROUND_SLAM_KEY = "earthian.ground_slam";
    private static final String ROOT_TRAP_KEY = "earthian.root_prison";
    private static final String WORLDBREAKER_KEY = "earthian.worldbreaker";
    private static final String OBSIDIAN_CAGE_KEY = "earthian.obsidian_cage";
    private static final String CONSUME_KEY = "earthian.consume";

    public EarthianBloodline(BloodlinePlugin plugin) {
        super(plugin, BloodlineType.EARTHIAN);
    }

    @Override
    public void applyPassive(Player player) {
        int resistanceAmplifier = level(player) >= 5
                ? plugin.getConfig().getInt("bloodlines.earthian.passive.resistance-amplifier-at-level-5", 1)
                : 0;
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, resistanceAmplifier, true, false, true));
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    @Override
    public void handlePrimaryAbility(Player player) {
        if (!startCooldown(player, GROUND_SLAM_KEY, configSeconds("ground-slam.cooldown-seconds"), configSeconds("ground-slam.cooldown-reduction-per-level"), 20L)) {
            cooldown(player, GROUND_SLAM_KEY);
            return;
        }

        manager().armGroundSlam(player);
        double launchPower = plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.launch-power", 1.9D)
                + Math.max(0, level(player) - 1) * plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.launch-power-per-level", 0.12D);
        player.setVelocity(new Vector(player.getVelocity().getX(), launchPower, player.getVelocity().getZ()));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0.4, 0.2, 0.4, 0.01);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.8F, 0.8F);
        activated(player, "Ability Activated");
    }

    @Override
    public void handleSecondaryAbility(Player player) {
        if (!startCooldown(player, ROOT_TRAP_KEY, configSeconds("root-trap.cooldown-seconds"), configSeconds("root-trap.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, ROOT_TRAP_KEY);
            return;
        }

        double radius = plugin.getConfig().getDouble("bloodlines.earthian.root-trap.radius", 5.0D)
                + Math.max(0, level(player) - 1) * plugin.getConfig().getDouble("bloodlines.earthian.root-trap.radius-per-level", 0.35D);
        int durationTicks = plugin.getConfig().getInt("bloodlines.earthian.root-trap.duration-ticks", 100)
                + Math.max(0, level(player) - 1) * plugin.getConfig().getInt("bloodlines.earthian.root-trap.duration-ticks-per-level", 20);
        int slownessAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.earthian.root-trap.slowness-amplifier-at-level-5", 3),
                2 + Math.max(0, level(player) - 1) / 2
        );
        int miningFatigueAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.earthian.root-trap.mining-fatigue-amplifier-at-level-5", 2),
                1 + Math.max(0, level(player) - 1) / 3
        );
        Vector center = player.getLocation().toVector();
        for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(radius, entity -> entity != player)) {
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, slownessAmplifier, true, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, miningFatigueAmplifier, true, true, true));
            Vector pull = center.clone().subtract(nearby.getLocation().toVector()).normalize().multiply(0.45D);
            pull.setY(Math.max(-0.08D, nearby.getVelocity().getY() * 0.25D));
            nearby.setVelocity(pull);
        }

        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 0.1, 0), 40, radius / 2, 0.2, radius / 2,
                player.getLocation().getBlock().getBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1F, 0.9F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsSpecialAbility() {
        return true;
    }

    @Override
    public void handleSpecialAbility(Player player) {
        if (!startCooldown(player, WORLDBREAKER_KEY, configSeconds("worldbreaker.cooldown-seconds"), configSeconds("worldbreaker.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, WORLDBREAKER_KEY);
            return;
        }

        manager().startEarthianWorldbreaker(player);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 0.1, 0), 85, 1.2, 0.35, 1.2,
                player.getLocation().getBlock().getBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.9F, 0.65F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsFourthAbility() {
        return true;
    }

    @Override
    public void handleFourthAbility(Player player) {
        if (level(player) < 4) {
            manager().showPopup(player, "Obsidian Cage unlocks at level 4.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, OBSIDIAN_CAGE_KEY, 240L, 10L, 75L)) {
            cooldown(player, OBSIDIAN_CAGE_KEY);
            return;
        }
        if (!manager().startObsidianCage(player)) {
            profile(player).clearCooldown(OBSIDIAN_CAGE_KEY);
            manager().showPopup(player, "No target for Obsidian Cage.", NamedTextColor.RED);
            return;
        }
        activated(player, "Obsidian Cage");
    }

    @Override
    public boolean supportsFifthAbility() {
        return true;
    }

    @Override
    public void handleFifthAbility(Player player) {
        if (level(player) < 5) {
            manager().showPopup(player, "Consume unlocks at level 5.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, CONSUME_KEY, 300L, 12L, 90L)) {
            cooldown(player, CONSUME_KEY);
            return;
        }
        if (!manager().startConsume(player)) {
            profile(player).clearCooldown(CONSUME_KEY);
            manager().showPopup(player, "No target for Consume.", NamedTextColor.RED);
            return;
        }
        activated(player, "Consume");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Resistance I always active", NamedTextColor.GRAY),
                Component.text("Reduced knockback and reduced fall damage", NamedTextColor.GRAY),
                Component.text("Regeneration after 20 seconds still", NamedTextColor.GRAY),
                Component.text("Special: Worldbreaker", NamedTextColor.GRAY),
                Component.text("Level 4: Obsidian Cage", NamedTextColor.GRAY),
                Component.text("Level 5: Consume", NamedTextColor.GRAY)
        );
    }
}
