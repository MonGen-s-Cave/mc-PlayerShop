package com.mongenscave.mcplayershop.listener;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopManager;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.service.PlayerShopService;
import com.mongenscave.mcplayershop.shop.service.ShopVisualService;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChunkListener implements Listener {

    private final McPlayerShop plugin = McPlayerShop.getInstance();

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        PlayerShopManager manager = plugin.getShopManager();

        List<PlayerShop> shops = manager.getByChunk(chunk);
        if (shops.isEmpty()) return;

        PlayerShopService service = plugin.getShopService();
        ShopVisualService visuals = plugin.getVisualService();

        McPlayerShop.getScheduler().runTask(() -> {
            for (PlayerShop shop : shops) {
                service.setupBlock(shop.getLocation());
                visuals.spawn(shop);
            }
        });
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        PlayerShopManager manager = plugin.getShopManager();

        List<PlayerShop> shops = manager.getByChunk(chunk);
        if (shops.isEmpty()) return;

        ShopVisualService visuals = plugin.getVisualService();
        for (PlayerShop shop : shops) {
            visuals.remove(shop.getShopId());
        }
    }
}