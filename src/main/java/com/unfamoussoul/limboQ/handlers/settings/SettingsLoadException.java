package com.unfamoussoul.limboQ.handlers.settings;

public class SettingsLoadException extends RuntimeException {
    public SettingsLoadException(Throwable cause) {
        this("An unexpected internal error was caught during loading the config.", cause);
    }

    public SettingsLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}