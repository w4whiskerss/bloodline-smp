package dev.zahen.bloodline.listener;

import dev.zahen.bloodline.BloodlinePlugin;
import dev.zahen.bloodline.gui.BloodlineGui;
import dev.zahen.bloodline.item.CustomItems;
import dev.zahen.bloodline.manager.BloodlineManager;
import dev.zahen.bloodline.model.BloodlineType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BloodlineListener implements Listener {

    private final BloodlinePlugin plugin;
    private final BloodlineManager manager;

    public BloodlineListener(BloodlinePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBloodlineManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        manager.pushClientState(event.getPlayer(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        manager.handleMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        manager.handleSneakToggle(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (manager.isBloodlineGameplayDisabled(player)) {
            return;
        }
        ItemStack item = event.getItem();
        Action action = event.getAction();
        String itemType = plugin.getCustomItems().getItemType(item);
        if (CustomItems.TYPE_UNIVERSAL_CORE.equals(itemType)) {
            event.setCancelled(true);
            manager.unlockUniversal(player);
            if (item != null) {
                item.setAmount(item.getAmount() - 1);
            }
            return;
        }

        if (item != null && item.getType() == Material.GLASS_BOTTLE
                && player.isSneaking()
                && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            if (manager.withdrawBloodlineToBottle(player, item)) {
                event.setCancelled(true);
            }
            return;
        }

        if (!player.isSneaking() && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            if (manager.tryLaunchHeldSpartanFireball(player)) {
                event.setCancelled(true);
                return;
            }
        }

        if (manager.usesClientHotkeys(player)) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            manager.triggerPrimary(player);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            manager.triggerSecondary(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (manager.isBloodlineGameplayDisabled(player)) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !player.isSneaking() || manager.usesClientHotkeys(player)) {
            return;
        }
        event.setCancelled(true);
        manager.triggerPrimary(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (manager.isBloodlineGameplayDisabled(player)) {
            return;
        }
        if (!player.isSneaking() || manager.usesClientHotkeys(player)) {
            return;
        }
        BloodlineType active = manager.profile(player).activeBloodline();
        if (!manager.getBloodline(active).supportsSpecialAbility()) {
            return;
        }
        event.setCancelled(true);
        manager.triggerSpecial(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker != null && manager.isBloodlineGameplayDisabled(attacker)) {
            return;
        }
        if (plugin.getGracePeriodManager().isGraceActive() && attacker != null && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            attacker.sendActionBar(net.kyori.adventure.text.Component.text("PvP is disabled during grace period.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        if (attacker != null && attacker.isSneaking() && !manager.usesClientHotkeys(attacker)) {
            event.setCancelled(true);
            manager.triggerSecondary(attacker);
            return;
        }

        if (event.getEntity() instanceof Player damagedPlayer) {
            manager.handleDamageTaken(damagedPlayer, event.getDamage(), event);
        }
        if (attacker != null) {
            manager.handleMeleeHit(attacker, event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onGenericDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.handleDamageTaken(player, event.getDamage(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        manager.handleVelocity(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        manager.handleDeath(event.getPlayer(), event.getPlayer().getKiller(), event, wasKilledByOmniBlade(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (manager.isBloodlineGameplayDisabled(event.getPlayer())) {
            return;
        }
        ItemStack item = event.getItem();
        String itemType = plugin.getCustomItems().getItemType(item);
        if (CustomItems.TYPE_TRAIT_POTION.equals(itemType)) {
            BloodlineType bloodline = plugin.getCustomItems().getBloodline(item);
            if (bloodline != null) {
                if (!manager.applyTraitPotion(event.getPlayer(), bloodline, plugin.getCustomItems().getStoredLevel(item))) {
                    event.setCancelled(true);
                }
            }
        } else if (CustomItems.TYPE_UPGRADE_POTION.equals(itemType)) {
            manager.applyUpgradePotion(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (plugin.getAdminPanelGui().isListTitle(title) || plugin.getAdminPanelGui().isEditTitle(title) || plugin.getAdminPanelGui().isDebugTitle(title)) {
            event.setCancelled(true);
            plugin.getAdminPanelGui().handleClick(player, event.getClickedInventory(), event.getSlot());
            return;
        }
        if (plugin.getTestItemsGui().isTitle(title)) {
            event.setCancelled(true);
            plugin.getTestItemsGui().handleClick(player, event.getSlot());
            return;
        }
        if (!title.equals(BloodlineGui.TITLE)) {
            return;
        }
        if (manager.isBloodlineGameplayDisabled(player)) {
            return;
        }
        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        BloodlineType type = switch (current.getType()) {
            case HEART_OF_THE_SEA -> BloodlineType.AQUA;
            case NETHERITE_SWORD -> BloodlineType.SPARTAN;
            case MOSS_BLOCK -> BloodlineType.EARTHIAN;
            case ENDER_EYE -> BloodlineType.VOIDER;
            case NETHER_STAR -> BloodlineType.UNIVERSAL;
            default -> null;
        };
        if (type != null) {
            manager.switchActiveBloodline(player, type);
            plugin.getBloodlineGui().open(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (plugin.getAdminPanelGui().isListTitle(title)
                || plugin.getAdminPanelGui().isEditTitle(title)
                || plugin.getAdminPanelGui().isDebugTitle(title)
                || plugin.getTestItemsGui().isTitle(title)
                || title.equals(BloodlineGui.TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player player && manager.isBloodlineGameplayDisabled(player)) {
            event.getInventory().setResult(null);
            return;
        }
        ItemStack result = manager.resolveCustomCraftResult(event.getInventory().getMatrix());
        event.getInventory().setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        ItemStack expected = manager.resolveCustomCraftResult(event.getInventory().getMatrix());
        ItemStack current = event.getCurrentItem();
        if (current != null && plugin.getCustomItems().getItemType(current) != null) {
            if (expected == null || !sameCustomItem(current, expected)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean sameCustomItem(ItemStack left, ItemStack right) {
        String leftType = plugin.getCustomItems().getItemType(left);
        String rightType = plugin.getCustomItems().getItemType(right);
        if (leftType == null || rightType == null || !leftType.equals(rightType)) {
            return false;
        }
        return plugin.getCustomItems().getBloodline(left) == plugin.getCustomItems().getBloodline(right)
                && plugin.getCustomItems().getStoredLevel(left) == plugin.getCustomItems().getStoredLevel(right);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean wasKilledByOmniBlade(Player victim) {
        if (!(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent)) {
            return false;
        }
        Player attacker = resolveAttacker(damageEvent);
        return attacker != null && plugin.getCustomItems().isOmniBlade(attacker.getInventory().getItemInMainHand());
    }
}
