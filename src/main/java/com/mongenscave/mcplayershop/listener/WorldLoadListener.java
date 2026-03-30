package com.mongenscave.mcplayershop.listener;

import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.shop.service.ShopLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class WorldLoadListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimeWorldLoad(@NotNull LoadSlimeWorldEvent event) {
        SlimeWorld slimeWorld = event.getSlimeWorld();
        String worldName = slimeWorld.getName();

        McPlayerShop.getScheduler().runTaskLater(() -> ShopLoader.loadByWorld(McPlayerShop.getInstance().getShopManager(), worldName), 20L);
    }
}