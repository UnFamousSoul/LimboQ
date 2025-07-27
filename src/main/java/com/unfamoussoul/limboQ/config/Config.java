package com.unfamoussoul.limboQ.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class Config {

    public String PREFIX;

    public String MAIN_SERVER = "survival";
    public Boolean MAIN_QUEUE_ON_LOGIN = true;
    public Boolean MAIN_ENABLE_KICK_MESSAGE = false;
    public String MAIN_KICK_MESSAGE = "The server is full";
    public Integer MAIN_CHECK_INTERVAL = 2;

    public String WORLD_NAME = "LimboQ";
    public Double WORLD_X = 0.5;
    public Double WORLD_Y = 0.0;
    public Double WORLD_Z = 0.5;
    public Float WORLD_YAW = 90.0F;
    public Float WORLD_PITCH = 0.0F;
    public String WORLD_DIMENSION = "THE_END";
    public Integer WORLD_TIME = 18000;
    public String WORLD_GAMEMODE = "SURVIVAL";
    public Integer WORLD_VIEW_DISTANCE = 2;
    public Integer WORLD_SIMULATION_DISTANCE = 2;

    public String LOCALE_QUEUE_MESSAGE = "{PRFX} Your position in queue: {0}";
    public String LOCALE_CONNECTING_MESSAGE = "{PRFX} <green>Connecting to the server!";
    public String LOCALE_SERVER_OFFLINE = "{PRFX} <red>Server is offline!";
    public String LOCALE_RELOAD = "<green>reloaded!";
    public String LOCALE_RELOAD_FAILED = "<red>Reload failed!";

    public Config(YamlDocument yaml) {
        PREFIX = yaml.getString("prefix", "LimboQ");

        loadMain(yaml.getSection("main"));
        loadWorld(yaml.getSection("world"));
        loadLocale(yaml.getSection("locale"));
    }
    
    private void loadMain(Section section) {
        if (section == null) return;

        MAIN_SERVER = section.getString("server", "survival");
        MAIN_QUEUE_ON_LOGIN = section.getBoolean("queue-on-login", true);
        MAIN_ENABLE_KICK_MESSAGE = section.getBoolean("enable-kick-message", false);
        MAIN_KICK_MESSAGE = section.getString("kick-message", "The server is full");
        MAIN_CHECK_INTERVAL = section.getInt("check-interval", 2000);
    }
    
    private void loadWorld(Section section) {
        if (section == null) return;

        WORLD_NAME = section.getString("name", "LimboQ");
        WORLD_X = section.getDouble("x", 0.5);
        WORLD_Y = section.getDouble("y", 1.0);
        WORLD_Z = section.getDouble("z", 0.5);
        WORLD_YAW = section.getFloat("yaw", 90.0F);
        WORLD_PITCH = section.getFloat("pitch", 0.0F);
        WORLD_DIMENSION = section.getString("dimension", "THE_END");
        WORLD_TIME = section.getInt("time", 18000);
        WORLD_GAMEMODE = section.getString("gamemode", "SURVIVAL");
        WORLD_VIEW_DISTANCE = section.getInt("view-distance", 2);
        WORLD_SIMULATION_DISTANCE = section.getInt("simulation-distance", 2);
    }
    
    private void loadLocale(Section section) {
        if (section == null) return;
        
        LOCALE_QUEUE_MESSAGE = section.getString("queue-message", "{PRFX} Your position in queue: {0}").replace("{PRFX}", PREFIX);
        LOCALE_CONNECTING_MESSAGE = section.getString("connecting-message", "{PRFX} <green>Connecting to the server!").replace("{PRFX}", PREFIX);
        LOCALE_SERVER_OFFLINE = section.getString("server-offline", "{PRFX} <red>Server is offline!").replace("{PRFX}", PREFIX);
        LOCALE_RELOAD = section.getString("reload", "<green>reloaded!").replace("{PRFX}", PREFIX);
        LOCALE_RELOAD_FAILED = section.getString("reload-failed", "<red>Reload failed!").replace("{PRFX}", PREFIX);
    }
}
