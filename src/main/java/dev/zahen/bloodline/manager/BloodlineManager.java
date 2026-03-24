package dev.zahen.bloodline.manager;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.ability.Bloodline;
import dev.zahen.bloodline.ability.impl.AquaBloodline;
import dev.zahen.bloodline.ability.impl.EarthianBloodline;
import dev.zahen.bloodline.ability.impl.SpartanBloodline;
import dev.zahen.bloodline.ability.impl.UniversalBloodline;
import dev.zahen.bloodline.ability.impl.VoiderBloodline;
import dev.zahen.bloodline.model.BloodlineType;
import dev.zahen.bloodline.model.PlayerProfile;
import dev.zahen.bloodline.util.TimeUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.kyori.adventure.title.Title;

public final class BloodlineManager {

    private final BloodlinePlugin plugin;
    private final Map<BloodlineType, Bloodline> bloodlines = new EnumMap<>(BloodlineType.class);
    private final Map<UUID, Long> inputDebounce = new ConcurrentHashMap<>();
    private final Map<UUID, GroundSlamState> groundSlams = new ConcurrentHashMap<>();
    private final Map<UUID, AquaCurseState> aquaCurses = new ConcurrentHashMap<>();
    private final Map<UUID, Long> aquaDashCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, TidalSurgeState> tidalSurges = new ConcurrentHashMap<>();
    private final Map<UUID, HeldFireballState> heldFireballs = new ConcurrentHashMap<>();
    private final Map<UUID, HellDominionState> spartanHellDominions = new ConcurrentHashMap<>();
    private final Map<UUID, EarthianWorldbreakerState> earthianWorldbreakers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> voidBlinkHits = new ConcurrentHashMap<>();
    private final Map<UUID, Long> universalBloodFusionUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> universalAscensionUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> universalRiftDashUntil = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> storedChestplates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> voidFlightEndsAt = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> voidFlightBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> stillSince = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyProfiles = ConcurrentHashMap.newKeySet();
    private final Set<UUID> clientHotkeyPlayers = ConcurrentHashMap.newKeySet();
    private final List<PotionEffectType> voidEffects = List.of(
            PotionEffectType.SPEED,
            PotionEffectType.STRENGTH,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.JUMP_BOOST,
            PotionEffectType.WATER_BREATHING,
            PotionEffectType.FIRE_RESISTANCE
    );
    private final NamespacedKey universalRecipeKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BloodlineManager(BloodlinePlugin plugin) {
        this.plugin = plugin;
        this.universalRecipeKey = new NamespacedKey(plugin, "universal_bloodline");
        bloodlines.put(BloodlineType.AQUA, new AquaBloodline(plugin));
        bloodlines.put(BloodlineType.SPARTAN, new SpartanBloodline(plugin));
        bloodlines.put(BloodlineType.EARTHIAN, new EarthianBloodline(plugin));
        bloodlines.put(BloodlineType.VOIDER, new VoiderBloodline(plugin, BloodlineType.VOIDER));
        bloodlines.put(BloodlineType.UNIVERSAL, new UniversalBloodline(plugin));
    }

