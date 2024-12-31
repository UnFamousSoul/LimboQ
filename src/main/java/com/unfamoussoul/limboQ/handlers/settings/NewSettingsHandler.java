package com.unfamoussoul.limboQ.handlers.settings;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class NewSettingsHandler {

    private final Yaml yaml = new Yaml();
    private final Logger logger = LoggerFactory.getLogger(NewSettingsHandler.class);

    private NewSettingsHandler clazz;
    private String prefix = null;

    public void reload(Path configFile, @Nullable String prefix) {
        switch (load(configFile, prefix)) {
            case SUCCESS: {save(configFile); break;}
            case FAIL:
            case CONFIG_NOT_EXISTS: {save(configFile); load(configFile, prefix); break;}
        }
    }

    public LoadResult load(Path configFile, @Nullable String prefix) {
        try {clazz = getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {throw new IllegalStateException("Unable to create new instance of " + getClass().getName());}
        this.prefix = prefix;

        if (!Files.exists(configFile)) return LoadResult.CONFIG_NOT_EXISTS;


        try (InputStream fileInputStream = Files.newInputStream(configFile)) {
            Map<String, Object> data = yaml.load(fileInputStream);

            if (data != null && !data.isEmpty()) {
                //
            }
        } catch (Throwable t) {throw new SettingsLoadException(t);}

        return LoadResult.SUCCESS;
    }

    public void save(Path configFile) {
        try {
            Path parent = configFile.getParent();
            if (!Files.exists(configFile) && parent != null) {
                Files.createDirectories(parent);
                Files.createFile(configFile);
            }

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(configFile)), StandardCharsets.UTF_8));
            writeSave(0);
            writer.close();
        } catch (Throwable t) {throw new SettingsSaveException(t);}
    }

    private void writeSave(int space) {
        String lineSeparator = System.lineSeparator();
        String spacing = getSpacing(space);
    }

    private String getSpacing(int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }

    public enum LoadResult {
        SUCCESS,
        FAIL,
        CONFIG_NOT_EXISTS
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Create { }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Final { }

    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface NewLine { int amount() default 1;}

    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Ignore { }
}
