package com.unfamoussoul.limboQ.handlers.settings;

public class SettingsSaveException extends RuntimeException {

    public SettingsSaveException(Throwable cause) {
        this("An unexpected internal error was caught during saving the config.", cause);
    }

    public SettingsSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}