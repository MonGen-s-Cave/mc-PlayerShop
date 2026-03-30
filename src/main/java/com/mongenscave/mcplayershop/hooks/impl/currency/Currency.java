package com.mongenscave.mcplayershop.hooks.impl.currency;

import org.bukkit.entity.Player;

public interface Currency {
    String id();
    boolean has(Player player, double amount);
    boolean withdraw(Player player, double amount);
    void deposit(Player player, double amount);
    String format(double amount);
}