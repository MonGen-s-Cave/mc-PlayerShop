package com.mongenscave.mcplayershop.guis;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import org.jetbrains.annotations.NotNull;

public interface ShopBoundMenu {

    @NotNull PlayerShop getShop();

    default boolean isStale() {
        PlayerShop shop = getShop();

        return McPlayerShop.getInstance().getShopManager()
                .get(shop.getLocation())
                .map(current -> !current.getShopId().equals(shop.getShopId()))
                .orElse(true);
    }
}