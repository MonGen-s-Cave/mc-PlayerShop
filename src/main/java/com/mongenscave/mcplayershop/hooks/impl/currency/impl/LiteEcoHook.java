package com.mongenscave.mcplayershop.hooks.impl.currency.impl;

import com.github.encryptsl.lite.eco.LiteEco;
import com.github.encryptsl.lite.eco.api.interfaces.LiteEconomyAPI;
import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public final class LiteEcoHook implements Currency {

    private final LiteEconomyAPI api = LiteEco.Companion.getInstance().getApi();
    private final String currency;

    public LiteEcoHook(@NotNull String currency) {
        this.currency = currency;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String id() {
        return "liteeco:" + currency;
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        return api.has(player.getUniqueId(), currency, BigDecimal.valueOf(amount));
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        UUID uuid = player.getUniqueId();

        if (!has(player, amount)) {
            return false;
        }

        McPlayerShop.getScheduler().runTaskAsynchronously(() -> {
            try {
                BuildersKt.runBlocking(Dispatchers.getIO(), (
                        Function2<CoroutineScope, Continuation<? super Unit>, Object>)
                        (scope, continuation) ->
                                api.withdraw(uuid, currency, BigDecimal.valueOf(amount), continuation));

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return true;
    }

    @Override
    public void deposit(@NotNull Player player, double amount) {
        UUID uuid = player.getUniqueId();

        McPlayerShop.getScheduler().runTaskAsynchronously(() -> {
            try {
                BuildersKt.runBlocking(Dispatchers.getIO(), (
                        Function2<CoroutineScope, Continuation<? super Unit>, Object>)
                        (scope, continuation) ->
                                api.deposit(uuid, currency, BigDecimal.valueOf(amount), continuation));

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @Contract(pure = true)
    public @NotNull String format(double amount) {
        return String.valueOf(amount);
    }
}