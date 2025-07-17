package com.lfmissonb.oris;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public final class OrisInfoInterface extends JavaPlugin {
    Logger logger;

    InterfaceManager interfaceManager;
    HistoryManager historyManager;

    private Object minecraftServer;
    private Field tpsField;

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger = getLogger();
        logger.info("--------------------------------------------------");
        logger.info("   ___       _       ___        __");
        logger.info("  / _ \\ _ __(_)___  |_ _|_ __  / _| ___     ");
        logger.info(" | | | | '__| / __|  | || '_ \\| |_ / _ \\    ");
        logger.info(" | |_| | |  | \\__ \\  | || | | |  _| (_) |   ");
        logger.info("  \\___/|_|  |_|___/ |___|_|_|_|_|  \\___/    ");
        logger.info(" |_ _|_ __ | |_ ___ _ __ / _| __ _  ___ ___ ");
        logger.info("  | || '_ \\| __/ _ \\ '__| |_ / _` |/ __/ _ \\");
        logger.info("  | || | | | ||  __/ |  |  _| (_| | (_|  __/");
        logger.info(" |___|_| |_|\\__\\___|_|  |_|  \\__,_|\\___\\___|");
        logger.info(String.format("Oris Info Interface v%s", getDescription().getVersion()));
        logger.info("--------------------------------------------------");

        // config
        saveDefaultConfig();
        if (!getConfig().getBoolean("plugin-enabled")) {
            logger.warning("Oris Info Interface has been disabled in config.yml. Is this your first time running the plugin? Please set plugin-enabled to true in config.yml to enable the plugin.");
            return;
        }

        // get MinecraftServer - prepared for TPS
        logDebug("Getting MinecraftServer...");
        Server server = getServer();

        if (getConfig().getBoolean("mspt.enabled")) {
            try {
                server.getClass().getMethod("getAverageTickTime");
            } catch (NoSuchMethodException e) {
                getConfig().set("mspt.enabled", false);
                logger.warning(String.format("Your server doesn't support getAverageTickTime method, mspt has been disabled: %s", e.getMessage()));
            }
        }

        Method getServerMethod;
        try {
            getServerMethod = server.getClass().getMethod("getServer");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try {
            minecraftServer = getServerMethod.invoke(server);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (minecraftServer == null) {
            throw new RuntimeException("MinecraftServer is null");
        }
        Class<?> serverClass = minecraftServer.getClass().getSuperclass();

        try {
            tpsField = serverClass.getDeclaredField("recentTps");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        tpsField.setAccessible(true);

        // initialize modules
        logDebug("Initializing InterfaceManager...");
        interfaceManager = new InterfaceManager(this);
        logDebug("Initializing HistoryManager...");
        historyManager = new HistoryManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (!getConfig().getBoolean("plugin-enabled")) return;
        if (interfaceManager != null) interfaceManager.stop();
        if (historyManager != null) historyManager.stop();
        logger.info("Oris Info Interface disabled.");
    }

    public double getTPS() {
        try {
            return (double) Math.round(((double[]) tpsField.get(minecraftServer))[0] * 100) / 100;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // return (double) Math.round(getServer().getTPS()[0] * 100) / 100;
    }

    public double getMSPT() {
        return (double) Math.round(getServer().getAverageTickTime() * 100) / 100;
    }

    public int getPlayerNum() {
        return getServer().getOnlinePlayers().size();
    }

    public void logDebug(String msg) {
        if (getConfig().getBoolean("debug-mode")) {
            logger.info(String.format("[DEBUG] %s", msg));
        }
    }
}
