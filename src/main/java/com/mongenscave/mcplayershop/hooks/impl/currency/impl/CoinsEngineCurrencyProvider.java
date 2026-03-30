package com.mongenscave.mcplayershop.hooks.impl.currency.impl;

import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

public final class CoinsEngineCurrencyProvider implements Currency {

    private final String currencyId;

    public CoinsEngineCurrencyProvider(String currencyId) {
        this.currencyId = currencyId;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String id() {
        return "coinsengine:" + currencyId;
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        return CoinsEngineAPI.getBalance(player.getUniqueId(), currencyId) >= amount;
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        return CoinsEngineAPI.removeBalance(player.getUniqueId(), currencyId, amount);
    }

    @Override
    public void deposit(@NotNull Player player, double amount) {
        CoinsEngineAPI.addBalance(player.getUniqueId(), currencyId, amount);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}