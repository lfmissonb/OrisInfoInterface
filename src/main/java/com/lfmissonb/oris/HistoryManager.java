package com.lfmissonb.oris;

import com.google.gson.JsonObject;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

public class HistoryManager {
    final Logger logger;
    final OrisInfoInterface plugin;

    final boolean playerHistoryEnabled;
    final int playerHistoryRetentionDays;

    final boolean tpsHistoryEnabled;
    final int tpsHistoryRetentionDays;
    final int tpsRefreshDelay;

    final boolean msptHistoryEnabled;
    final int msptHistoryRetentionDays;
    final int msptRefreshDelay;

    PlayerListener playerListener;
    int lastPlayerNum = -1;
    private Connection connection;

    public HistoryManager(OrisInfoInterface plugin) {
        logger = plugin.logger;
        this.plugin = plugin;

        playerHistoryEnabled = plugin.getConfig().getBoolean("player.enabled") && plugin.getConfig().getBoolean("player.history.enabled");
        playerHistoryRetentionDays = plugin.getConfig().getInt("player.history.retention-days");

        tpsHistoryEnabled = plugin.getConfig().getBoolean("tps.enabled") && plugin.getConfig().getBoolean("tps.history.enabled");
        tpsHistoryRetentionDays = plugin.getConfig().getInt("tps.history.retention-days");
        tpsRefreshDelay = plugin.getConfig().getInt("tps.history.refresh-delay");

        msptHistoryEnabled = plugin.getConfig().getBoolean("mspt.enabled") && plugin.getConfig().getBoolean("mspt.history.enabled");
        msptHistoryRetentionDays = plugin.getConfig().getInt("mspt.history.retention-days");
        msptRefreshDelay = plugin.getConfig().getInt("mspt.history.refresh-delay");

        if (!playerHistoryEnabled && !tpsHistoryEnabled && !msptHistoryEnabled) return;

        initializeDatabase();

        if (playerHistoryEnabled) {
            createTable("player", "INTEGER");
            playerListener = new PlayerListener(this);
            plugin.getServer().getPluginManager().registerEvents(playerListener, plugin);
        }

        if (tpsHistoryEnabled) {
            createTable("tps", "FLOAT");

            new BukkitRunnable() {
                @Override
                public void run() {
                    updateTPS();
                }
            }.runTaskTimer(plugin, 0, tpsRefreshDelay * 20L);
        }

        if (msptHistoryEnabled) {
            createTable("mspt", "FLOAT");


            new BukkitRunnable() {
                @Override
                public void run() {
                    updateMSPT();
                }
            }.runTaskTimer(plugin, 0, msptRefreshDelay * 20L);
        }
    }

    private void initializeDatabase() {
        plugin.logDebug("Initializing database...");

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) if (!dataFolder.mkdirs()) throw new RuntimeException("Cannot create data folder");


        File dbFile = new File(dataFolder, "database.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            logger.severe("Cannot create database.");
            throw new RuntimeException(e);
        }

        plugin.logDebug("Database initialized.");
    }

    private void createTable(String tableName, String valueType) {
        plugin.logDebug(String.format("Initializing table %s...", tableName));

        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "count %s NOT NULL)", tableName, valueType));
            statement.executeUpdate(String.format("CREATE INDEX IF NOT EXISTS idx_time ON %s (time)", tableName));
        } catch (SQLException e) {
            logger.severe(String.format("Cannot create table %s.", tableName));
            throw new RuntimeException(e);
        }

        plugin.logDebug(String.format("Table %s initialized.", tableName));
    }

    public void stop() {
        plugin.logDebug("Closing database...");

        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            logger.severe("Cannot close database.");
            throw new RuntimeException(e);
        }

        plugin.logDebug("Database closed.");
    }

    void updatePlayerCount() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int currentNum = plugin.getPlayerNum();

            if (currentNum != lastPlayerNum) {
                recordCount("player", playerHistoryRetentionDays, currentNum);
                lastPlayerNum = currentNum;
            }
        });
    }

    void updateTPS() {
        recordCount("tps", tpsHistoryRetentionDays, plugin.getTPS());
    }

    void updateMSPT() {
        recordCount("mspt", msptHistoryRetentionDays, plugin.getMSPT());
    }

    private void recordCount(String tableName, int retentionDays, Object count) {
        plugin.logDebug(String.format("Record data to %s: %s", tableName, count.toString()));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = String.format("INSERT INTO %s (count) VALUES (?)", tableName);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, count);
                statement.executeUpdate();

                if (retentionDays > 0) {
                    cleanOldData(tableName, retentionDays);
                }
            } catch (SQLException e) {
                logger.severe(String.format("Cannot insert record to %s.", tableName));
                throw new RuntimeException(e);
            }
        });
    }

    private void cleanOldData(String tableName, int retentionDays) {
        String sql = String.format("DELETE FROM %s WHERE time < datetime('now', ?)", tableName);
        String interval = "-" + retentionDays + " days";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, interval);
            int deleted = statement.executeUpdate();

            if (deleted > 0) {
                plugin.logDebug(String.format("Cleaned %d records from %s table", deleted, tableName));
            }
        } catch (SQLException e) {
            logger.severe(String.format("Cannot clean record from %s: %s", tableName, e.getMessage()));
        }
    }

    private JsonObject getRecords(String tableName, int retentionDays) {
        JsonObject json = new JsonObject();
        String sql;

        if (retentionDays > 0) {
            sql = String.format("SELECT strftime('%%s', time) AS timestamp, count FROM %s WHERE time >= datetime('now', ?)", tableName);
        } else {
            sql = String.format("SELECT strftime('%%s', time) AS timestamp, count FROM %s", tableName);
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // 设置查询参数（如果适用）
            if (retentionDays > 0) {
                statement.setString(1, "-" + retentionDays + " days");
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String timestamp = resultSet.getString("timestamp");
                    Number count = (Number) resultSet.getObject("count");

                    json.addProperty(timestamp, count);
                }
            }
        } catch (SQLException e) {
            logger.severe(String.format("Cannot get record from %s: %s", tableName, e.getMessage()));
        }

        return json;
    }

    public JsonObject getPlayerRecords() {
        return getRecords("player", playerHistoryRetentionDays);
    }

    public JsonObject getTPSRecords() {
        return getRecords("tps", tpsHistoryRetentionDays);
    }

    public JsonObject getMSPTRecords() {
        return getRecords("mspt", msptHistoryRetentionDays);
    }
}
