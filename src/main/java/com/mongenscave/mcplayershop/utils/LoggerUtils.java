package com.mongenscave.mcplayershop.utils;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("McPlayerShop");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;110;202;157m";
        String reset = "\u001B[0m";
        String software = McPlayerShop.getInstance().getServer().getName();
        String version = McPlayerShop.getInstance().getServer().getVersion();

        info("");
        info("{}    _______   ______ ___   ___  _   _ _   _  ___  _____ {}", main, reset);
        info("{}   |_   _\\ \\ / / ___/ _ \\ / _ \\| \\ | | | | |/ _ \\| ____| {}", main, reset);
        info("{}     | |  \\ V / |  | | | | | | |  \\| | |_| | | | |  _|   {}", main, reset);
        info("{}     | |   | || |__| |_| | |_| | |\\  |  _  | |_| | |___  {}", main, reset);
        info("{}     |_|   |_| \\____\\___/ \\___/|_| \\_|_| |_|\\___/|_____| {}", main, reset);
        info("");
        info("{}   The plugin successfully started.{}", main, reset);
        info("{}   mc-PlayerShop {} {}{}", main, software, version, reset);
        info("{}   Discord @ dc.mongenscave.com{}", main, reset);
        info("");
        info("\u001B[33m   [Database] Selected database type: {}\u001B[0m", DatabaseManager.getDatabaseType());
    }
}