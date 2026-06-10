package com.mongenscave.mcplayershop.hooks.impl.currency.impl;

import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class PlayerPointsHook implements Currency {

    private final PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();

    @NotNull
    @Contract(pure = true)
    @Override
    public String id() {
        return "playerpoints";
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        return api.look(player.getUniqueId()) >= amount;
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        return api.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public void deposit(@NotNull Player player, double amount) {
        api.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}