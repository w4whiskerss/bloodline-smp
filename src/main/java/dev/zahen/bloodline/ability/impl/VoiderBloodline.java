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

public class VoiderBloodline extends AbstractBloodline {

    private static final String VOID_BLINK_KEY = "voider.void_blink";
    private static final String VOID_FLIGHT_KEY = "voider.void_flight";

    public VoiderBloodline(BloodlinePlugin plugin, BloodlineType type) {
        super(plugin, type);
    }

    @Override
    public void applyPassive(Player player) {
        manager().applyVoiderDailyPassive(player);
        if (level(player) >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    @Override
    public void removePassive(Player player) {
        manager().removeVoiderDailyPassive(player);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    @Override
    public void handlePrimaryAbility(Player player) {
        if (!startCooldown(player, VOID_BLINK_KEY, configSeconds("void-blink.cooldown-seconds"), configSeconds("void-blink.cooldown-reduction-per-level"), 12L)) {
            cooldown(player, VOID_BLINK_KEY);
            return;
        }
        if (!manager().startVoidBlink(player, type() == BloodlineType.UNIVERSAL)) {
            profile(player).clearCooldown(VOID_BLINK_KEY);
            player.sendActionBar(Component.text("No safe blink path ahead.", NamedTextColor.RED));
            return;
        }

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 42, 0.45, 0.6, 0.45, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1F, 1.2F);
        activated(player, "Ability Activated");
    }

    @Override
    public void handleSecondaryAbility(Player player) {
        if (!manager().consumeVoidSendCharge(player)) {
            player.sendActionBar(Component.text("Ability on cooldown: " + manager().nextVoidSendRecharge(player), NamedTextColor.RED));
            return;
        }
        if (!manager().tryVoidSend(player, type() == BloodlineType.UNIVERSAL)) {
            manager().refundVoidSendCharge(player);
            player.sendActionBar(Component.text("No valid player target in sight.", NamedTextColor.RED));
            return;
        }

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsSpecialAbility() {
        return true;
    }

    @Override
    public void handleSpecialAbility(Player player) {
        if (!startCooldown(player, VOID_FLIGHT_KEY, configSeconds("void-flight.cooldown-seconds"), configSeconds("void-flight.cooldown-reduction-per-level"), 120L)) {
            cooldown(player, VOID_FLIGHT_KEY);
            return;
        }
        manager().startVoidFlight(player, type() == BloodlineType.UNIVERSAL);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 1F, 1.1F);
        activated(player, "Ability Activated");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Daily random level 1 potion effect", NamedTextColor.GRAY),
                Component.text("Void Blink grants temporary invisibility", NamedTextColor.GRAY),
                Component.text("Higher levels strengthen the daily buff and flight/send values", NamedTextColor.GRAY),
                Component.text("Primary: Void Blink", NamedTextColor.GRAY),
                Component.text("Secondary: Void Send (2 charges)", NamedTextColor.GRAY),
                Component.text("Special: Shift + Double Jump for Void Flight", NamedTextColor.GRAY)
        );
    }
}
