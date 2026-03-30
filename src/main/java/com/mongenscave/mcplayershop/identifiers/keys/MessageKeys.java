package com.mongenscave.mcplayershop.identifiers.keys;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),
    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),
    PLAYER_NOT_FOUND("messages.player-not-found"),
    MISSING_ARGUMENT("messages.missing-argument"),

    SHOP_STORAGE_NOT_EMPTY("messages.shop.storage-not-empty"),
    SHOP_WITHDRAW_FAILED("messages.shop.withdraw-failed"),
    SHOP_DEPOSIT_FAILED("messages.shop.deposit-failed"),

    SHOP_TRANSACTION_SUCCESS("messages.shop.transaction-success"),
    SHOP_TRANSACTION_FAILED("messages.shop.transaction-failed");

    private final String path;
    private static final Config config = McPlayerShop.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));
    }

    @NotNull
    public @Unmodifiable List<String> getMessages() {
        return config.getStringList(path)
                .stream()
                .map(MessageProcessor::process)
                .toList();
    }
}