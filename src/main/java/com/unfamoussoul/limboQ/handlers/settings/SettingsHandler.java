package com.unfamoussoul.limboQ.handlers.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SettingsHandler {

    private final Yaml yaml = new Yaml();
    private SettingsHandler original;
    private String prefix = null;
    private final FieldNameStyle classFieldNameStyle;
    private final FieldNameStyle nodeFieldNameStyle;

    private final Logger logger = LoggerFactory.getLogger(SettingsHandler.class);

    public SettingsHandler() {
        classFieldNameStyle = FieldNameStyle.MACRO_CASE;
        nodeFieldNameStyle = FieldNameStyle.KEBAB_CASE;
    }

    public void reload(File configFile, @Nullable String prefix) {
        reload(configFile.toPath(), prefix);
    }

    public void reload(Path configFile, @Nullable String prefix) {
        switch (load(configFile, prefix)) {
            case SUCCESS: {
                save(configFile);
                break;
            }
            case FAIL:
            case CONFIG_NOT_EXISTS: {
                save(configFile);
                load(configFile, prefix); // Load again, because it now exists.
                break;
            }
            default: {
                throw new AssertionError("Invalid Result.");
            }
        }
    }

    public LoadResult load(Path configFile, @Nullable String prefix) {
        try {original = getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {throw new IllegalStateException("Unable to create new instance of " + getClass().getName());}

        if (!Files.exists(configFile)) return LoadResult.CONFIG_NOT_EXISTS;

        dispose();

        this.prefix = prefix;

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", "_").replace(":", ".");
        now = now.substring(0, now.lastIndexOf("."));
        try (InputStream fileInputStream = Files.newInputStream(configFile)) {
            Map<String, Object> data = yaml.load(fileInputStream);

            if (data != null && !data.isEmpty()) {
                processMap(data, original, "", null, now, false);
                processMap(data, this, "", configFile, now, true);
            }
        } catch (Throwable t) {
            try {
                Path parent = configFile.getParent();
                if (parent == null) {
                    throw new NullPointerException("Config parent path is null for " + configFile);
                }

                String newFileName = configFile.getFileName() + "_invalid_" + now;
                Path configFileCopy = parent.resolve(newFileName);
                Files.copy(configFile, configFileCopy, StandardCopyOption.REPLACE_EXISTING);

                throw new SettingsLoadException("Unable to load config. File was copied to " + newFileName, t);
            } catch (IOException e) {
                throw new SettingsLoadException("Unable to load config and to make a copy.", e);
            }
        }

        return LoadResult.SUCCESS;
    }

    private void processMap(Map<String, Object> input, Object instance, String oldPath, @Nullable Path configFile, String now, boolean usePrefix) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = oldPath + (oldPath.isEmpty() ? oldPath : ".") + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = ((String) value).replace("{NL}", "\n");
                if (!usePrefix) continue;

                if (prefix != null) {stringValue = stringValue.replace("{PRFX}", prefix);}

                value = stringValue;

                if (key.equals("prefix")) {prefix = stringValue;}
            }

            setFieldByKey(key, instance, value, configFile, now, usePrefix);
        }
    }

    @SuppressWarnings("unchecked")
    private void setFieldByKey(String key, Object destination, Object value, @Nullable Path configFile, String now, boolean usePrefix) {
        String[] split = key.split("\\.");
        Object instance = getInstance(destination, split);
        if (instance == null) return;
        Field field = getField(split, instance);
        if (field == null) return;
        try {
            if (field.getType() != Map.class && value instanceof Map) {
                processMap((Map<String, Object>) value, destination, key, configFile, now, usePrefix);
            } else if (field.getAnnotation(Final.class) == null) {
                setFieldBack(field, value, instance, usePrefix);
            }
        } catch (Throwable t) {
            logger.warn("Failed to set config option: {}: {} | {}", key, value, instance);
            if (configFile == null) return;
            Path parent = configFile.getParent();
            if (parent == null) throw new NullPointerException("Config parent path is null for " + configFile);

            Path configFileBackup = parent.resolve(configFile.getFileName() + "_backup_" + now);
            if (Files.exists(configFileBackup)) return;

            try {
                Files.copy(configFile, configFileBackup, StandardCopyOption.REPLACE_EXISTING);
                logger.warn("Unable to load some of the config options. File was copied to {}", configFileBackup.getFileName());
            } catch (Throwable t2) {logger.warn("Unable to load some of the config options and to make a copy.", t2);}
        }
    }

    @SuppressWarnings("unchecked")
    private void setFieldBack(Field field, Object value, Object instance, boolean usePrefix) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        Object text = null;
        if (field.getGenericType() instanceof ParameterizedType) {
            if (field.getType() == Map.class && value instanceof Map) {
                Type parameterType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
                if (!(parameterType instanceof Class<?> parameter)) return;
                if (!isNodeMapping(parameter)) return;
                text = ((Map<String, ?>) value).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> createNodeSequence(parameter, e.getValue(), usePrefix)));
            } else if (field.getType() == List.class && value instanceof List) {
                Type parameterType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                if (!(parameterType instanceof Class<?> parameter)) return;
                if (!isNodeMapping(parameter)) return;
                text = ((List<?>) value).stream()
                        .map(obj -> createNodeSequence(parameter, obj, usePrefix))
                        .collect(Collectors.toList());
            }
        } else {text = value;}

        setField(field, instance, text);
    }

    private Object getInstance(Object instance, String[] split) {
        try {
            for (String s : split) {
                String name = toClassFieldName(s);
                Field field = instance.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(instance);
                if (value == null) {
                    value = field.getType().getDeclaredConstructor().newInstance();
                    setField(field, instance, value);
                }
                instance = value;
            }
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Unable to find field " + e.getMessage() + " in " + instance.getClass().getName());
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to create new instance: " + e.getMessage());
        }

        return instance;
    }

    @Nullable
    private Field getField(String[] split, Object instance) {
        try {
            String className = toClassFieldName(split[split.length - 1]);
            Field field = instance.getClass().getField(className);
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            logger.warn("Invalid config field: {} for {}", String.join(".", split), instance.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isNodeMapping(Class<?> cls) {
        return cls.getAnnotation(NodeSequence.class) != null
                || (!cls.isPrimitive() && !cls.isEnum() && !Number.class.isAssignableFrom(cls)
                && !Map.class.isAssignableFrom(cls) && !List.class.isAssignableFrom(cls)
                && !String.class.isAssignableFrom(cls));
    }

    public void save(Path configFile) {
        try {
            Path parent = configFile.getParent();
            if (!Files.exists(configFile) && parent != null) {
                Files.createDirectories(parent);
                Files.createFile(configFile);
            }

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(configFile)), StandardCharsets.UTF_8));
            writeConfigKeyValue(writer, getClass(), this, original, 0, true);
            writer.close();
        } catch (Throwable t) {
            throw new SettingsSaveException(t);
        }
    }

    private void writeConfigKeyValue(PrintWriter writer, Class<?> clazz, Object instance, Object original, int indent, boolean usePrefix)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        String lineSeparator = System.lineSeparator();
        String spacing = getSpacing(indent);

        for (Field field : clazz.getFields()) {
            if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers())) continue;
            Class<?> current = field.getType();
            if (current.getAnnotation(Ignore.class) != null) continue;

            writeNewLines(field.getAnnotation(NewLine.class), writer, lineSeparator);

            if (field.getAnnotation(Create.class) != null) {
                writeNewLines(current.getAnnotation(NewLine.class), writer, lineSeparator);

                writer.write(spacing);
                writer.write(toNodeFieldName(field.getName()));
                writer.write(':');
                writer.write(lineSeparator);

                field.setAccessible(true);
                Object value = field.get(instance);

                if (value == null) {
                    value = current.getDeclaredConstructor().newInstance();
                    setField(field, instance, value);
                }

                Object originalValue = field.get(original);

                if (originalValue == null) {
                    originalValue = current.getDeclaredConstructor().newInstance();
                    setField(field, original, originalValue);
                }

                writeConfigKeyValue(writer, current, value, originalValue, indent + 2, usePrefix);
            } else {
                String fieldName = field.getName();

                String fieldValue = toYamlString(field.get(instance), lineSeparator, spacing, usePrefix);
                String originalFieldValue = toYamlString(field.get(original), lineSeparator, spacing, usePrefix);
                String valueToWrite = fieldValue;

                if (prefix != null) {
                    if (fieldValue.startsWith("\"") && fieldValue.endsWith("\"")) { // String
                        if (fieldValue.replace("{PRFX}", prefix).equals(originalFieldValue.replace("{PRFX}", prefix))) {
                            valueToWrite = originalFieldValue;
                        }
                    } else if (fieldValue.contains(lineSeparator)) { // Map/List
                        StringBuilder builder = new StringBuilder();
                        String[] lines = fieldValue.split(lineSeparator);
                        String[] originalLines = originalFieldValue.split(lineSeparator);
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            String toAppend = line;
                            if (i < originalLines.length) {
                                String originalLine = originalLines[i];
                                if (line.replace("{PRFX}", prefix).equals(originalLine.replace("{PRFX}", prefix))) {
                                    toAppend = originalLine;
                                }
                            }
                            builder.append(toAppend).append(lineSeparator);
                        }
                        builder.setLength(builder.length() - lineSeparator.length());
                        valueToWrite = builder.toString();
                    }
                }

                writer.write(spacing);
                writer.write(toNodeFieldName(fieldName));
                writer.write((valueToWrite.contains(lineSeparator) ? ":" : ": "));
                writer.write(valueToWrite);
                writer.write(lineSeparator);
            }
        }
    }

    private String getSpacing(int indent) {
        return new String(new char[indent]).replace('\0', ' ');
    }

    private void writeNewLines(@Nullable NewLine newLine, PrintWriter writer, String lineSeparator) {
        if (newLine == null) return;
        for (int i = 0; i < newLine.amount(); ++i) {
            writer.write(lineSeparator);
        }
    }

    @SuppressWarnings("unchecked")
    private void setField(Field field, Object owner, Object value)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {

        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) throw new IllegalStateException("This field shouldn't be static.");
        if (Modifier.isFinal(modifiers)) throw new IllegalStateException("This field shouldn't be final.");

        if (field.getType() == Map.class && value instanceof Map) {
            if (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] != String.class) {
                throw new IllegalStateException("Key type of this map should be " + String.class);
            }
            value = ((Map<?, ?>) value).entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
        } else if (field.getType().isEnum()) {
            String stringValue = String.valueOf(value);
            value = stringValue.isEmpty() || stringValue.equals("null") ? null : Enum.valueOf((Class<? extends Enum>) field.getType(), stringValue.toUpperCase(Locale.ROOT));
        }
        field.set(owner, value);
    }

    protected static <T> T createNodeSequence(Class<T> nodeSequenceClass) {
        try {
            Constructor<T> constructor = nodeSequenceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Method not found: " + e.getMessage());
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to create instance of " + nodeSequenceClass.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createNodeSequence(Class<T> nodeSequenceClass, Object objects, boolean usePrefix) {
        if (!(objects instanceof Map)) {
            return (T) objects;
        }

        T instance = createNodeSequence(nodeSequenceClass);
        processMap((Map<String, Object>) objects, instance, "", null, null, usePrefix);
        return instance;
    }

    private String toNodeFieldName(@NotNull String field) {
        if (field.matches("^\\d+")) {
            return toNodeFieldName('"' + field + '"');
        }

        return nodeFieldNameStyle.fromMacroCase(classFieldNameStyle.toMacroCase(field));
    }

    private String toClassFieldName(String field) {
        return classFieldNameStyle.fromMacroCase(nodeFieldNameStyle.toMacroCase(field));
    }

    private String toYamlString(Object value, String lineSeparator, String spacing, boolean usePrefix)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return toYamlString(value, lineSeparator, spacing, false, false, 0, usePrefix);
    }

    private String toYamlString(Object value, String lineSeparator, String spacing, boolean isCollection, boolean isMap, int nested, boolean usePrefix)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) return "{}";

            StringBuilder builder = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object mapValue = entry.getValue();
                String data = toYamlString(mapValue, lineSeparator, spacing, true, true, 0, usePrefix);
                builder.append(lineSeparator)
                        .append(spacing).append("  ")
                        .append(toNodeFieldName(String.valueOf(key))).append(data.startsWith(lineSeparator) ? ":" : ": ")
                        .append(data);
            }

            return builder.toString();
        } else if (value instanceof List<?> listValue) {
            if (listValue.isEmpty()) {
                return "[]";
            }

            StringBuilder builder = new StringBuilder();
            boolean newLine = nested == 0;
            for (Object obj : listValue) {
                if (newLine) {
                    builder.append(lineSeparator).append(spacing).append(getSpacing(2 + nested * 2));
                } else {
                    newLine = true;
                }

                builder.append("- ").append(
                        toYamlString(obj, lineSeparator, spacing, true, false, nested + 1, usePrefix));
            }

            return builder.toString();
        } else if (value instanceof String stringValue) {
            if (stringValue.isEmpty()) return "\"\"";
            return ('"' + stringValue.replace("\\", "\\\\").replace("\"", "\\\"") + '"').replace("\n", "{NL}");
        } else if (value != null && isCollection && isNodeMapping(value.getClass())) {
            try (
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter writer = new PrintWriter(stringWriter)
            ) {
                if (isMap) writer.write(lineSeparator);
                int indent = spacing.length() + 4;
                writeConfigKeyValue(writer, value.getClass(), value, value, indent, usePrefix);
                writer.flush();
                String data = stringWriter.toString();
                return data.substring(isMap ? 0 : indent, data.length() - lineSeparator.length());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return String.valueOf(value);
        }
    }

    public void dispose() {
        prefix = null;
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

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface NodeSequence { }

    protected enum FieldNameStyle {
        KEBAB_CASE(s -> s.replace("_", "-").toLowerCase(Locale.ROOT), s -> s.replace("-", "_").toUpperCase(Locale.ROOT)),
        MACRO_CASE(s -> s, s -> s);

        private final Function<String, String> fromMacroCase;
        private final Function<String, String> toMacroCase;

        FieldNameStyle(Function<String, String> fromMacroCase, Function<String, String> toMacroCase) {
            this.fromMacroCase = fromMacroCase;
            this.toMacroCase = toMacroCase;
        }

        private String fromMacroCase(String fieldName) {
            return fromMacroCase.apply(fieldName);
        }

        private String toMacroCase(String fieldName) {
            return toMacroCase.apply(fieldName);
        }
    }
}