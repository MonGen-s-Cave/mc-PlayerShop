package com.mongenscave.mcplayershop.utils;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.listener.MenuListener;
import com.mongenscave.mcplayershop.listener.ShopListener;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    private final McPlayerShop plugin = McPlayerShop.getInstance();

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ShopListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin).build();
    }
}