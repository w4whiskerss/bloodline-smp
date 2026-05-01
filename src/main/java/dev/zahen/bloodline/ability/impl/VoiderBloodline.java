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
    private static final String DARKENED_KEY = "voider.darkened";
    private static final String VOID_CONTROL_KEY = "voider.void_control";
    private static final String ENDERMAN_GUARD_KEY = "voider.enderman_guard";
    private static final String VOID_COLLAPSE_KEY = "voider.void_collapse";

    public VoiderBloodline(BloodlinePlugin plugin, BloodlineType type) {
        super(plugin, type);
    }

    @Override
    public void applyPassive(Player player) {
        manager().applyVoiderDailyPassive(player);
        if (manager().shouldGrantVoiderInvisibility(player, type())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (level(player) >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    @Override
    public void removePassive(Player player) {
        manager().removeVoiderDailyPassive(player);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
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
        if (!startCooldown(player, VOID_CONTROL_KEY, configSeconds("void-control.cooldown-seconds"), configSeconds("void-control.cooldown-reduction-per-level"), 45L)) {
            cooldown(player, VOID_CONTROL_KEY);
            return;
        }

        if (!manager().startVoidControl(player, type() == BloodlineType.UNIVERSAL)) {
            profile(player).clearCooldown(VOID_CONTROL_KEY);
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
        if (!startCooldown(player, DARKENED_KEY, configSeconds("darkened.cooldown-seconds"), configSeconds("darkened.cooldown-reduction-per-level"), 60L)) {
            cooldown(player, DARKENED_KEY);
            return;
        }

        manager().startDarkened(player, type() == BloodlineType.UNIVERSAL);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSE, SoundCategory.PLAYERS, 1F, 0.7F);
        activated(player, "Ability Activated");
    }

    @Override
    public boolean supportsFourthAbility() {
        return true;
    }

    @Override
    public void handleFourthAbility(Player player) {
        if (level(player) < 4) {
            manager().showPopup(player, "Enderman Guard unlocks at level 4.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, ENDERMAN_GUARD_KEY, 210L, 8L, 75L)) {
            cooldown(player, ENDERMAN_GUARD_KEY);
            return;
        }
        manager().startEndermanGuard(player);
        activated(player, "Enderman Guard");
    }

    @Override
    public boolean supportsFifthAbility() {
        return true;
    }

    @Override
    public void handleFifthAbility(Player player) {
        if (level(player) < 5) {
            manager().showPopup(player, "Fifth Voider ability unlocks at level 5.", NamedTextColor.RED);
            return;
        }
        if (!startCooldown(player, VOID_COLLAPSE_KEY, 300L, 12L, 90L)) {
            cooldown(player, VOID_COLLAPSE_KEY);
            return;
        }
        manager().startVoiderFinalAbility(player);
        activated(player, plugin.getGameplaySettings().isPublicMode() ? "Void Collapse" : "Void Domain");
    }

    @Override
    public List<Component> describePassives(Player player) {
        return List.of(
                Component.text("Daily random level 1 potion effect", NamedTextColor.GRAY),
                Component.text("Invisibility only applies in the End", NamedTextColor.GRAY),
                Component.text("Higher levels strengthen the daily buff and blink values", NamedTextColor.GRAY),
                Component.text("Primary: Void Blink", NamedTextColor.GRAY),
                Component.text("Secondary: Void Control", NamedTextColor.GRAY),
                Component.text("Special: Darkened", NamedTextColor.GRAY),
                Component.text("Level 4: Enderman Guard", NamedTextColor.GRAY),
                Component.text("Level 5: Void Collapse / Void Domain", NamedTextColor.GRAY)
        );
    }
}
