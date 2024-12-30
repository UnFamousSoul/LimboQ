package com.unfamoussoul.limboQ.handlers;

import com.unfamoussoul.limboQ.LimboQ;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

public class QueueHandler implements LimboSessionHandler {
    private final LimboQ plugin;
    private LimboPlayer player;

    public QueueHandler(LimboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.player.disableFalling();
        plugin.queuedPlayers.add(player);
    }

    @Override
    public void onDisconnect() {
        plugin.queuedPlayers.remove(player);
    }
}
