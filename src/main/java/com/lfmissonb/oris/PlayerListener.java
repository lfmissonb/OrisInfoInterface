package com.lfmissonb.oris;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final HistoryManager historyManager;

    public PlayerListener(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        historyManager.updatePlayerCount();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        historyManager.updatePlayerCount();
    }
}
