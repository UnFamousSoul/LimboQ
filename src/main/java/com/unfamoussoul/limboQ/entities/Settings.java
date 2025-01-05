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
            MAIN_SERVER = yaml.getString("server", "survival");
            MAIN_QUEUE_ON_LOGIN = yaml.getBoolean("queue_on_login", true);
            MAIN_ENABLE_KICK_MESSAGE = yaml.getBoolean("enable_kick_message", false);
            MAIN_KICK_MESSAGE = yaml.getString("kick_message", "The server is full");
            MAIN_CHECK_INTERVAL = yaml.getInt("check_interval", 2);
        }

        Section world = yaml.getSection("world");
        if (world != null) {
            WORLD_NAME = yaml.getString("name", "LimboQ");
            WORLD_X = yaml.getDouble("x", 0.5);
            WORLD_Y = yaml.getDouble("y", 0.0);
            WORLD_Z = yaml.getDouble("z", 0.5);
            WORLD_YAW = yaml.getFloat("yaw", 90.0F);
            WORLD_PITCH = yaml.getFloat("pitch", 0.0F);
            WORLD_DIMENSION = yaml.getString("dimension", "THE_END");
            WORLD_TIME = yaml.getInt("time", 18000);
            WORLD_GAMEMODE = yaml.getString("gamemode", "SURVIVAL");
            WORLD_VIEW_DISTANCE = yaml.getInt("view_distance", 2);
            WORLD_SIMULATION_DISTANCE = yaml.getInt("simulation_distance", 2);
        }

        Section locale = yaml.getSection("locale");
        if (locale != null) {
            LOCALE_QUEUE_MESSAGE = yaml.getString("queue message", "{PRFX} Your position in queue: {0}");
            LOCALE_CONNECTING_MESSAGE = yaml.getString("connecting_message", "{PRFX} <green>Connecting to the server!");
            LOCALE_SERVER_OFFLINE = yaml.getString("server_offline", "{PRFX} <red>Server is offline!");
            LOCALE_RELOAD = yaml.getString("reload", "<green>reloaded!");
            LOCALE_RELOAD_FAILED = yaml.getString("reload_failed", "<red>Reload failed!");
        }
    }
}
