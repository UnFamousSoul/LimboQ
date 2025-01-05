package com.unfamoussoul.limboQ.entities;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class Settings {

    public String PREFIX;

    public String MAIN_SERVER = "survival";
    public boolean MAIN_QUEUE_ON_LOGIN = true;
    public boolean MAIN_ENABLE_KICK_MESSAGE = false;
    public String MAIN_KICK_MESSAGE = "The server is full";
    public int MAIN_CHECK_INTERVAL = 2;

    public String WORLD_NAME = "LimboQ";
    public double WORLD_X = 0.5;
    public double WORLD_Y = 0.0;
    public double WORLD_Z = 0.5;
    public float WORLD_YAW = 90.0F;
    public float WORLD_PITCH = 0.0F;
    public String WORLD_DIMENSION = "THE_END";
    public long WORLD_TIME = 18000;
    public String WORLD_GAMEMODE = "SURVIVAL";
    public int WORLD_VIEW_DISTANCE = 2;
    public int WORLD_SIMULATION_DISTANCE = 2;

    public String LOCALE_QUEUE_MESSAGE = "{PRFX} Your position in queue: {0}";
    public String LOCALE_CONNECTING_MESSAGE = "{PRFX} <green>Connecting to the server!";
    public String LOCALE_SERVER_OFFLINE = "{PRFX} <red>Server is offline!";
    public String LOCALE_RELOAD = "<green>reloaded!";
    public String LOCALE_RELOAD_FAILED = "<red>Reload failed!";

    public Settings(YamlDocument yaml) {
        PREFIX = yaml.getString("prefix", "LimboQ");

        Section main = yaml.getSection("main");
        if (main != null) {
            MAIN_SERVER = main.getString("server", "survival");
            MAIN_QUEUE_ON_LOGIN = main.getBoolean("queue-on-login", true);
            MAIN_ENABLE_KICK_MESSAGE = main.getBoolean("enable-kick-message", false);
            MAIN_KICK_MESSAGE = main.getString("kick-message", "The server is full");
            MAIN_CHECK_INTERVAL = main.getInt("check-interval", 2000);
        }

        Section world = yaml.getSection("world");
        if (world != null) {
            WORLD_NAME = world.getString("name", "LimboQ");
            WORLD_X = world.getDouble("x", 0.5);
            WORLD_Y = world.getDouble("y", 1.0);
            WORLD_Z = world.getDouble("z", 0.5);
            WORLD_YAW = world.getFloat("yaw", 90.0F);
            WORLD_PITCH = world.getFloat("pitch", 0.0F);
            WORLD_DIMENSION = world.getString("dimension", "THE_END");
            WORLD_TIME = world.getInt("time", 18000);
            WORLD_GAMEMODE = world.getString("gamemode", "SURVIVAL");
            WORLD_VIEW_DISTANCE = world.getInt("view-distance", 2);
            WORLD_SIMULATION_DISTANCE = world.getInt("simulation-distance", 2);
        }

        Section locale = yaml.getSection("locale");
        if (locale != null) {
            LOCALE_QUEUE_MESSAGE = locale.getString("queue-message", "{PRFX} Your position in queue: {0}").replace("{PRFX}", PREFIX);
            LOCALE_CONNECTING_MESSAGE = locale.getString("connecting-message", "{PRFX} <green>Connecting to the server!").replace("{PRFX}", PREFIX);
            LOCALE_SERVER_OFFLINE = locale.getString("server-offline", "{PRFX} <red>Server is offline!").replace("{PRFX}", PREFIX);
            LOCALE_RELOAD = locale.getString("reload", "<green>reloaded!").replace("{PRFX}", PREFIX);
            LOCALE_RELOAD_FAILED = locale.getString("reload-failed", "<red>Reload failed!").replace("{PRFX}", PREFIX);
        }
    }
}
