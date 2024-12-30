package com.unfamoussoul.limboQ;

import com.google.inject.Inject;
import com.unfamoussoul.limboQ.commands.Reload;
import com.unfamoussoul.limboQ.entities.Config;
import com.unfamoussoul.limboQ.handlers.QueueHandler;
import com.unfamoussoul.limboQ.listeners.QueueListener;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
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

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "limboq", name = "LimboQ", version = BuildConstants.VERSION, description = "blockiter project", url = "mc.blockiter.com", authors = {"UnFamousSoul"})
public class LimboQ {

    @Inject
    private final Logger logger;
    private final ProxyServer server;
    private final File configFile;
    private final LimboFactory factory;
    public LinkedList<LimboPlayer> queuedPlayers = new LinkedList<>();
    private RegisteredServer targetServer;
    private ServerStatus serverStatus;
    private Limbo queueServer;
    private ScheduledTask queueTask;
    private ScheduledTask pingTask;

    @Inject
    public LimboQ(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;

        this.server = server;

        File dataDirectoryFile = dataDirectory.toFile();
        configFile = new File(dataDirectoryFile, "config.yml");

        factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        reload();
    }

    public void reload() {
        Config.IMP.reload(configFile);
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

        Optional<RegisteredServer> server = getServer().getServer(Config.IMP.MAIN.SERVER);
        server.ifPresentOrElse(registeredServer -> {
            targetServer = registeredServer;
            startPingTask();
            startQueueTask();
        }, () -> logger.error("Server {} doesn't exists!", Config.IMP.MAIN.SERVER));
    }

    private Limbo createQueueServer() {
        return factory.createLimbo(
                        factory.createVirtualWorld(Dimension.valueOf(Config.IMP.MAIN.WORLD.DIMENSION), Config.IMP.MAIN.WORLD.X, Config.IMP.MAIN.WORLD.Y, Config.IMP.MAIN.WORLD.Z, Config.IMP.MAIN.WORLD.YAW, Config.IMP.MAIN.WORLD.PITCH)
                )
                .setName(Config.IMP.MAIN.WORLD.NAME)
                .setWorldTime(Config.IMP.MAIN.WORLD.WORLD_TIME)
                .setGameMode(GameMode.valueOf(Config.IMP.MAIN.WORLD.GAMEMODE))
                .setViewDistance(Config.IMP.MAIN.WORLD.VIEW_DISTANCE)
                .setSimulationDistance(Config.IMP.MAIN.WORLD.SIMULATION_DISTANCE);
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
        queueTask = getServer().getScheduler().buildTask(this, this::queue).repeat(Config.IMP.MAIN.CHECK_INTERVAL, TimeUnit.SECONDS).schedule();
    }

    private void queue() {
        switch (serverStatus) {
            case NORMAL -> {
                if (queuedPlayers.isEmpty()) return;

                LimboPlayer player = queuedPlayers.getFirst();
                player.getProxyPlayer().sendRichMessage(Config.IMP.MESSAGES.CONNECTING_MESSAGE);
                player.disconnect();
            }
            case FULL -> {
                AtomicInteger i = new AtomicInteger(0);
                queuedPlayers.forEach(p -> p.getProxyPlayer().sendRichMessage(MessageFormat.format(Config.IMP.MESSAGES.QUEUE_MESSAGE, i.incrementAndGet())));
            }
            case OFFLINE -> queuedPlayers.forEach((p) -> p.getProxyPlayer().sendRichMessage(Config.IMP.MESSAGES.SERVER_OFFLINE));
        }
    }

    private void startPingTask() {
        if (pingTask != null) pingTask.cancel();
        pingTask = getServer().getScheduler().buildTask(this, this::ping).repeat(Config.IMP.MAIN.CHECK_INTERVAL, TimeUnit.SECONDS).schedule();
    }

    private void ping() {
        try {
            ServerPing serverPing = targetServer.ping().get();
            if (serverPing.getPlayers().isEmpty()) return;
            ServerPing.Players players = serverPing.getPlayers().get();
            serverStatus = players.getOnline() >= players.getMax() ? ServerStatus.FULL : ServerStatus.NORMAL;
        } catch (InterruptedException | ExecutionException ignored) {
            serverStatus = ServerStatus.OFFLINE;
        }
    }
}
