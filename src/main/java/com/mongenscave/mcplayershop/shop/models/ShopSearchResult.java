package com.mongenscave.mcplayershop.shop.models;

import com.mongenscave.mcplayershop.identifiers.ShopMode;
import org.jetbrains.annotations.NotNull;

public record ShopSearchResult(@NotNull PlayerShop shop, int stock, int space, int score) {
    public boolean isAvailable() {
        return shop.getMode() == ShopMode.SELL ? stock > 0 : space > 0;
    }
}