    public void startSchedulers() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickPlayers, 20L, 20L);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirtyProfiles, 200L, 200L);
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            endVoidFlight(player, true);
        }
        flushDirtyProfiles();
    }

    public void registerRecipes() {
        Bukkit.removeRecipe(universalRecipeKey);
        ShapedRecipe recipe = new ShapedRecipe(universalRecipeKey, plugin.getCustomItems().createUniversalCore());
        recipe.shape("NAN", "EDV", "NSN");
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('A', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.ECHO_SHARD);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('V', Material.ECHO_SHARD);
        Bukkit.addRecipe(recipe);
    }

    public PlayerProfile profile(Player player) {
        return plugin.getPlayerDataManager().getOrCreate(player.getUniqueId());
    }

    public Bloodline getBloodline(BloodlineType type) {
        return bloodlines.get(type);
    }

    public void handleJoin(Player player) {
        clientHotkeyPlayers.remove(player.getUniqueId());
        PlayerProfile profile = profile(player);
        if (profile.activeBloodline() == BloodlineType.VOIDER && !canUseVoider(player)) {
            profile.setSingleBloodline(rollEligibleInitialBloodline(player), 1);
            markDirty(player);
        }
        if (!profile.owns(profile.activeBloodline())) {
            profile.setLevel(profile.activeBloodline(), 1);
        }
        stillSince.put(player.getUniqueId(), System.currentTimeMillis());
        lastBlocks.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        getBloodline(profile.activeBloodline()).applyPassive(player);
        updateFlightAvailability(player);
        markDirty(player);
        if (profile.freshAssignmentPending()) {
            runFirstJoinSelectionAnimation(player, profile.activeBloodline());
            profile.setFreshAssignmentPending(false);
            markDirty(player);
        } else {
            player.sendActionBar(Component.text("Bloodline loaded: " + profile.activeBloodline().displayName(), profile.activeBloodline().color()));
        }
    }

    public void handleQuit(Player player) {
        endVoidFlight(player, true);
        removeAllPassives(player);
        plugin.getPlayerDataManager().unload(player.getUniqueId());
        lastBlocks.remove(player.getUniqueId());
        stillSince.remove(player.getUniqueId());
        groundSlams.remove(player.getUniqueId());
        aquaCurses.remove(player.getUniqueId());
        aquaDashCooldown.remove(player.getUniqueId());
        TidalSurgeState surge = tidalSurges.remove(player.getUniqueId());
        if (surge != null) {
            endAquaTidalSurge(player, surge, true);
        }
        heldFireballs.remove(player.getUniqueId());
        HellDominionState dominion = spartanHellDominions.remove(player.getUniqueId());
        if (dominion != null) {
            endHellDominion(player, dominion, true);
        }
        EarthianWorldbreakerState worldbreaker = earthianWorldbreakers.remove(player.getUniqueId());
        if (worldbreaker != null) {
            restoreTemporaryTerrain(worldbreaker);
        }
        voidBlinkHits.remove(player.getUniqueId());
        universalBloodFusionUntil.remove(player.getUniqueId());
        universalRiftDashUntil.remove(player.getUniqueId());
        universalAscensionUntil.remove(player.getUniqueId());
        clientHotkeyPlayers.remove(player.getUniqueId());
        inputDebounce.remove(player.getUniqueId());
        dirtyProfiles.remove(player.getUniqueId());
        BossBar bossBar = voidFlightBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    public void handleMove(Player player, Location from, Location to) {
        if (to == null) {
            return;
        }

        if (changedBlock(from, to)) {
            lastBlocks.put(player.getUniqueId(), to.getBlock().getLocation());
            stillSince.put(player.getUniqueId(), System.currentTimeMillis());
        }

        GroundSlamState slam = groundSlams.get(player.getUniqueId());
        if (slam != null) {
            if (!isOnSolidGround(player)) {
                groundSlams.put(player.getUniqueId(), slam.withLeftGround(true));
            } else if (slam.leftGround()) {
                triggerGroundSlam(player, slam.universal());
            }
        }

        BloodlineType active = profile(player).activeBloodline();
        if (active == BloodlineType.AQUA || active == BloodlineType.UNIVERSAL) {
            getBloodline(active).applyPassive(player);
        }
        updateFlightAvailability(player);
    }

    public void handleSneakToggle(Player player) {
        updateFlightAvailability(player);
    }

    public void triggerPrimary(Player player) {
        if (!allowInput(player, "primary")) {
            return;
        }
        getBloodline(profile(player).activeBloodline()).handlePrimaryAbility(player);
        markDirty(player);
    }

    public void triggerSecondary(Player player) {
        if (!allowInput(player, "secondary")) {
            return;
        }
        getBloodline(profile(player).activeBloodline()).handleSecondaryAbility(player);
        markDirty(player);
    }

    public void triggerSpecial(Player player) {
        if (!allowInput(player, "special")) {
            return;
        }
        Bloodline bloodline = getBloodline(profile(player).activeBloodline());
        if (bloodline.supportsSpecialAbility()) {
            bloodline.handleSpecialAbility(player);
            markDirty(player);
        }
    }

    public void markClientHotkeys(Player player) {
        clientHotkeyPlayers.add(player.getUniqueId());
    }

    public boolean usesClientHotkeys(Player player) {
        return clientHotkeyPlayers.contains(player.getUniqueId());
    }

    public boolean withdrawBloodlineToBottle(Player player, ItemStack glassBottle) {
        PlayerProfile profile = profile(player);
        BloodlineType type = profile.activeBloodline();
        int level = Math.max(1, profile.activeLevel());
        if (type == BloodlineType.UNIVERSAL) {
            player.sendActionBar(Component.text("Universal bloodline cannot be bottled.", NamedTextColor.RED));
            return false;
        }
        if (glassBottle == null || glassBottle.getAmount() <= 0) {
            return false;
        }

        glassBottle.setAmount(glassBottle.getAmount() - 1);
        player.getInventory().addItem(plugin.getCustomItems().createTraitPotion(type, level));
        profile.resetActiveBloodlineLevel();
        clearBloodlineState(profile);
        getBloodline(type).applyPassive(player);
        markDirty(player);
        player.sendActionBar(Component.text("Bloodline withdrawn into a bottle.", NamedTextColor.GOLD));
        return true;
    }

    public boolean startCooldown(Player player, String key, long baseSeconds, long reductionPerLevel, long minSeconds) {
        PlayerProfile profile = profile(player);
        long now = System.currentTimeMillis();
        if (profile.getCooldown(key) > now) {
            return false;
        }

        int level = Math.max(1, profile.activeLevel());
        long cooldownSeconds = Math.max(minSeconds, baseSeconds - ((level - 1L) * reductionPerLevel));
        profile.setCooldown(key, now + cooldownSeconds * 1000L);
        markDirty(player);
        return true;
    }

    public long remainingCooldown(PlayerProfile profile, String key) {
        return Math.max(0L, profile.getCooldown(key) - System.currentTimeMillis());
    }

    public String formatCooldown(PlayerProfile profile, String key) {
        return TimeUtil.formatMillis(remainingCooldown(profile, key));
    }

    public String cooldownLine(Player player, String slot) {
        PlayerProfile profile = profile(player);
        return switch (slot) {
            case "primary" -> labelForKey(profile, primaryKey(profile.activeBloodline()));
            case "secondary" -> labelForKey(profile, secondaryKey(profile.activeBloodline()));
            case "special" -> labelForKey(profile, specialKey(profile.activeBloodline()));
            default -> "READY";
        };
    }

    public void armGroundSlam(Player player) {
        groundSlams.put(player.getUniqueId(), new GroundSlamState(System.currentTimeMillis(), false, profile(player).activeBloodline() == BloodlineType.UNIVERSAL, player.getLocation().getY()));
    }

    public void startUniversalRiftDash(Player player) {
        int level = profile(player).activeLevel();
        long durationMillis = 1_500L + Math.max(0, level - 1) * 300L;
        universalRiftDashUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
        double strength = 1.8D + Math.max(0, level - 1) * 0.12D;
        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(strength);
        direction.setY(Math.max(0.12D, direction.getY() * 0.15D));
        player.setVelocity(direction);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 40, 0.4, 0.5, 0.4, 0.04);
    }

    public void startUniversalBloodFusion(Player player) {
        int level = profile(player).activeLevel();
        long durationSeconds = 8L + Math.max(0, level - 1);
        universalBloodFusionUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationSeconds * 1000L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (durationSeconds * 20L), level >= 4 ? 1 : 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) (durationSeconds * 20L), level >= 5 ? 1 : 0, true, true, true));
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 55, 0.45, 0.7, 0.45, 0.4);
    }

    public void startUniversalAscension(Player player) {
        int level = profile(player).activeLevel();
        long durationSeconds = 10L + Math.max(0, level - 1);
        universalAscensionUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationSeconds * 1000L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (durationSeconds * 20L), level >= 5 ? 1 : 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (durationSeconds * 20L), 0, true, true, true));
        player.setGlowing(true);
        player.setAllowFlight(true);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 70, 0.55, 0.8, 0.55, 0.03);
    }

    public void prepareSpartanFireball(Player player) {
        heldFireballs.remove(player.getUniqueId());
        long holdMillis = plugin.getConfig().getLong("bloodlines.spartan.fireball.hold-seconds", 15L) * 1000L;
        heldFireballs.put(player.getUniqueId(), new HeldFireballState(System.currentTimeMillis() + holdMillis));
        player.sendActionBar(Component.text("Fireball primed. Left click to launch.", NamedTextColor.GOLD));
    }

    public boolean tryLaunchHeldSpartanFireball(Player player) {
        HeldFireballState state = heldFireballs.get(player.getUniqueId());
        if (state == null || state.expiresAt() <= System.currentTimeMillis()) {
            heldFireballs.remove(player.getUniqueId());
            return false;
        }

        int level = profile(player).activeLevel();
        double speed = plugin.getConfig().getDouble("bloodlines.spartan.fireball.speed", 1.5D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.spartan.fireball.speed-per-level", 0.1D);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Fireball fireball = player.getWorld().spawn(player.getEyeLocation().add(direction.multiply(0.9D)), Fireball.class, spawned -> {
            spawned.setGravity(false);
            spawned.setDirection(direction);
            spawned.setVelocity(direction.multiply(speed));
            spawned.setAcceleration(direction.multiply(0.12D));
            spawned.setYield((float) (plugin.getConfig().getDouble("bloodlines.spartan.fireball.explosion-power", 1.8D)
                    + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.spartan.fireball.explosion-power-per-level", 0.35D)));
            spawned.setIsIncendiary(true);
            spawned.setShooter(player);
        });
        heldFireballs.remove(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1F, 0.9F);
        player.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 20, 0.18, 0.18, 0.18, 0.02);
        return true;
    }

    public void startVoidFlight(Player player, boolean universal) {
        int level = profile(player).activeLevel();
        long durationSeconds = plugin.getConfig().getLong("bloodlines.voider.void-flight.duration-seconds", 300L)
                + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.voider.void-flight.duration-seconds-per-level", 30L);
        if (universal) {
            durationSeconds = Math.round(durationSeconds * 0.75D);
        }
        removeVoidFlightElytra(player);
        ItemStack currentChestplate = player.getInventory().getChestplate();
        storedChestplates.put(player.getUniqueId(), currentChestplate == null ? null : currentChestplate.clone());

        ItemStack elytra = plugin.getCustomItems().createVoidFlightElytra();
        player.getInventory().setChestplate(elytra);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGliding(true);
        player.setVelocity(player.getVelocity().add(new Vector(0.0D, 0.8D, 0.0D)));
        int speedAmplifier = Math.min(2, Math.max(0, (level - 1) / 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (durationSeconds * 20L), speedAmplifier, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) (durationSeconds * 20L), level >= 5 ? 1 : 0, true, true, true));
        long endsAt = System.currentTimeMillis() + durationSeconds * 1000L;
        voidFlightEndsAt.put(player.getUniqueId(), endsAt);
        BossBar bossBar = BossBar.bossBar(Component.text("Void Flight: " + TimeUtil.formatMillis(durationSeconds * 1000L), NamedTextColor.DARK_PURPLE),
                1.0F,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS);
        BossBar previousBar = voidFlightBossBars.put(player.getUniqueId(), bossBar);
        if (previousBar != null) {
            player.hideBossBar(previousBar);
        }
        player.showBossBar(bossBar);
        markDirty(player);
    }

    public boolean startVoidBlink(Player player, boolean universal) {
        int level = profile(player).activeLevel();
        double maxDistance = plugin.getConfig().getDouble("bloodlines.voider.void-blink.distance", 8.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.voider.void-blink.distance-per-level", 0.5D);
        if (universal) {
            maxDistance *= 0.8D;
        }

        Location target = findSafeBlinkDestination(player, maxDistance);
        if (target == null) {
            return false;
        }

        Location from = player.getLocation().clone();
        player.teleport(target);
        player.setFallDistance(0F);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, from.add(0, 1, 0), 25, 0.25, 0.4, 0.25, 0.03);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 35, 0.35, 0.45, 0.35, 0.05);
        if (!universal) {
            int invisibilitySeconds = plugin.getConfig().getInt("bloodlines.voider.void-blink.invisibility-seconds", 6)
                    + Math.max(0, level - 1) * plugin.getConfig().getInt("bloodlines.voider.void-blink.invisibility-seconds-per-level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, invisibilitySeconds * 20, 0, true, true, true));
        }

        long buffMillis = plugin.getConfig().getLong("bloodlines.voider.void-blink.follow-up-window-seconds", 3L) * 1000L;
        voidBlinkHits.put(player.getUniqueId(), System.currentTimeMillis() + buffMillis);
        return true;
    }

    public void runAquaWaterDash(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = aquaDashCooldown.getOrDefault(uuid, 0L);
        if (now - last < 400L) {
            return;
        }
        aquaDashCooldown.put(uuid, now);

        int level = profile(player).activeLevel();
        double radius = plugin.getConfig().getDouble("bloodlines.aqua.water-dash.hit-radius", 1.5D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.water-dash.hit-radius-per-level", 0.1D);
        double damage = plugin.getConfig().getDouble("bloodlines.aqua.water-dash.damage", 4.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.water-dash.damage-per-level", 0.8D);
        double knockback = plugin.getConfig().getDouble("bloodlines.aqua.water-dash.knockback", 1.15D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.water-dash.knockback-per-level", 0.08D);

        Set<UUID> hit = ConcurrentHashMap.newKeySet();
        for (int step = 1; step <= 6; step++) {
            Location point = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(step * 0.75D));
            for (LivingEntity nearby : point.getNearbyLivingEntities(radius, entity -> entity != player)) {
                if (!hit.add(nearby.getUniqueId())) {
                    continue;
                }
                nearby.damage(damage, player);
                Vector push = nearby.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockback);
                push.setY(0.3D);
                nearby.setVelocity(push);
                nearby.getWorld().spawnParticle(Particle.SPLASH, nearby.getLocation().add(0, 1, 0), 20, 0.3, 0.25, 0.3, 0.08);
            }
        }
    }

    public boolean applyAquaSuffocationCurse(Player player) {
        int level = profile(player).activeLevel();
        double range = plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.range", 24.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.range-per-level", 1.5D);
        RayTraceResult trace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && entity != player
        );
        if (trace == null || !(trace.getHitEntity() instanceof LivingEntity target)) {
            return false;
        }

        long durationTicks = plugin.getConfig().getLong("bloodlines.aqua.suffocation-curse.duration-seconds", 10L) * 20L;
        long durationExtra = Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.aqua.suffocation-curse.duration-seconds-per-level", 1L) * 20L;
        if (target instanceof Player targetPlayer) {
            aquaCurses.put(targetPlayer.getUniqueId(), new AquaCurseState(
                    player.getUniqueId(),
                    System.currentTimeMillis() + ((durationTicks + durationExtra) * 50L)
            ));
        } else {
            startAquaEntityCurse(player, target, level, durationTicks + durationExtra);
        }
        target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, target.getLocation().add(0, 1, 0), 28, 0.35, 0.45, 0.35, 0.04);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.85F, 0.85F);
        return true;
    }

    public void startAquaTidalSurge(Player player) {
        UUID uuid = player.getUniqueId();
        TidalSurgeState old = tidalSurges.remove(uuid);
        if (old != null) {
            endAquaTidalSurge(player, old, true);
        }

        int level = profile(player).activeLevel();
        long durationSeconds = plugin.getConfig().getLong("bloodlines.aqua.tidal-surge.duration-seconds", 10L)
                + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.aqua.tidal-surge.duration-seconds-per-level", 1L);
        long endAt = System.currentTimeMillis() + durationSeconds * 1000L;

        int strengthAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.aqua.tidal-surge.strength-amplifier-at-level-5", 2),
                level >= 5 ? 2 : Math.max(0, (level - 1) / 2)
        );
        int speedAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.aqua.tidal-surge.speed-amplifier-at-level-5", 1),
                level >= 4 ? 1 : 0
        );
        int dolphinsAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.aqua.tidal-surge.dolphins-grace-amplifier-at-level-5", 1),
                level >= 5 ? 1 : 0
        );
        double radius = plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.radius", 5.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.radius-per-level", 0.35D);
        double damage = plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.damage", 3.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.damage-per-level", 0.75D);
        double knockback = plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.knockback", 1.2D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.tidal-surge.knockback-per-level", 0.08D);

        for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(radius, entity -> entity != player)) {
            nearby.damage(damage, player);
            Vector push = nearby.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockback);
            push.setY(0.35D);
            nearby.setVelocity(push);
        }

        Location returnLocation = player.getLocation().clone();
        boolean teleported = false;
        if (plugin.getConfig().getBoolean("bloodlines.aqua.tidal-surge.teleport-water-world.enabled", false)) {
            World waterWorld = Bukkit.getWorld(plugin.getConfig().getString("bloodlines.aqua.tidal-surge.teleport-water-world.world-name", "bloodline_aqua_realm"));
            if (waterWorld != null) {
                Location target = new Location(
                        waterWorld,
                        0.5D,
                        plugin.getConfig().getInt("bloodlines.aqua.tidal-surge.teleport-water-world.spawn-y", 65),
                        0.5D,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );
                player.teleport(target);
                teleported = true;
            }
        }

        List<TemporaryWaterBlock> changedBlocks = createTemporaryWaterArea(player.getLocation(), radius);
        tidalSurges.put(uuid, new TidalSurgeState(endAt, changedBlocks, returnLocation, teleported));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (durationSeconds * 20L), strengthAmplifier, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (durationSeconds * 20L), speedAmplifier, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, (int) (durationSeconds * 20L), dolphinsAmplifier, true, true, true));
    }

    public void startSpartanHellDominion(Player player) {
        int level = profile(player).activeLevel();
        long durationSeconds = plugin.getConfig().getLong("bloodlines.spartan.hell-dominion.duration-seconds", 10L)
                + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.spartan.hell-dominion.duration-seconds-per-level", 1L);
        Location activationLocation = player.getLocation().clone();
        Location returnLocation = activationLocation.clone();
        boolean teleported = false;
        sendHellDominionTargetsToNether(player, activationLocation);
        if (plugin.getConfig().getBoolean("bloodlines.spartan.hell-dominion.teleport-fire-world.enabled", false)) {
            World fireWorld = Bukkit.getWorld(plugin.getConfig().getString("bloodlines.spartan.hell-dominion.teleport-fire-world.world-name", "bloodline_spartan_realm"));
            if (fireWorld != null) {
                Location target = new Location(fireWorld, 0.5D, plugin.getConfig().getInt("bloodlines.spartan.hell-dominion.teleport-fire-world.spawn-y", 80), 0.5D, player.getLocation().getYaw(), player.getLocation().getPitch());
                player.teleport(target);
                teleported = true;
            }
        }
        spartanHellDominions.put(player.getUniqueId(), new HellDominionState(System.currentTimeMillis() + durationSeconds * 1000L, returnLocation, teleported));
        int strengthAmplifier = Math.min(plugin.getConfig().getInt("bloodlines.spartan.hell-dominion.strength-amplifier-at-level-5", 2), level >= 5 ? 2 : Math.max(0, (level - 1) / 2));
        int speedAmplifier = Math.min(plugin.getConfig().getInt("bloodlines.spartan.hell-dominion.speed-amplifier-at-level-5", 1), level >= 4 ? 1 : 0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (durationSeconds * 20L), strengthAmplifier, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (durationSeconds * 20L), speedAmplifier, true, true, true));
        player.setFireTicks((int) (durationSeconds * 20L));
    }

    private void sendHellDominionTargetsToNether(Player player, Location center) {
        double radius = plugin.getConfig().getDouble("bloodlines.spartan.hell-dominion.nether-send-radius", 5.0D);
        int maxTargets = Math.max(0, plugin.getConfig().getInt("bloodlines.spartan.hell-dominion.nether-send-max-targets", 2));
        if (maxTargets <= 0) {
            return;
        }

        World nether = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NETHER)
                .findFirst()
                .orElse(null);
        if (nether == null) {
            return;
        }

        List<LivingEntity> targets = center.getNearbyLivingEntities(radius, entity -> entity != player).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .limit(maxTargets)
                .toList();
        for (LivingEntity target : targets) {
            Location destination = findSafeNetherLocation(nether, target.getLocation(), target.getLocation().getYaw(), target.getLocation().getPitch());
            if (destination == null) {
                continue;
            }
            target.teleport(destination);
            target.setFireTicks(0);
            target.setFallDistance(0F);
            target.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination.clone().add(0, 1, 0), 22, 0.35, 0.55, 0.35, 0.03);
            target.getWorld().playSound(destination, Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, SoundCategory.PLAYERS, 0.9F, 0.8F);
            if (target instanceof Player targetPlayer) {
                targetPlayer.sendActionBar(Component.text("Hell Dominion dragged you into the Nether.", NamedTextColor.RED));
            }
        }
    }

    public void startEarthianWorldbreaker(Player player) {
        UUID uuid = player.getUniqueId();
        EarthianWorldbreakerState old = earthianWorldbreakers.remove(uuid);

        int level = profile(player).activeLevel();
        long durationSeconds = plugin.getConfig().getLong("bloodlines.earthian.worldbreaker.duration-seconds", 8L)
                + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.earthian.worldbreaker.duration-seconds-per-level", 1L);
        double radius = plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.radius", 12.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.radius-per-level", 0.9D);
        double damage = plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.damage", 12.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.damage-per-level", 1.8D);
        double knockback = plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.knockback", 2.8D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.knockback-per-level", 0.18D);

        for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(radius, entity -> entity != player)) {
            nearby.damage(damage, player);
            Vector push = nearby.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockback);
            push.setY(0.55D);
            nearby.setVelocity(push);
        }

        int resistanceAmplifier = Math.min(plugin.getConfig().getInt("bloodlines.earthian.worldbreaker.resistance-amplifier-at-level-5", 2), level >= 5 ? 2 : 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) (durationSeconds * 20L), resistanceAmplifier, true, true, true));
        earthianWorldbreakers.put(uuid, new EarthianWorldbreakerState(System.currentTimeMillis() + durationSeconds * 1000L, player.getLocation().clone(), radius, List.of()));

        int tntCount = plugin.getConfig().getInt("bloodlines.earthian.worldbreaker.tnt-count", 12)
                + Math.max(0, level - 1) * plugin.getConfig().getInt("bloodlines.earthian.worldbreaker.tnt-count-per-level", 2);
        double tntSpawnRadius = plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.tnt-spawn-radius", 4.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.tnt-spawn-radius-per-level", 0.35D);
        int fuseTicks = Math.max(10, plugin.getConfig().getInt("bloodlines.earthian.worldbreaker.tnt-fuse-ticks", 32));
        float explosionPower = (float) (plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.explosion-power", 4.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.earthian.worldbreaker.explosion-power-per-level", 0.45D));
        Location center = player.getLocation().clone();
        for (int index = 0; index < tntCount; index++) {
            double angle = (Math.PI * 2D * index) / Math.max(1, tntCount);
            double distance = tntSpawnRadius * (0.65D + ThreadLocalRandom.current().nextDouble(0.45D));
            Location spawnLocation = center.clone().add(
                    Math.cos(angle) * distance,
                    0.2D + ThreadLocalRandom.current().nextDouble(0.6D),
                    Math.sin(angle) * distance
            );
            player.getWorld().spawn(spawnLocation, TNTPrimed.class, tnt -> {
                tnt.setFuseTicks(fuseTicks + ThreadLocalRandom.current().nextInt(0, 12));
                tnt.setYield(explosionPower);
                tnt.setIsIncendiary(false);
                tnt.setSource(player);
                tnt.setVelocity(new Vector(
                        ThreadLocalRandom.current().nextDouble(-0.12D, 0.12D),
                        ThreadLocalRandom.current().nextDouble(0.18D, 0.35D),
                        ThreadLocalRandom.current().nextDouble(-0.12D, 0.12D)
                ));
            });
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 6);
    }

    public void endVoidFlight(Player player, boolean interrupted) {
        UUID uuid = player.getUniqueId();
        if (!voidFlightEndsAt.containsKey(uuid)) {
            return;
        }
        voidFlightEndsAt.remove(uuid);
        BossBar bossBar = voidFlightBossBars.remove(uuid);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        ItemStack stored = storedChestplates.remove(uuid);
        player.setGliding(false);
        restoreChestplateAfterVoidFlight(player, stored);
        removeVoidFlightElytra(player);
        if (!isOnSolidGround(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false, true));
        }
        updateFlightAvailability(player);
        if (!interrupted) {
            player.sendActionBar(Component.text("Void Flight ended", NamedTextColor.GRAY));
        }
    }

    public void applyVoiderDailyPassive(Player player) {
        PlayerProfile profile = profile(player);
        int level = profile.activeLevel();
        long now = System.currentTimeMillis();
        long cycleMillis = 24L * 60L * 60L * 1000L;
        if (profile.voidDailyEffect() == null || now - profile.voidDailyEffectAssignedAt() >= cycleMillis) {
            PotionEffectType effect = voidEffects.get(ThreadLocalRandom.current().nextInt(voidEffects.size()));
            profile.setVoidDailyEffect(effect.getKey().getKey());
            profile.setVoidDailyEffectAssignedAt(now);
            markDirty(player);
        }

        PotionEffectType effectType = Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(profile.voidDailyEffect()));
        if (effectType != null) {
            int amplifier = switch (level) {
                case 5 -> Math.max(1, plugin.getConfig().getInt("bloodlines.voider.passive.amplifier-at-level-5", 2));
                case 4 -> 1;
                default -> 0;
            };
            player.addPotionEffect(new PotionEffect(effectType, 220, amplifier, true, false, true));
        }
    }

    public void removeVoiderDailyPassive(Player player) {
        for (PotionEffectType effect : voidEffects) {
            player.removePotionEffect(effect);
        }
    }

    public boolean tryVoidSend(Player player, boolean universal) {
        int level = profile(player).activeLevel();
        double range = plugin.getConfig().getDouble("bloodlines.voider.void-send.range", 35.0D)
                + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.voider.void-send.range-per-level", 2.0D);
        if (universal) {
            range *= 0.8D;
        } else {
            range *= 1.25D;
        }

        RayTraceResult trace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && entity != player
        );
        if (trace == null || !(trace.getHitEntity() instanceof LivingEntity target)) {
            return false;
        }

        World end = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.THE_END)
                .findFirst()
                .orElse(null);
        if (end == null) {
            player.sendActionBar(Component.text("No End dimension available.", NamedTextColor.RED));
            return false;
        }

        int radius = plugin.getConfig().getInt("bloodlines.voider.void-send.random-radius", 900)
                + Math.max(0, level - 1) * plugin.getConfig().getInt("bloodlines.voider.void-send.random-radius-per-level", 75);
        Location destination = null;
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            destination = findSafeEndstoneLocation(end, x, z);
            if (destination != null) {
                break;
            }
        }
        if (destination == null) {
            Location spawn = end.getSpawnLocation();
            destination = findSafeEndstoneLocation(end, spawn.getBlockX(), spawn.getBlockZ());
        }
        if (destination == null) {
            player.sendActionBar(Component.text("No safe End Stone destination found.", NamedTextColor.RED));
            return false;
        }

        target.teleport(destination);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 70, 0.6, 1.0, 0.6, 0.06);
        if (target instanceof Player targetPlayer) {
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1F, 0.7F);
        }
        return true;
    }

    public boolean consumeVoidSendCharge(Player player) {
        PlayerProfile profile = profile(player);
        rechargeVoidSend(profile);
        if (profile.voidSendCharges() <= 0) {
            return false;
        }
        profile.setVoidSendCharges(profile.voidSendCharges() - 1);
        if (profile.voidSendCharges() < 2) {
            profile.setVoidSendLastRechargeAt(System.currentTimeMillis());
        }
        markDirty(player);
        return true;
    }

    public void refundVoidSendCharge(Player player) {
        PlayerProfile profile = profile(player);
        profile.setVoidSendCharges(profile.voidSendCharges() + 1);
        markDirty(player);
    }

    public String nextVoidSendRecharge(Player player) {
        PlayerProfile profile = profile(player);
        rechargeVoidSend(profile);
        if (profile.voidSendCharges() > 0) {
            return "READY";
        }
        long rechargeMillis = plugin.getConfig().getLong("bloodlines.voider.void-send.recharge-hours", 12L) * 60L * 60L * 1000L;
        long remaining = (profile.voidSendLastRechargeAt() + rechargeMillis) - System.currentTimeMillis();
        return TimeUtil.formatMillis(remaining);
    }

    public void switchActiveBloodline(Player player, BloodlineType type) {
        PlayerProfile profile = profile(player);
        if (type != profile.activeBloodline()) {
            player.sendActionBar(Component.text("You can only change bloodlines with trait potions.", NamedTextColor.RED));
        }
    }

    public boolean applyTraitPotion(Player player, BloodlineType type, int level) {
        if (type == BloodlineType.VOIDER && !canUseVoider(player)) {
            player.sendActionBar(Component.text("Voider is locked to " + allowedVoiderOwnerName() + ".", NamedTextColor.RED));
            return false;
        }
        PlayerProfile profile = profile(player);
        removeAllPassives(player);
        profile.setSingleBloodline(type, Math.max(1, Math.min(PlayerProfile.MAX_LEVEL, level)));
        clearBloodlineState(profile);
        getBloodline(type).applyPassive(player);
        updateFlightAvailability(player);
        markDirty(player);
        player.sendActionBar(Component.text("Switched to " + type.displayName() + " level " + profile.activeLevel(), NamedTextColor.GREEN));
        return true;
    }

    public void applyUpgradePotion(Player player) {
        PlayerProfile profile = profile(player);
        BloodlineType active = profile.activeBloodline();
        int current = profile.level(active);
        if (current >= PlayerProfile.MAX_LEVEL) {
            player.sendActionBar(Component.text(active.displayName() + " is already max level.", NamedTextColor.RED));
            return;
        }
        profile.setLevel(active, current + 1);
        markDirty(player);
        player.sendActionBar(Component.text(active.displayName() + " upgraded to level " + profile.level(active), NamedTextColor.GOLD));
    }

    public void grantBloodline(Player player, BloodlineType type, int level) {
        if (type == BloodlineType.VOIDER && !canUseVoider(player)) {
            player.sendActionBar(Component.text("Voider is locked to " + allowedVoiderOwnerName() + ".", NamedTextColor.RED));
            return;
        }
        PlayerProfile profile = profile(player);
        profile.setSingleBloodline(type, Math.max(1, Math.min(PlayerProfile.MAX_LEVEL, level)));
        clearBloodlineState(profile);
        removeAllPassives(player);
        getBloodline(type).applyPassive(player);
        updateFlightAvailability(player);
        markDirty(player);
    }

    public void forceActiveBloodline(Player player, BloodlineType type, int level) {
        if (type == BloodlineType.VOIDER && !canUseVoider(player)) {
            player.sendActionBar(Component.text("Voider is locked to " + allowedVoiderOwnerName() + ".", NamedTextColor.RED));
            return;
        }
        PlayerProfile profile = profile(player);
        removeAllPassives(player);
        profile.setSingleBloodline(type, Math.max(1, Math.min(PlayerProfile.MAX_LEVEL, level)));
        clearBloodlineState(profile);
        getBloodline(type).applyPassive(player);
        updateFlightAvailability(player);
        markDirty(player);
    }

    public void adminSetBloodline(UUID targetUuid, BloodlineType type, int level) {
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            forceActiveBloodline(online, type, level);
            plugin.getPlayerDataManager().saveSync(profile(online));
            return;
        }

        PlayerProfile profile = plugin.getPlayerDataManager().getOrCreate(targetUuid);
        if (type == BloodlineType.VOIDER && !canUseVoider(targetUuid)) {
            return;
        }
        clearBloodlineState(profile);
        profile.setSingleBloodline(type, Math.max(1, Math.min(PlayerProfile.MAX_LEVEL, level)));
        plugin.getPlayerDataManager().saveSync(profile);
    }

    public void maxAllBaseBloodlines(Player player) {
        PlayerProfile profile = profile(player);
        profile.setSingleBloodline(profile.activeBloodline(), PlayerProfile.MAX_LEVEL);
        markDirty(player);
    }

    public BloodlineType rerollInitialBloodline(Player player, boolean animated) {
        PlayerProfile profile = profile(player);
        BloodlineType rolled = rollEligibleInitialBloodline(player);
        removeAllPassives(player);
        profile.setSingleBloodline(rolled, 1);
        clearBloodlineState(profile);
        if (animated) {
            runFirstJoinSelectionAnimation(player, rolled);
        } else {
            player.sendActionBar(Component.text("Rerolled to " + rolled.displayName(), rolled.color()));
        }
        getBloodline(rolled).applyPassive(player);
        updateFlightAvailability(player);
        markDirty(player);
        return rolled;
    }

    public void unlockUniversal(Player player) {
        PlayerProfile profile = profile(player);
        if (!profile.hasAllBaseBloodlinesAtMax()) {
            player.sendActionBar(Component.text("Max all four base bloodlines first.", NamedTextColor.RED));
            return;
        }
        profile.setLevel(BloodlineType.UNIVERSAL, 1);
        profile.setActiveBloodline(BloodlineType.UNIVERSAL);
        removeAllPassives(player);
        getBloodline(BloodlineType.UNIVERSAL).applyPassive(player);
        markDirty(player);
        player.sendActionBar(Component.text("Universal Bloodline unlocked.", NamedTextColor.LIGHT_PURPLE));
    }

    public void handleDamageTaken(Player player, double originalDamage, org.bukkit.event.entity.EntityDamageEvent event) {
        BloodlineType active = profile(player).activeBloodline();
        EarthianWorldbreakerState worldbreaker = earthianWorldbreakers.get(player.getUniqueId());
        if (worldbreaker != null
                && worldbreaker.endsAt() > System.currentTimeMillis()
                && (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            event.setCancelled(true);
            event.setDamage(0.0D);
            return;
        }
        if ((active == BloodlineType.EARTHIAN || active == BloodlineType.UNIVERSAL)
                && event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) {
            double base = plugin.getConfig().getDouble("bloodlines.earthian.passive.fall-damage-multiplier", 0.5D);
            double improvement = plugin.getConfig().getDouble("bloodlines.earthian.passive.fall-damage-multiplier-improvement-per-level", 0.05D);
            double scaled = Math.max(0.1D, base - Math.max(0, profile(player).activeLevel() - 1) * improvement);
            event.setDamage(originalDamage * scaled);
        }
    }

    public void handleVelocity(Player player) {
        BloodlineType active = profile(player).activeBloodline();
        if (earthianWorldbreakers.containsKey(player.getUniqueId())) {
            player.setVelocity(new Vector(0.0D, Math.max(player.getVelocity().getY(), 0.0D), 0.0D));
            return;
        }
        if (active == BloodlineType.EARTHIAN || active == BloodlineType.UNIVERSAL) {
            double base = plugin.getConfig().getDouble("bloodlines.earthian.passive.knockback-multiplier", 0.6D);
            double improvement = plugin.getConfig().getDouble("bloodlines.earthian.passive.knockback-multiplier-improvement-per-level", 0.05D);
            double scaled = Math.max(0.15D, base - Math.max(0, profile(player).activeLevel() - 1) * improvement);
            player.setVelocity(player.getVelocity().multiply(scaled));
        }
    }

    public void handleMeleeHit(Player attacker, Entity target) {
        PlayerProfile attackerProfile = profile(attacker);
        if (!(target instanceof LivingEntity victim)) {
            return;
        }

        if (attackerProfile.activeBloodline() == BloodlineType.SPARTAN && attackerProfile.spartanFlamingHandsUntil() >= System.currentTimeMillis()) {
            int level = attackerProfile.activeLevel();
            long burnSeconds = plugin.getConfig().getLong("bloodlines.spartan.flaming-hands.burn-seconds", 300L)
                    + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.spartan.flaming-hands.burn-seconds-per-level", 30L);
            long burnMillis = burnSeconds * 1000L;
            victim.setFireTicks((int) (burnMillis / 50L));
            if (victim instanceof Player victimPlayer) {
                PlayerProfile victimProfile = profile(victimPlayer);
                victimProfile.setCursedBySpartan(attacker.getUniqueId());
                victimProfile.setCursedUntil(System.currentTimeMillis() + burnMillis);
                markDirty(victimPlayer);
            }
        }

        Long blinkHitUntil = voidBlinkHits.get(attacker.getUniqueId());
        if (blinkHitUntil != null && blinkHitUntil >= System.currentTimeMillis()) {
            int level = attackerProfile.activeBloodline() == BloodlineType.VOIDER
                    ? attackerProfile.activeLevel()
                    : attackerProfile.level(BloodlineType.VOIDER);
            double bonusDamage = plugin.getConfig().getDouble("bloodlines.voider.void-blink.bonus-damage", 3.0D)
                    + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.voider.void-blink.bonus-damage-per-level", 0.75D);
            double knockback = plugin.getConfig().getDouble("bloodlines.voider.void-blink.knockback", 0.85D)
                    + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.voider.void-blink.knockback-per-level", 0.06D);
            victim.damage(bonusDamage, attacker);
            Vector push = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(knockback);
            push.setY(0.2D);
            victim.setVelocity(push);
            victim.getWorld().spawnParticle(Particle.REVERSE_PORTAL, victim.getLocation().add(0, 1, 0), 24, 0.25, 0.35, 0.25, 0.04);
            voidBlinkHits.remove(attacker.getUniqueId());
        }

        HellDominionState dominion = spartanHellDominions.get(attacker.getUniqueId());
        if (dominion != null && dominion.endsAt() >= System.currentTimeMillis()) {
            int level = attackerProfile.activeBloodline() == BloodlineType.SPARTAN ? attackerProfile.activeLevel() : attackerProfile.level(BloodlineType.SPARTAN);
            long bonusBurnSeconds = plugin.getConfig().getLong("bloodlines.spartan.hell-dominion.hit-burn-seconds", 6L)
                    + Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.spartan.hell-dominion.hit-burn-seconds-per-level", 1L);
            victim.setFireTicks(Math.max(victim.getFireTicks(), (int) (bonusBurnSeconds * 20L)));
            if (victim instanceof Player victimPlayer) {
                PlayerProfile victimProfile = profile(victimPlayer);
                long extended = Math.max(victimProfile.cursedUntil(), System.currentTimeMillis()) + (bonusBurnSeconds * 1000L);
                victimProfile.setCursedBySpartan(attacker.getUniqueId());
                victimProfile.setCursedUntil(extended);
                markDirty(victimPlayer);
            }
        }

        Long fusionUntil = universalBloodFusionUntil.get(attacker.getUniqueId());
        if (fusionUntil != null && fusionUntil >= System.currentTimeMillis()) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 60));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, true, true));
        }

        Long ascensionUntil = universalAscensionUntil.get(attacker.getUniqueId());
        if (ascensionUntil != null && ascensionUntil >= System.currentTimeMillis()) {
            victim.damage(3.0D + Math.max(0, attackerProfile.activeLevel() - 1) * 0.8D, attacker);
            PotionEffectType debuff = switch (ThreadLocalRandom.current().nextInt(4)) {
                case 0 -> PotionEffectType.SLOWNESS;
                case 1 -> PotionEffectType.WEAKNESS;
                case 2 -> PotionEffectType.MINING_FATIGUE;
                default -> PotionEffectType.BLINDNESS;
            };
            victim.addPotionEffect(new PotionEffect(debuff, 60, 0, true, true, true));
        }

        Long riftDashUntil = universalRiftDashUntil.get(attacker.getUniqueId());
        if (riftDashUntil != null && riftDashUntil >= System.currentTimeMillis()) {
            victim.damage(4.0D + Math.max(0, attackerProfile.activeLevel() - 1) * 0.7D, attacker);
            Vector push = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(0.8D);
            push.setY(0.2D);
            victim.setVelocity(push);
            universalRiftDashUntil.remove(attacker.getUniqueId());
        }
    }

    public void handleDeath(Player victim, Player killer, org.bukkit.event.entity.PlayerDeathEvent event) {
        if (killer != null) {
            PlayerProfile killerProfile = profile(killer);
            if (killerProfile.cursedBySpartan() != null && killerProfile.cursedBySpartan().equals(victim.getUniqueId())) {
                clearCurse(killerProfile);
                markDirty(killer);
            }
        }

        PlayerProfile victimProfile = profile(victim);
        BloodlineType dropType = victimProfile.activeBloodline() == BloodlineType.UNIVERSAL ? BloodlineType.randomBase() : victimProfile.activeBloodline();
        int dropLevel = Math.max(1, victimProfile.activeLevel());
        double rareChance = plugin.getConfig().getDouble("drops.trait-potion-chance", 0.25D);
        ItemStack reward = ThreadLocalRandom.current().nextDouble() < rareChance
                ? plugin.getCustomItems().createTraitPotion(dropType, dropLevel)
                : plugin.getCustomItems().createUpgradePotion();
        victim.getWorld().dropItemNaturally(victim.getLocation(), reward);

        endVoidFlight(victim, true);
        aquaCurses.remove(victim.getUniqueId());
        heldFireballs.remove(victim.getUniqueId());
        HellDominionState dominion = spartanHellDominions.remove(victim.getUniqueId());
        if (dominion != null) {
            endHellDominion(victim, dominion, false);
        }
        voidBlinkHits.remove(victim.getUniqueId());
        universalBloodFusionUntil.remove(victim.getUniqueId());
        universalRiftDashUntil.remove(victim.getUniqueId());
        universalAscensionUntil.remove(victim.getUniqueId());
        TidalSurgeState surge = tidalSurges.remove(victim.getUniqueId());
        if (surge != null) {
            endAquaTidalSurge(victim, surge, false);
        }
        EarthianWorldbreakerState worldbreaker = earthianWorldbreakers.remove(victim.getUniqueId());
        if (worldbreaker != null) {
            restoreTemporaryTerrain(worldbreaker);
        }
        clearCurse(victimProfile);
        victimProfile.resetActiveBloodlineLevel();
        clearBloodlineState(victimProfile);
        markDirty(victim);
    }

    public void markDirty(Player player) {
        dirtyProfiles.add(player.getUniqueId());
    }

    public ItemStack resolveCustomCraftResult(ItemStack[] matrix) {
        BloodlineType shardType = resolveShardCraft(matrix);
        if (shardType != null) {
            return plugin.getCustomItems().createBloodlineShard(shardType);
        }
        if (matchesUniversalShardRecipe(matrix)) {
            return plugin.getCustomItems().createUniversalCore();
        }
        return null;
    }

    public boolean isUniversalRecipe(ItemStack result) {
        String type = plugin.getCustomItems().getItemType(result);
        return type != null && type.equals(dev.zahen.bloodline.item.CustomItems.TYPE_UNIVERSAL_CORE);
    }

    public NamespacedKey universalRecipeKey() {
        return universalRecipeKey;
    }

    private void tickPlayers() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profile(player);
            BloodlineType active = profile.activeBloodline();
            getBloodline(active).applyPassive(player);

            GroundSlamState slamState = groundSlams.get(player.getUniqueId());
            if (slamState != null && slamState.leftGround()) {
                double slamGravity = plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.slam-gravity", 1.6D);
                Vector velocity = player.getVelocity();
                player.setVelocity(new Vector(velocity.getX(), Math.min(-slamGravity, velocity.getY() - 0.6D), velocity.getZ()));
            }

            if ((active == BloodlineType.EARTHIAN || active == BloodlineType.UNIVERSAL)
                    && now - stillSince.getOrDefault(player.getUniqueId(), now) >= earthianRegenDelayMillis(profile) ) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, true));
            } else if (active != BloodlineType.EARTHIAN && active != BloodlineType.UNIVERSAL) {
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }

            if (profile.cursedUntil() > now) {
                player.setFireTicks(60);
                double damage = plugin.getConfig().getDouble("bloodlines.spartan.flaming-hands.damage-per-second", 1.0D)
                        + Math.max(0, profile.level(BloodlineType.SPARTAN) - 1) * plugin.getConfig().getDouble("bloodlines.spartan.flaming-hands.damage-per-second-per-level", 0.35D);
                player.damage(damage);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 6, 0.2, 0.4, 0.2, 0.01);
            } else if (profile.cursedUntil() != 0L) {
                clearCurse(profile);
                markDirty(player);
            }

            AquaCurseState aquaCurse = aquaCurses.get(player.getUniqueId());
            if (aquaCurse != null) {
                if (player.isInWater() || aquaCurse.endsAt() <= now) {
                    aquaCurses.remove(player.getUniqueId());
                    player.setRemainingAir(player.getMaximumAir());
                } else {
                    PlayerProfile casterProfile = plugin.getPlayerDataManager().getOrCreate(aquaCurse.caster());
                    int casterLevel = casterProfile.activeBloodline() == BloodlineType.AQUA
                            ? casterProfile.activeLevel()
                            : casterProfile.level(BloodlineType.AQUA);
                    int slowAmplifier = Math.min(
                            plugin.getConfig().getInt("bloodlines.aqua.suffocation-curse.slowness-amplifier-at-level-5", 2),
                            casterLevel >= 5 ? 2 : Math.max(0, (casterLevel - 1) / 2)
                    );
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slowAmplifier, true, true, true));
                    player.setRemainingAir(Math.max(-20, player.getRemainingAir() - plugin.getConfig().getInt("bloodlines.aqua.suffocation-curse.air-loss-per-second", 45)));
                    Player caster = Bukkit.getPlayer(aquaCurse.caster());
                    double curseDamage = plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.damage-per-second", 1.0D);
                    if (caster != null && caster.isOnline()) {
                        curseDamage += Math.max(0, casterLevel - 1) * plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.damage-per-second-per-level", 0.35D);
                        player.damage(curseDamage, caster);
                    } else {
                        curseDamage += Math.max(0, casterLevel - 1) * plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.damage-per-second-per-level", 0.35D);
                        player.damage(curseDamage);
                    }
                    player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(0, 1, 0), 8, 0.2, 0.35, 0.2, 0.03);
                }
            }

            if (profile.spartanFlamingHandsUntil() > 0L && profile.spartanFlamingHandsUntil() <= now) {
                profile.setSpartanFlamingHandsUntil(0L);
                markDirty(player);
            }

            HeldFireballState heldFireball = heldFireballs.get(player.getUniqueId());
            if (heldFireball != null) {
                if (heldFireball.expiresAt() <= now) {
                    heldFireballs.remove(player.getUniqueId());
                }
            }

            HellDominionState dominion = spartanHellDominions.get(player.getUniqueId());
            if (dominion != null) {
                if (dominion.endsAt() <= now) {
                    spartanHellDominions.remove(player.getUniqueId());
                    endHellDominion(player, dominion, true);
                } else {
                    int level = profile.activeBloodline() == BloodlineType.SPARTAN ? profile.activeLevel() : profile.level(BloodlineType.SPARTAN);
                    double auraRadius = plugin.getConfig().getDouble("bloodlines.spartan.hell-dominion.aura-radius", 4.0D)
                            + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.spartan.hell-dominion.aura-radius-per-level", 0.2D);
                    double auraDamage = plugin.getConfig().getDouble("bloodlines.spartan.hell-dominion.aura-damage-per-second", 1.0D)
                            + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.spartan.hell-dominion.aura-damage-per-second-per-level", 0.25D);
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 12, 0.5, 0.7, 0.5, 0.03);
                    for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(auraRadius, entity -> entity != player)) {
                        nearby.damage(auraDamage, player);
                        nearby.setFireTicks(Math.max(nearby.getFireTicks(), 40));
                    }
                }
            }

            Long fusionUntil = universalBloodFusionUntil.get(player.getUniqueId());
            if (fusionUntil != null && fusionUntil <= now) {
                universalBloodFusionUntil.remove(player.getUniqueId());
            }

            Long ascensionUntil = universalAscensionUntil.get(player.getUniqueId());
            if (ascensionUntil != null) {
                if (ascensionUntil <= now) {
                    universalAscensionUntil.remove(player.getUniqueId());
                    player.setGlowing(false);
                    if (!voidFlightEndsAt.containsKey(player.getUniqueId())) {
                        player.setAllowFlight(false);
                    }
                } else {
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.02);
                }
            }

            Long riftDashUntil = universalRiftDashUntil.get(player.getUniqueId());
            if (riftDashUntil != null) {
                if (riftDashUntil <= now) {
                    universalRiftDashUntil.remove(player.getUniqueId());
                } else {
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.6, 0), 4, 0.2, 0.1, 0.2, 0.01);
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 0.4, 0), 4, 0.2, 0.1, 0.2, 0.04);
                    for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(1.6D, entity -> entity != player)) {
                        nearby.setFireTicks(Math.max(nearby.getFireTicks(), 40));
                        nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, true, true, true));
                    }
                }
            }

            Long blinkHitUntil = voidBlinkHits.get(player.getUniqueId());
            if (blinkHitUntil != null && blinkHitUntil <= now) {
                voidBlinkHits.remove(player.getUniqueId());
            }

            Long flightEnds = voidFlightEndsAt.get(player.getUniqueId());
            if (flightEnds != null) {
                BossBar flightBossBar = voidFlightBossBars.get(player.getUniqueId());
                if (flightEnds <= now) {
                    endVoidFlight(player, false);
                } else if (flightBossBar != null) {
                    long remainingMillis = flightEnds - now;
                    long totalDurationMillis = plugin.getConfig().getLong("bloodlines.voider.void-flight.duration-seconds", 300L) * 1000L;
                    int level = profile.activeBloodline() == BloodlineType.VOIDER ? profile.activeLevel() : profile.level(BloodlineType.VOIDER);
                    totalDurationMillis += Math.max(0, level - 1) * plugin.getConfig().getLong("bloodlines.voider.void-flight.duration-seconds-per-level", 30L) * 1000L;
                    float progress = Math.max(0.0F, Math.min(1.0F, remainingMillis / (float) totalDurationMillis));
                    flightBossBar.name(Component.text("Void Flight: " + TimeUtil.formatMillis(remainingMillis), NamedTextColor.DARK_PURPLE));
                    flightBossBar.progress(progress);
                }
            }

            TidalSurgeState surge = tidalSurges.get(player.getUniqueId());
            if (surge != null) {
                if (surge.endsAt() <= now) {
                    tidalSurges.remove(player.getUniqueId());
                    endAquaTidalSurge(player, surge, true);
                } else {
                    player.getWorld().spawnParticle(Particle.FALLING_WATER, player.getLocation().add(0, 1, 0), 8, 0.35, 0.35, 0.35, 0.04);
                }
            }

            EarthianWorldbreakerState worldbreaker = earthianWorldbreakers.get(player.getUniqueId());
            if (worldbreaker != null) {
                if (worldbreaker.endsAt() <= now) {
                    earthianWorldbreakers.remove(player.getUniqueId());
                }
            }

            rechargeVoidSend(profile);
            updateFlightAvailability(player);
            sendCooldownBar(player);
        }
    }

    private void triggerGroundSlam(Player player, boolean universal) {
        GroundSlamState state = groundSlams.remove(player.getUniqueId());
        double radius = plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.radius", 4.5D);
        radius += Math.max(0, profile(player).activeLevel() - 1) * plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.radius-per-level", 0.35D);
        double damage = plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.damage", 5.0D);
        double bonusPerBlock = plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.damage-per-fall-block", 0.6D);
        double fallBonus = state == null ? 0.0D : Math.max(0.0D, state.startY() - player.getLocation().getY()) * bonusPerBlock;
        if (universal) {
            radius *= 0.8D;
            damage *= 0.75D;
        } else {
            damage += (profile(player).activeLevel() - 1) * plugin.getConfig().getDouble("bloodlines.earthian.ground-slam.damage-per-level", 1.0D);
            damage += fallBonus;
        }

        player.setFallDistance(0F);
        player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), (float) Math.min(10.0D, radius / 1.8D), false, false, player);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 2);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 0.1, 0), 65, radius / 2, 0.2, radius / 2,
                player.getLocation().getBlock().getBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.9F, 0.8F);

        for (LivingEntity nearby : player.getLocation().getNearbyLivingEntities(radius, entity -> entity != player)) {
            nearby.damage(damage, player);
            Vector push = nearby.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2D);
            push.setY(0.45D);
            nearby.setVelocity(push);
            if (nearby instanceof Player target) {
                target.setFallDistance(0F);
            }
        }
    }

    private void sendCooldownBar(Player player) {
        PlayerProfile profile = profile(player);
        String primary = labelForKey(profile, primaryKey(profile.activeBloodline()));
        String secondary = labelForKey(profile, secondaryKey(profile.activeBloodline()));
        String special = labelForKey(profile, specialKey(profile.activeBloodline()));
        player.sendActionBar(Component.text("P: " + primary + " | S: " + secondary + " | X: " + special, NamedTextColor.YELLOW));
    }

    private String labelForKey(PlayerProfile profile, String key) {
        if (key == null) {
            return "N/A";
        }
        if (key.equals("voider.void_send")) {
            return "Charges " + profile.voidSendCharges() + "/2";
        }
        if (key.equals("voider.void_blink_hit")) {
            Long until = voidBlinkHits.get(profile.uuid());
            if (until == null || until <= System.currentTimeMillis()) {
                return "READY";
            }
            return TimeUtil.formatMillis(until - System.currentTimeMillis());
        }
        return formatCooldown(profile, key);
    }

    private String primaryKey(BloodlineType type) {
        return switch (type) {
            case AQUA -> "aqua.water_dash";
            case SPARTAN -> "spartan.fireball";
            case EARTHIAN -> "earthian.ground_slam";
            case VOIDER -> "voider.void_blink";
            case UNIVERSAL -> "universal.rift_dash";
            default -> null;
        };
    }

    private String secondaryKey(BloodlineType type) {
        return switch (type) {
            case AQUA -> "aqua.suffocation_curse";
            case SPARTAN -> "spartan.flaming_hands";
            case EARTHIAN -> "earthian.root_prison";
            case UNIVERSAL -> "universal.blood_fusion";
            case VOIDER -> "voider.void_send";
            default -> null;
        };
    }

    private String specialKey(BloodlineType type) {
        return switch (type) {
            case AQUA -> "aqua.tidal_surge";
            case SPARTAN -> "spartan.hell_dominion";
            case EARTHIAN -> "earthian.worldbreaker";
            case VOIDER -> "voider.void_flight";
            case UNIVERSAL -> "universal.ascension";
            default -> null;
        };
    }

    private void updateFlightAvailability(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        boolean ascended = universalAscensionUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
        if (ascended) {
            player.setAllowFlight(true);
        } else if (!voidFlightEndsAt.containsKey(player.getUniqueId())) {
            player.setAllowFlight(false);
        }
    }

    private void rechargeVoidSend(PlayerProfile profile) {
        long rechargeMillis = plugin.getConfig().getLong("bloodlines.voider.void-send.recharge-hours", 12L) * 60L * 60L * 1000L;
        if (profile.voidSendCharges() >= 2) {
            profile.setVoidSendLastRechargeAt(System.currentTimeMillis());
            return;
        }
        long now = System.currentTimeMillis();
        while (profile.voidSendCharges() < 2 && now - profile.voidSendLastRechargeAt() >= rechargeMillis) {
            profile.setVoidSendCharges(profile.voidSendCharges() + 1);
            profile.setVoidSendLastRechargeAt(profile.voidSendLastRechargeAt() + rechargeMillis);
        }
    }

    private void removeAllPassives(Player player) {
        for (Bloodline bloodline : bloodlines.values()) {
            bloodline.removePassive(player);
        }
    }

    private boolean canUseVoider(Player player) {
        return canUseVoiderName(player.getName());
    }

    private boolean canUseVoider(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return canUseVoiderName(name);
    }

    private boolean canUseVoiderName(String name) {
        return name != null && name.equalsIgnoreCase(allowedVoiderOwnerName());
    }

    private String allowedVoiderOwnerName() {
        return plugin.getConfig().getString("bloodlines.voider.owner-username", "W4Whiskers");
    }

    private BloodlineType rollEligibleInitialBloodline(Player player) {
        return canUseVoider(player)
                ? plugin.getPlayerDataManager().rollInitialBloodline()
                : plugin.getPlayerDataManager().rollNonVoiderBloodline();
    }

    private void runFirstJoinSelectionAnimation(Player player, BloodlineType selected) {
        List<BloodlineType> sequence = List.of(
                BloodlineType.AQUA,
                BloodlineType.EARTHIAN,
                BloodlineType.SPARTAN,
                BloodlineType.VOIDER,
                BloodlineType.AQUA,
                BloodlineType.SPARTAN,
                BloodlineType.EARTHIAN,
                BloodlineType.VOIDER,
                BloodlineType.AQUA,
                BloodlineType.EARTHIAN,
                selected
        );
        int[] delays = {0, 2, 4, 6, 9, 13, 18, 24, 31, 40, 52};

        for (int index = 0; index < sequence.size(); index++) {
            BloodlineType shown = sequence.get(index);
            int delay = delays[Math.min(index, delays.length - 1)];
            boolean finalReveal = index == sequence.size() - 1;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.showTitle(Title.title(
                        Component.text(shown.displayName(), shown.color()),
                        Component.text(finalReveal ? "Bloodline Chosen" : "Bloodline Rolling...", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(finalReveal ? 1100 : 250), Duration.ofMillis(100))
                ));
                player.playSound(player.getLocation(),
                        finalReveal ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.UI_BUTTON_CLICK,
                        SoundCategory.PLAYERS,
                        1F,
                        finalReveal ? 1.0F : 1.3F
                );
            }, delay);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Component message = Component.text(player.getName() + " awakened the " + selected.displayName() + " Bloodline.", selected.color());
            Bukkit.broadcast(message);
            player.sendActionBar(Component.text("Your bloodline is " + selected.displayName(), selected.color()));
            sendFirstJoinWebhook(player, selected);
        }, 62L);
    }

    private boolean allowInput(Player player, String channel) {
        long now = System.currentTimeMillis();
        UUID key = UUID.nameUUIDFromBytes((player.getUniqueId() + ":" + channel).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long last = inputDebounce.getOrDefault(key, 0L);
        if (now - last < 250L) {
            return false;
        }
        inputDebounce.put(key, now);
        return true;
    }

    private void clearCurse(PlayerProfile profile) {
        profile.setCursedUntil(0L);
        profile.setCursedBySpartan(null);
    }

    private void clearBloodlineState(PlayerProfile profile) {
        profile.cooldowns().clear();
        profile.setSpartanFlamingHandsUntil(0L);
        clearCurse(profile);
        aquaCurses.remove(profile.uuid());
        heldFireballs.remove(profile.uuid());
        HellDominionState dominion = spartanHellDominions.remove(profile.uuid());
        if (dominion != null) {
            Player online = Bukkit.getPlayer(profile.uuid());
            endHellDominion(online, dominion, online != null);
        }
        voidBlinkHits.remove(profile.uuid());
        universalBloodFusionUntil.remove(profile.uuid());
        universalRiftDashUntil.remove(profile.uuid());
        universalAscensionUntil.remove(profile.uuid());
        TidalSurgeState surge = tidalSurges.remove(profile.uuid());
        if (surge != null) {
            Player online = Bukkit.getPlayer(profile.uuid());
            endAquaTidalSurge(online, surge, online != null);
        }
        EarthianWorldbreakerState worldbreaker = earthianWorldbreakers.remove(profile.uuid());
        if (worldbreaker != null) {
            restoreTemporaryTerrain(worldbreaker);
        }
    }

    private void flushDirtyProfiles() {
        Set<UUID> snapshot = Set.copyOf(dirtyProfiles);
        for (UUID uuid : snapshot) {
            dirtyProfiles.remove(uuid);
            plugin.getPlayerDataManager().saveSync(plugin.getPlayerDataManager().getOrCreate(uuid));
        }
    }

    private boolean changedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ();
    }

    private BloodlineType resolveShardCraft(ItemStack[] matrix) {
        if (matrix == null || matrix.length == 0) {
            return null;
        }
        int upgradePotions = 0;
        BloodlineType traitType = null;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            String itemType = plugin.getCustomItems().getItemType(item);
            if (dev.zahen.bloodline.item.CustomItems.TYPE_UPGRADE_POTION.equals(itemType)) {
                upgradePotions += item.getAmount();
                continue;
            }
            if (dev.zahen.bloodline.item.CustomItems.TYPE_TRAIT_POTION.equals(itemType)) {
                BloodlineType foundType = plugin.getCustomItems().getBloodline(item);
                if (traitType != null || foundType == null) {
                    return null;
                }
                traitType = foundType;
                continue;
            }
            return null;
        }
        return upgradePotions == 5 && traitType != null ? traitType : null;
    }

    private boolean matchesUniversalShardRecipe(ItemStack[] matrix) {
        if (matrix == null || matrix.length < 9) {
            return false;
        }
        return isNetherStar(matrix[0])
                && plugin.getCustomItems().isBloodlineShard(matrix[1], BloodlineType.AQUA)
                && isNetherStar(matrix[2])
                && plugin.getCustomItems().isBloodlineShard(matrix[3], BloodlineType.EARTHIAN)
                && isExactMaterial(matrix[4], Material.DRAGON_EGG)
                && plugin.getCustomItems().isBloodlineShard(matrix[5], BloodlineType.VOIDER)
                && isNetherStar(matrix[6])
                && plugin.getCustomItems().isBloodlineShard(matrix[7], BloodlineType.SPARTAN)
                && isNetherStar(matrix[8]);
    }

    private boolean isNetherStar(ItemStack item) {
        return isExactMaterial(item, Material.NETHER_STAR);
    }

    private boolean isExactMaterial(ItemStack item, Material material) {
        return item != null && item.getAmount() == 1 && item.getType() == material;
    }

    private long earthianRegenDelayMillis(PlayerProfile profile) {
        long baseSeconds = plugin.getConfig().getLong("bloodlines.earthian.passive.regen-after-seconds", 20L);
        long reductionPerLevel = plugin.getConfig().getLong("bloodlines.earthian.passive.regen-after-seconds-reduction-per-level", 2L);
        long scaledSeconds = Math.max(8L, baseSeconds - Math.max(0, profile.activeLevel() - 1) * reductionPerLevel);
        return scaledSeconds * 1000L;
    }

    private boolean isOnSolidGround(Player player) {
        return player.getLocation().clone().add(0.0D, -0.1D, 0.0D).getBlock().getType().isSolid();
    }

    private void sendFirstJoinWebhook(Player player, BloodlineType type) {
        if (!plugin.getConfig().getBoolean("discord-webhook.enabled", false)) {
            return;
        }
        String url = plugin.getConfig().getString("discord-webhook.url", "").trim();
        if (url.isBlank()) {
            return;
        }

        String payload = "{\"content\":\"" + escapeJson(player.getName() + " awakened the " + type.displayName() + " Bloodline.") + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + throwable.getMessage());
                    return null;
                });
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Location findSafeEndstoneLocation(World end, int centerX, int centerZ) {
        for (int offset = 0; offset <= 8; offset++) {
            for (int x = centerX - offset; x <= centerX + offset; x++) {
                for (int z = centerZ - offset; z <= centerZ + offset; z++) {
                    int highest = end.getHighestBlockYAt(x, z);
                    if (highest <= end.getMinHeight()) {
                        continue;
                    }

                    Location feet = new Location(end, x + 0.5D, highest + 1D, z + 0.5D);
                    Material floor = feet.clone().add(0D, -1D, 0D).getBlock().getType();
                    Material feetType = feet.getBlock().getType();
                    Material headType = feet.clone().add(0D, 1D, 0D).getBlock().getType();
                    if (floor == Material.END_STONE && feetType.isAir() && headType.isAir()) {
                        return feet;
                    }
                }
            }
        }
        return null;
    }

    private Location findSafeNetherLocation(World nether, Location origin, float yaw, float pitch) {
        int centerX = origin.getBlockX() / 8;
        int centerZ = origin.getBlockZ() / 8;
        int preferredY = plugin.getConfig().getInt("bloodlines.spartan.hell-dominion.nether-send-preferred-y", 80);
        for (int offset = 0; offset <= 10; offset++) {
            for (int x = centerX - offset; x <= centerX + offset; x++) {
                for (int z = centerZ - offset; z <= centerZ + offset; z++) {
                    for (int y = Math.min(nether.getMaxHeight() - 2, preferredY + 20); y >= Math.max(nether.getMinHeight() + 1, preferredY - 20); y--) {
                        Location feet = new Location(nether, x + 0.5D, y, z + 0.5D, yaw, pitch);
                        Material floor = feet.clone().add(0D, -1D, 0D).getBlock().getType();
                        Material feetType = feet.getBlock().getType();
                        Material headType = feet.clone().add(0D, 1D, 0D).getBlock().getType();
                        if (floor.isSolid() && floor != Material.LAVA && floor != Material.FIRE && floor != Material.SOUL_FIRE
                                && feetType.isAir() && headType.isAir()) {
                            return feet;
                        }
                    }
                }
            }
        }
        return nether.getSpawnLocation().clone().add(0.5D, 1D, 0.5D);
    }

    private Location findSafeBlinkDestination(Player player, double maxDistance) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location base = player.getLocation().clone();
        Location best = null;
        for (double distance = 0.75D; distance <= maxDistance; distance += 0.5D) {
            Location candidate = base.clone().add(direction.clone().multiply(distance));
            candidate.setYaw(base.getYaw());
            candidate.setPitch(base.getPitch());
            if (!isSafeBlinkLocation(candidate)) {
                break;
            }
            best = candidate;
        }
        return best;
    }

    private boolean isSafeBlinkLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);
        return feet.isPassable() && head.isPassable() && below.getType().isSolid();
    }

    private void startAquaEntityCurse(Player caster, LivingEntity target, int level, long totalTicks) {
        int slownessAmplifier = Math.min(
                plugin.getConfig().getInt("bloodlines.aqua.suffocation-curse.slowness-amplifier-at-level-5", 2),
                level >= 5 ? 2 : Math.max(0, (level - 1) / 2)
        );
        final int[] remainingSeconds = {(int) Math.max(1L, totalTicks / 20L)};
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) totalTicks, slownessAmplifier, true, true, true));
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!target.isValid() || target.isDead()) {
                task.cancel();
                return;
            }
            if (target.isInWater() || remainingSeconds[0]-- <= 0) {
                task.cancel();
                return;
            }
            double curseDamage = plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.damage-per-second", 1.0D)
                    + Math.max(0, level - 1) * plugin.getConfig().getDouble("bloodlines.aqua.suffocation-curse.damage-per-second-per-level", 0.35D);
            target.damage(curseDamage, caster);
            target.getWorld().spawnParticle(Particle.BUBBLE, target.getLocation().add(0, 1, 0), 8, 0.2, 0.35, 0.2, 0.03);
        }, 20L, 20L);
    }

    private void removeVoidFlightElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (plugin.getCustomItems().isVoidFlightElytra(chestplate)) {
            player.getInventory().setChestplate(null);
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (plugin.getCustomItems().isVoidFlightElytra(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private void restoreChestplateAfterVoidFlight(Player player, ItemStack stored) {
        ItemStack current = player.getInventory().getChestplate();
        boolean currentIsTemporary = plugin.getCustomItems().isVoidFlightElytra(current);
        if (currentIsTemporary || current == null) {
            player.getInventory().setChestplate(stored);
            return;
        }
        if (stored == null) {
            return;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stored);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private List<TemporaryWaterBlock> createTemporaryWaterArea(Location center, double radius) {
        List<TemporaryWaterBlock> changed = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            return changed;
        }

        int blockRadius = Math.max(1, (int) Math.floor(radius));
        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int z = -blockRadius; z <= blockRadius; z++) {
                if ((x * x) + (z * z) > radius * radius) {
                    continue;
                }
                Block block = center.clone().add(x, 0, z).getBlock();
                if (!block.getType().isAir()) {
                    continue;
                }
                Block below = block.getRelative(0, -1, 0);
                if (!below.getType().isSolid()) {
                    continue;
                }
                changed.add(new TemporaryWaterBlock(block.getLocation(), block.getBlockData().clone()));
                block.setType(Material.WATER, false);
            }
        }
        return changed;
    }

    private void restoreTemporaryWater(TidalSurgeState surge) {
        for (TemporaryWaterBlock changed : surge.changedBlocks()) {
            Block block = changed.location().getBlock();
            if (block.getType() == Material.WATER) {
                block.setBlockData(changed.originalData(), false);
            }
        }
    }

    private void endAquaTidalSurge(Player player, TidalSurgeState surge, boolean teleportBack) {
        restoreTemporaryWater(surge);
        if (player != null && teleportBack && surge.teleported() && surge.returnLocation() != null) {
            player.teleport(surge.returnLocation());
        }
    }

    private void endHellDominion(Player player, HellDominionState dominion, boolean teleportBack) {
        if (player != null) {
            if (teleportBack && dominion.teleported() && dominion.returnLocation() != null) {
                player.teleport(dominion.returnLocation());
            }
            player.setFireTicks(0);
        }
    }

    private List<TemporaryTerrainBlock> createTemporaryRoughTerrain(Location center, double radius) {
        List<TemporaryTerrainBlock> changed = new ArrayList<>();
        int blockRadius = Math.max(1, (int) Math.floor(radius));
        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int z = -blockRadius; z <= blockRadius; z++) {
                if ((x * x) + (z * z) > radius * radius) {
                    continue;
                }
                Block block = center.clone().add(x, -1, z).getBlock();
                if (!block.getType().isSolid()) {
                    continue;
                }
                Material replacement = ((Math.abs(x) + Math.abs(z)) % 2 == 0) ? Material.COARSE_DIRT : Material.COBBLESTONE;
                changed.add(new TemporaryTerrainBlock(block.getLocation(), block.getBlockData().clone()));
                block.setType(replacement, false);
            }
        }
        return changed;
    }

    private void restoreTemporaryTerrain(EarthianWorldbreakerState state) {
        for (TemporaryTerrainBlock changed : state.terrain()) {
            changed.location().getBlock().setBlockData(changed.originalData(), false);
        }
    }

    private record GroundSlamState(long armedAt, boolean leftGround, boolean universal, double startY) {
        private GroundSlamState withLeftGround(boolean value) {
            return new GroundSlamState(armedAt, value, universal, startY);
        }
    }

    private record AquaCurseState(UUID caster, long endsAt) {
    }

    private record TemporaryWaterBlock(Location location, BlockData originalData) {
    }

    private record TidalSurgeState(long endsAt, List<TemporaryWaterBlock> changedBlocks, Location returnLocation, boolean teleported) {
    }

    private record HeldFireballState(long expiresAt) {
    }

    private record HellDominionState(long endsAt, Location returnLocation, boolean teleported) {
    }

    private record TemporaryTerrainBlock(Location location, BlockData originalData) {
    }

    private record EarthianWorldbreakerState(long endsAt, Location center, double radius, List<TemporaryTerrainBlock> terrain) {
    }
}
