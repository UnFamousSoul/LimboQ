package com.unfamoussoul.limboQ.handlers;

import com.unfamoussoul.limboQ.entities.Settings;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class SettingsHandler{
    private final String name = "config.yml";
    private final Path directory;
    private final Logger logger;
    public Settings settings;

    @Nullable
    private @Unmodifiable YamlDocument Check() {
        if (new File(directory.toString(), name).exists()) {return ReadConfig();}
        return CreateConfig();
    }

    @Nullable
    private YamlDocument ReadConfig() {
        try {
            YamlDocument c = YamlDocument.create(new File(directory.toFile(), name));
            c.update();
            return c;
        } catch (IOException e) {
            return CreateConfig();
        }
    }


    @Nullable
    private YamlDocument CreateConfig() {
        try {
            InputStream file_path = SettingsHandler.class.getResourceAsStream("/" + name);
            if (file_path == null) return null;
            YamlDocument c = YamlDocument.create(new File(directory.toFile(), name), file_path);
            c.update();
            c.save();
            return c;
        } catch (IOException e) {
            logger.error("ConfigHandler: ", e); return null;
        }
    }

    public SettingsHandler(Path _directory, Logger _logger) {
        directory = _directory;
        logger = _logger;

        YamlDocument yaml = Check();
        if (yaml == null) return;
        settings = new Settings(yaml);
    }
}