package com.unfamoussoul.limboQ.listeners;

import com.unfamoussoul.limboQ.LimboQ;
import com.unfamoussoul.limboQ.entities.ServerStatus;
import com.unfamoussoul.limboQ.entities.Settings;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.TextComponent;

public record QueueListener(LimboQ plugin) {

    @Subscribe()
    public void onLogin(LoginLimboRegisterEvent event) {
        if (!Settings.IMP.MAIN.QUEUE_ON_LOGIN) return;

        plugin.refreshStatus();
        if (plugin.getServerStatus() == ServerStatus.NORMAL) return;

        Player player = event.getPlayer();
        event.addOnJoinCallback(() -> plugin.queuePlayer(player));
    }

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
        if (!Settings.IMP.MAIN.ENABLE_KICK_MESSAGE) return;

        event.setOnKickCallback(kickEvent -> {
            if (!kickEvent.getServer().equals(plugin.getTargetServer()) || kickEvent.getServerKickReason().isEmpty()) return false;

            String reason = ((TextComponent) kickEvent.getServerKickReason().get()).content();
            if (!reason.contains(Settings.IMP.MAIN.KICK_MESSAGE)) return false;

            plugin.queuePlayer(kickEvent.getPlayer());
            return true;
        });
    }
}
