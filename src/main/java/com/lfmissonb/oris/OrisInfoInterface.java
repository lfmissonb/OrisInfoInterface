package com.lfmissonb.oris;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class OrisInfoInterface extends JavaPlugin {
    Logger logger;

    InterfaceManager interfaceManager;
    HistoryManager historyManager;

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
        logger.info(String.format("Oris Info Interface v%s", getPluginMeta().getVersion()));
        logger.info("--------------------------------------------------");

        saveDefaultConfig();
        if (!getConfig().getBoolean("plugin-enabled")) {
            logger.warning("Oris Info Interface has been disabled in config.yml. Is this your first time running the plugin? Please set plugin-enabled to true in config.yml to enable the plugin.");
            return;
        }

        interfaceManager = new InterfaceManager(this);
        historyManager = new HistoryManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (!getConfig().getBoolean("plugin-enabled")) return;
        interfaceManager.stop();
        historyManager.stop();
        logger.info("Oris Info Interface disabled.");
    }

    public double getTPS() {
        return (double) Math.round(getServer().getTPS()[0] * 100) / 100;
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
