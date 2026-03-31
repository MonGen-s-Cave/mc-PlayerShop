package com.mongenscave.mcplayershop.shop.service;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopManager;

public final class ShopLoader {

    public static void load(PlayerShopManager manager, ShopVisualService visuals) {
        DatabaseManager.getDatabase().findAllShops().thenAccept(list -> {
            McPlayerShop.getScheduler().runTask(() -> {
                list.forEach(shop -> {
                    manager.register(shop);
                    visuals.spawn(shop);
                });
            });
        });
    }

    public static void loadByWorld(PlayerShopManager manager, String world) {
        DatabaseManager.getDatabase().findShopsByWorld(world).thenAccept(list -> {
            McPlayerShop.getScheduler().runTask(() -> {
                list.forEach(shop -> {
                    manager.register(shop);
                    McPlayerShop.getInstance().getVisualService().spawn(shop);
                });

            });
        });
    }
}