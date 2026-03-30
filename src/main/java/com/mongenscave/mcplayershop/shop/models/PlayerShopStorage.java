package com.mongenscave.mcplayershop.shop.models;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@Getter
public final class PlayerShopStorage {

    private final UUID shopId;
    private final ItemStack[] contents;

    private volatile boolean dirty;

    public PlayerShopStorage(UUID shopId, int size) {
        this.shopId = shopId;
        this.contents = new ItemStack[size];
    }

    public void set(int slot, ItemStack item) {
        contents[slot] = item;
        dirty = true;
    }

    public void markSaved() {
        dirty = false;
    }
}