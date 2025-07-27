package com.unfamoussoul.limboQ;

import com.google.inject.Inject;
import com.unfamoussoul.limboQ.commands.Reload;
import com.unfamoussoul.limboQ.config.Config;
import com.unfamoussoul.limboQ.entities.ServerStatus;
import com.unfamoussoul.limboQ.handlers.QueueHandler;
import com.unfamoussoul.limboQ.config.ConfigHandler;
import com.unfamoussoul.limboQ.listeners.QueueListener;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(
        id = "limboq",
        name = "LimboQ",
        version = "1.0",
        description = "Плагин для очередей и задержки людей в Лимбо перед заходом на сервер!",
        url = "https://github.com/UnFamousSoul/LimboQ",
        authors = {"UnFamousSoul"},
        dependencies = {@Dependency(id = "limboapi")}
)
public class LimboQ {

    private final Logger logger;
    private final ProxyServer server;
    private final LimboFactory factory;
    private final Path directory;

    public Config config;
    public LinkedList<LimboPlayer> queuedPlayers = new LinkedList<>();

    private RegisteredServer targetServer;
    private ServerStatus serverStatus;
    private Limbo queueServer;
    private ScheduledTask queueTask;
    private ScheduledTask pingTask;

    @Inject
    public LimboQ(Logger _logger, ProxyServer _server, @DataDirectory Path _directory) {
        logger = _logger;
        server = _server;
        factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
        directory = _directory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        reload();
    }

    public void reload() {
        config = new ConfigHandler(directory, logger).config;
        
        queueServer = createQueueServer();
        server.getEventManager().register(this, new QueueListener(this));

        {
            CommandManager manager = server.getCommandManager();
            manager.unregister("limboq");
            CommandMeta commandMeta = manager.metaBuilder("limboq")
                    .aliases("lq", "queue")
                    .plugin(this)
                    .build();
            manager.register(commandMeta, new Reload(this));
        }

        Optional<RegisteredServer> server = getServer().getServer(config.MAIN_SERVER);
        server.ifPresentOrElse(registeredServer -> {
            targetServer = registeredServer;
            startPingTask();
            startQueueTask();
        }, () -> logger.error("Server {} doesn't exists!", config.MAIN_SERVER));
    }

    private Limbo createQueueServer() {
        return factory.createLimbo(
                        factory.createVirtualWorld(
                                Dimension.valueOf(config.WORLD_DIMENSION),
                                config.WORLD_X,
                                config.WORLD_Y,
                                config.WORLD_Z,
                                config.WORLD_YAW,
                                config.WORLD_PITCH)
                )
                .setName(config.WORLD_NAME)
                .setWorldTime(config.WORLD_TIME)
                .setGameMode(GameMode.valueOf(config.WORLD_GAMEMODE))
                .setViewDistance(config.WORLD_VIEW_DISTANCE)
                .setSimulationDistance(config.WORLD_SIMULATION_DISTANCE);
    }

    public void queuePlayer(Player player) {
        queueServer.spawnPlayer(player, new QueueHandler(this));
    }

    public ProxyServer getServer() {
        return server;
    }

    public RegisteredServer getTargetServer() {
        return targetServer;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public void refreshStatus() {
        ping();
    }

    private void startQueueTask() {
        if (queueTask != null) queueTask.cancel();
        queueTask = getServer().getScheduler().buildTask(this, this::queue).repeat(config.MAIN_CHECK_INTERVAL, TimeUnit.MILLISECONDS).schedule();
    }

    private void queue() {
        if (serverStatus == null) return;
        switch (serverStatus) {
            case NORMAL -> {
                if (queuedPlayers.isEmpty()) return;

                LimboPlayer player = queuedPlayers.getFirst();
                player.getProxyPlayer().sendRichMessage(config.LOCALE_CONNECTING_MESSAGE);
                player.disconnect();
            }
            case FULL -> {
                AtomicInteger i = new AtomicInteger(0);
                queuedPlayers.forEach(p -> p.getProxyPlayer().sendRichMessage(MessageFormat.format(config.LOCALE_QUEUE_MESSAGE, i.incrementAndGet())));
            }
            case OFFLINE -> queuedPlayers.forEach((p) -> p.getProxyPlayer().sendRichMessage(config.LOCALE_SERVER_OFFLINE));
        }
    }

    private void startPingTask() {
        if (pingTask != null) pingTask.cancel();
        pingTask = getServer().getScheduler().buildTask(this, this::ping).repeat(config.MAIN_CHECK_INTERVAL, TimeUnit.MILLISECONDS).schedule();
    }

    private void ping() {
        try {
            ServerPing serverPing = targetServer.ping().get();
            if (serverPing.getPlayers().isEmpty()) return;
            ServerPing.Players players = serverPing.getPlayers().get();
            serverStatus = players.getOnline() >= players.getMax() ? ServerStatus.FULL : ServerStatus.NORMAL;
        } catch (Exception ignored) {
            serverStatus = ServerStatus.OFFLINE;
        }
    }
}
