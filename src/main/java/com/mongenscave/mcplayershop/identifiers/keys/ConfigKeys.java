package com.mongenscave.mcplayershop.identifiers.keys;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ConfigKeys {
    UPDATE_CHECKER_ENABLED("update-checker.enabled"),

    DATABASE_MYSQL("database.mysql"),
    DATABASE_POOL("database.pool"),

    STORAGE_MAX_ITEMS("storage.max-items"),

    FORMATTING_AMOUNT_FORMAT("formatting.amount-format"),
    FORMATTING_TIME_FORMAT("formatting.time-format"),

    HOLOGRAM("hologram");

    private static final Config config = McPlayerShop.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public long getLong() {
        return config.getLong(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }

    public @NotNull Section getSection() {
        return config.getSection(path);
    }
}