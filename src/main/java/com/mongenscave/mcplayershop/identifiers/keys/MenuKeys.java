package com.mongenscave.mcplayershop.identifiers.keys;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum MenuKeys {

    SHOP_MAIN_TITLE("shop-main.title"),
    SHOP_MAIN_SIZE("shop-main.size"),
    SHOP_MAIN_TICK("shop-main.tick"),

    SHOP_TRANSACTIONS_TITLE("shop-transactions.title"),
    SHOP_TRANSACTIONS_SIZE("shop-transactions.size"),
    SHOP_TRANSACTIONS_TICK("shop-transactions.tick"),

    SHOP_STORAGE_TITLE("shop-storage.title"),
    SHOP_STORAGE_SIZE("shop-storage.size"),
    SHOP_STORAGE_TICK("shop-storage.tick"),

    SHOP_TRADE_TITLE("shop-trade.title"),
    SHOP_TRADE_SIZE("shop-trade.size"),
    SHOP_TRADE_TICK("shop-trade.tick");

    private static final Config config = McPlayerShop.getInstance().getGuis();
    private final String path;

    MenuKeys(@NotNull String path) {
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

    public List<Integer> getIntList() {
        return config.getIntList(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }
}