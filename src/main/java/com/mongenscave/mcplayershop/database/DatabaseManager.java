package com.mongenscave.mcplayershop.database;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.database.impl.H2;
import com.mongenscave.mcplayershop.database.impl.MySQL;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DatabaseManager {
    private static final McPlayerShop plugin = McPlayerShop.getInstance();
    private static final Config config = plugin.getConfiguration();
    private static Database database;

    private DatabaseManager() {
    }

    public static void initialize() {
        String databaseType = config.getString("database.type", "h2").toLowerCase();

        switch (databaseType) {
            case "mysql" -> database = new MySQL();
            case "h2" -> database = new H2();
            default -> {
                LoggerUtils.error("Unsupported database type: " + databaseType);
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        try {
            database.initialize().join();
            LoggerUtils.info("Database initialized successfully");
        } catch (Exception exception) {
            LoggerUtils.error("Failed to initialize database: " + exception.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static @NotNull Database getDatabase() {
        return database;
    }

    @NotNull
    @Contract(pure = true)
    public static String getDatabaseType() {
        return database instanceof MySQL ? "MySQL" : "H2";
    }
}