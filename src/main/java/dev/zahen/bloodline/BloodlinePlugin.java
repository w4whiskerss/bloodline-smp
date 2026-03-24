package dev.zahen.bloodline;

import dev.zahen.bloodline.command.AbilityCommand;
import dev.zahen.bloodline.command.BloodlineAdminCommand;
import dev.zahen.bloodline.command.BloodlineCommand;
import dev.zahen.bloodline.command.BloodlineModCommand;
import dev.zahen.bloodline.command.BloodlineReloadCommand;
import dev.zahen.bloodline.command.BloodlineTestCommand;
import dev.zahen.bloodline.command.GraceCommand;
import dev.zahen.bloodline.data.PlayerDataManager;
import dev.zahen.bloodline.gui.AdminPanelGui;
import dev.zahen.bloodline.gui.BloodlineGui;
import dev.zahen.bloodline.item.CustomItems;
import dev.zahen.bloodline.listener.BloodlineListener;
import dev.zahen.bloodline.manager.BloodlineManager;
import dev.zahen.bloodline.manager.GracePeriodManager;
import dev.zahen.bloodline.world.WaterWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BloodlinePlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private BloodlineManager bloodlineManager;
    private GracePeriodManager gracePeriodManager;
    private BloodlineGui bloodlineGui;
    private AdminPanelGui adminPanelGui;
    private CustomItems customItems;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeSpecialWorlds();

        this.customItems = new CustomItems(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.bloodlineGui = new BloodlineGui(this);
        this.adminPanelGui = new AdminPanelGui(this);
        this.gracePeriodManager = new GracePeriodManager(this);
        this.bloodlineManager = new BloodlineManager(this);

        BloodlineListener listener = new BloodlineListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand command = getCommand("bloodline");
        if (command != null) {
            BloodlineCommand executor = new BloodlineCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        GraceCommand graceCommand = new GraceCommand(this);
        PluginCommand graceStart = getCommand("gracestart");
        if (graceStart != null) {
            graceStart.setExecutor(graceCommand);
            graceStart.setTabCompleter(graceCommand);
        }
        PluginCommand graceStop = getCommand("gracestop");
        if (graceStop != null) {
            graceStop.setExecutor(graceCommand);
            graceStop.setTabCompleter(graceCommand);
        }
        PluginCommand graceTimeSet = getCommand("gracetimeset");
        if (graceTimeSet != null) {
            graceTimeSet.setExecutor(graceCommand);
            graceTimeSet.setTabCompleter(graceCommand);
        }
        PluginCommand testCommand = getCommand("bloodlinetest");
        if (testCommand != null) {
            BloodlineTestCommand executor = new BloodlineTestCommand(this);
            testCommand.setExecutor(executor);
            testCommand.setTabCompleter(executor);
        }
        PluginCommand adminCommand = getCommand("bloodlineadmin");
        if (adminCommand != null) {
            BloodlineAdminCommand executor = new BloodlineAdminCommand(this);
            adminCommand.setExecutor(executor);
            adminCommand.setTabCompleter(executor);
        }
        PluginCommand reloadCommand = getCommand("bloodlinereload");
        if (reloadCommand != null) {
            BloodlineReloadCommand executor = new BloodlineReloadCommand(this);
            reloadCommand.setExecutor(executor);
            reloadCommand.setTabCompleter(executor);
        }
        PluginCommand modCommand = getCommand("bloodlinemod");
        if (modCommand != null) {
            BloodlineModCommand executor = new BloodlineModCommand(this);
            modCommand.setExecutor(executor);
            modCommand.setTabCompleter(executor);
        }
        AbilityCommand abilityCommand = new AbilityCommand(this);
        for (String commandName : java.util.List.of("ability1", "ability2", "ability3", "primary", "secondary", "special")) {
            PluginCommand ability = getCommand(commandName);
            if (ability != null) {
                ability.setExecutor(abilityCommand);
                ability.setTabCompleter(abilityCommand);
            }
        }

        bloodlineManager.startSchedulers();
        bloodlineManager.registerRecipes();
        gracePeriodManager.start();
    }

    @Override
    public void onDisable() {
        if (bloodlineManager != null) {
            bloodlineManager.shutdown();
        }
        if (gracePeriodManager != null) {
            gracePeriodManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BloodlineManager getBloodlineManager() {
        return bloodlineManager;
    }

    public BloodlineGui getBloodlineGui() {
        return bloodlineGui;
    }

    public AdminPanelGui getAdminPanelGui() {
        return adminPanelGui;
    }

    public GracePeriodManager getGracePeriodManager() {
        return gracePeriodManager;
    }

    public CustomItems getCustomItems() {
        return customItems;
    }

    public void initializeSpecialWorlds() {
        if (!getConfig().getBoolean("bloodlines.aqua.tidal-surge.teleport-water-world.enabled", false)) {
        } else {
            String worldName = getConfig().getString("bloodlines.aqua.tidal-surge.teleport-water-world.world-name", "bloodline_aqua_realm");
            if (worldName != null && !worldName.isBlank()) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    world = new WorldCreator(worldName).generator(new WaterWorldGenerator()).environment(World.Environment.NORMAL).createWorld();
                }
                if (world != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey().asString() + " run gamerule doWeatherCycle false");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey().asString() + " run gamerule doDaylightCycle false");
                    world.setStorm(false);
                    world.setTime(6000L);
                    world.setSpawnLocation(0, getConfig().getInt("bloodlines.aqua.tidal-surge.teleport-water-world.spawn-y", 65), 0);
                }
            }
        }
        if (getConfig().getBoolean("bloodlines.spartan.hell-dominion.teleport-fire-world.enabled", false)) {
            String worldName = getConfig().getString("bloodlines.spartan.hell-dominion.teleport-fire-world.world-name", "bloodline_spartan_realm");
            if (worldName != null && !worldName.isBlank()) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    world = new WorldCreator(worldName).environment(World.Environment.NETHER).createWorld();
                }
                if (world != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey().asString() + " run gamerule doWeatherCycle false");
                    world.setStorm(false);
                    world.setTime(18000L);
                    world.setSpawnLocation(0, getConfig().getInt("bloodlines.spartan.hell-dominion.teleport-fire-world.spawn-y", 80), 0);
                }
            }
        }
    }
}
