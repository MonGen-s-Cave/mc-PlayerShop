package com.mongenscave.mcplayershop.hooks.impl.currency.impl;

import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class VaultCurrencyProvider implements Currency {

    private final Economy economy;

    public VaultCurrencyProvider(Economy economy) {
        this.economy = economy;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String id() {
        return "vault";
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}