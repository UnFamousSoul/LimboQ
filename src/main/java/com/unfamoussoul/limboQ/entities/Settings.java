package com.unfamoussoul.limboQ.entities;

import com.unfamoussoul.limboQ.handlers.settings.SettingsHandler;

public class Settings extends SettingsHandler {

    @Ignore
    public static final Settings IMP = new Settings();

    public String PREFIX = "LimboQ";

    @Create
    public Settings.MAIN MAIN;

    public static class MAIN {
        public String SERVER = "survival";
        public boolean QUEUE_ON_LOGIN = true;
        public boolean ENABLE_KICK_MESSAGE = false;
        public String KICK_MESSAGE = "The server is full";
        public int CHECK_INTERVAL = 2;

        @Create
        public Settings.MAIN.WORLD WORLD;

        public static class WORLD {
            public String NAME = "LimboQ";

            public double X = 0;
            public double Y = 100;
            public double Z = 0;
            public float YAW = 90.0f;
            public float PITCH = 0.0f;

            public String DIMENSION = "OVERWORLD";
            public long WORLD_TIME = 6000;
            public String GAMEMODE = "SPECTATOR";
            public int VIEW_DISTANCE = 2;
            public int SIMULATION_DISTANCE = 2;
        }
    }

    @Create
    public Settings.MESSAGES MESSAGES;

    public static class MESSAGES {
        public String QUEUE_MESSAGE = "{PRFX} Your position in queue: {0}";
        public String CONNECTING_MESSAGE = "{PRFX} <green>Connecting to the server!";
        public String SERVER_OFFLINE = "{PRFX} <red>Server is offline!";
        public String RELOAD = "<green>reloaded!";
        public String RELOAD_FAILED = "<red>Reload failed!";
    }
}
