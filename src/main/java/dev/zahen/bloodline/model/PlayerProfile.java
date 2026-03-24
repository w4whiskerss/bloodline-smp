package dev.zahen.bloodline.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {

    public static final int MAX_LEVEL = 5;

    private final UUID uuid;
    private final EnumMap<BloodlineType, Integer> levels = new EnumMap<>(BloodlineType.class);
    private final Map<String, Long> cooldowns = new HashMap<>();
    private BloodlineType activeBloodline;
    private long spartanFlamingHandsUntil;
    private UUID cursedBySpartan;
    private long cursedUntil;
    private int voidSendCharges = 2;
    private long voidSendLastRechargeAt;
    private String voidDailyEffect;
    private long voidDailyEffectAssignedAt;
    private boolean freshAssignmentPending;

    public PlayerProfile(UUID uuid, BloodlineType activeBloodline) {
        this.uuid = uuid;
        this.activeBloodline = activeBloodline;
    }

    public UUID uuid() {
        return uuid;
    }

    public BloodlineType activeBloodline() {
        return activeBloodline;
    }

    public void setActiveBloodline(BloodlineType activeBloodline) {
        this.activeBloodline = activeBloodline;
    }

    public int level(BloodlineType type) {
        return levels.getOrDefault(type, 0);
    }

    public Map<BloodlineType, Integer> levels() {
        return levels;
    }

    public void setLevel(BloodlineType type, int level) {
        if (level <= 0) {
            levels.remove(type);
            return;
        }
        levels.put(type, Math.min(MAX_LEVEL, level));
    }

    public void setSingleBloodline(BloodlineType type, int level) {
        levels.clear();
        setLevel(type, level);
        this.activeBloodline = type;
    }

    public boolean owns(BloodlineType type) {
        return level(type) > 0;
    }

    public int activeLevel() {
        return level(activeBloodline);
    }

    public Map<String, Long> cooldowns() {
        return cooldowns;
    }

    public long getCooldown(String key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public void setCooldown(String key, long value) {
        cooldowns.put(key, value);
    }

    public void clearCooldown(String key) {
        cooldowns.remove(key);
    }

    public long spartanFlamingHandsUntil() {
        return spartanFlamingHandsUntil;
    }

    public void setSpartanFlamingHandsUntil(long spartanFlamingHandsUntil) {
        this.spartanFlamingHandsUntil = spartanFlamingHandsUntil;
    }

    public UUID cursedBySpartan() {
        return cursedBySpartan;
    }

    public void setCursedBySpartan(UUID cursedBySpartan) {
        this.cursedBySpartan = cursedBySpartan;
    }

    public long cursedUntil() {
        return cursedUntil;
    }

    public void setCursedUntil(long cursedUntil) {
        this.cursedUntil = cursedUntil;
    }

    public int voidSendCharges() {
        return voidSendCharges;
    }

    public void setVoidSendCharges(int voidSendCharges) {
        this.voidSendCharges = Math.max(0, Math.min(2, voidSendCharges));
    }

    public long voidSendLastRechargeAt() {
        return voidSendLastRechargeAt;
    }

    public void setVoidSendLastRechargeAt(long voidSendLastRechargeAt) {
        this.voidSendLastRechargeAt = voidSendLastRechargeAt;
    }

    public String voidDailyEffect() {
        return voidDailyEffect;
    }

    public void setVoidDailyEffect(String voidDailyEffect) {
        this.voidDailyEffect = voidDailyEffect;
    }

    public long voidDailyEffectAssignedAt() {
        return voidDailyEffectAssignedAt;
    }

    public void setVoidDailyEffectAssignedAt(long voidDailyEffectAssignedAt) {
        this.voidDailyEffectAssignedAt = voidDailyEffectAssignedAt;
    }

    public boolean freshAssignmentPending() {
        return freshAssignmentPending;
    }

    public void setFreshAssignmentPending(boolean freshAssignmentPending) {
        this.freshAssignmentPending = freshAssignmentPending;
    }

    public boolean hasAllBaseBloodlinesAtMax() {
        return level(BloodlineType.AQUA) >= MAX_LEVEL
                && level(BloodlineType.SPARTAN) >= MAX_LEVEL
                && level(BloodlineType.EARTHIAN) >= MAX_LEVEL
                && level(BloodlineType.VOIDER) >= MAX_LEVEL;
    }

    public void resetActiveBloodlineLevel() {
        setSingleBloodline(activeBloodline, 1);
        cooldowns.clear();
    }
}
