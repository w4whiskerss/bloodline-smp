package dev.zahen.bloodline.data;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerDataManager {

    private final BloodlinePlugin plugin;
    private final File dataDirectory;
    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public PlayerDataManager(BloodlinePlugin plugin) {
        this.plugin = plugin;
        this.dataDirectory = new File(plugin.getDataFolder(), "players");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
    }

    public PlayerProfile getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadProfile);
    }

    public void saveAsync(PlayerProfile profile) {
        YamlConfiguration yaml = serialize(profile);
        File file = fileFor(profile.uuid());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                yaml.save(file);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save profile " + profile.uuid(), exception);
            }
        });
    }

    public void saveAllSync() {
        cache.values().forEach(this::saveSync);
    }

    public void saveSync(PlayerProfile profile) {
        YamlConfiguration yaml = serialize(profile);
        try {
            yaml.save(fileFor(profile.uuid()));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save profile " + profile.uuid(), exception);
        }
    }

    public void unload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile != null) {
            saveSync(profile);
        }
    }

    public List<KnownPlayerRecord> knownPlayers() {
        List<KnownPlayerRecord> players = new ArrayList<>();
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return players;
        }

        for (File file : files) {
            String raw = file.getName().substring(0, file.getName().length() - 4);
            try {
                UUID uuid = UUID.fromString(raw);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName();
                players.add(new KnownPlayerRecord(uuid, name == null || name.isBlank() ? uuid.toString() : name));
            } catch (IllegalArgumentException ignored) {
            }
        }

        players.sort(Comparator.comparing(KnownPlayerRecord::name, String.CASE_INSENSITIVE_ORDER));
        return players;
    }

    private PlayerProfile loadProfile(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) {
            PlayerProfile created = new PlayerProfile(uuid, rollInitialBloodline());
            created.setLevel(created.activeBloodline(), 1);
            created.setVoidSendCharges(2);
            created.setVoidSendLastRechargeAt(System.currentTimeMillis());
            created.setFreshAssignmentPending(true);
            return created;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        BloodlineType active = BloodlineType.fromKey(yaml.getString("active-bloodline", BloodlineType.AQUA.key()));
        if (active == null) {
            active = BloodlineType.AQUA;
        }

        PlayerProfile profile = new PlayerProfile(uuid, active);
        ConfigurationSection levelsSection = yaml.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                BloodlineType type = BloodlineType.fromKey(key);
                if (type != null) {
                    profile.setLevel(type, levelsSection.getInt(key, 0));
                }
            }
        }
        if (!profile.owns(profile.activeBloodline())) {
            profile.setLevel(profile.activeBloodline(), 1);
        }

        ConfigurationSection cooldownSection = yaml.getConfigurationSection("cooldowns");
        if (cooldownSection != null) {
            for (String key : cooldownSection.getKeys(false)) {
                profile.setCooldown(key, cooldownSection.getLong(key));
            }
        }

        profile.setSpartanFlamingHandsUntil(yaml.getLong("spartan.flaming-hands-until", 0L));
        String cursedBy = yaml.getString("spartan.cursed-by");
        profile.setCursedBySpartan(cursedBy == null || cursedBy.isBlank() ? null : UUID.fromString(cursedBy));
        profile.setCursedUntil(yaml.getLong("spartan.cursed-until", 0L));
        profile.setVoidSendCharges(yaml.getInt("voider.void-send-charges", 2));
        profile.setVoidSendLastRechargeAt(yaml.getLong("voider.void-send-last-recharge-at", System.currentTimeMillis()));
        profile.setVoidDailyEffect(yaml.getString("voider.daily-effect"));
        profile.setVoidDailyEffectAssignedAt(yaml.getLong("voider.daily-effect-assigned-at", 0L));
        return profile;
    }

    private YamlConfiguration serialize(PlayerProfile profile) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("uuid", profile.uuid().toString());
        yaml.set("active-bloodline", profile.activeBloodline().key());

        for (Map.Entry<BloodlineType, Integer> entry : profile.levels().entrySet()) {
            yaml.set("levels." + entry.getKey().key(), entry.getValue());
        }
        for (Map.Entry<String, Long> entry : profile.cooldowns().entrySet()) {
            yaml.set("cooldowns." + entry.getKey(), entry.getValue());
        }

        yaml.set("spartan.flaming-hands-until", profile.spartanFlamingHandsUntil());
        yaml.set("spartan.cursed-by", profile.cursedBySpartan() == null ? null : profile.cursedBySpartan().toString());
        yaml.set("spartan.cursed-until", profile.cursedUntil());
        yaml.set("voider.void-send-charges", profile.voidSendCharges());
        yaml.set("voider.void-send-last-recharge-at", profile.voidSendLastRechargeAt());
        yaml.set("voider.daily-effect", profile.voidDailyEffect());
        yaml.set("voider.daily-effect-assigned-at", profile.voidDailyEffectAssignedAt());
        return yaml;
    }

    private File fileFor(UUID uuid) {
        return new File(dataDirectory, uuid + ".yml");
    }

    public BloodlineType rollInitialBloodline() {
        double roll = Math.max(0.0D, Math.min(100.0D, Math.random() * 100.0D));
        double aqua = plugin.getConfig().getDouble("first-join-selection.chances.aqua", 35.0D);
        double earthian = plugin.getConfig().getDouble("first-join-selection.chances.earthian", 30.0D);
        double spartan = plugin.getConfig().getDouble("first-join-selection.chances.spartan", 20.0D);
        if (roll < aqua) {
            return BloodlineType.AQUA;
        }
        if (roll < aqua + earthian) {
            return BloodlineType.EARTHIAN;
        }
        if (roll < aqua + earthian + spartan) {
            return BloodlineType.SPARTAN;
        }
        return BloodlineType.VOIDER;
    }

    public record KnownPlayerRecord(UUID uuid, String name) {
    }
}
