package com.mongenscave.mcplayershop.database.impl;

import com.mongenscave.mcplayershop.database.Database;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShopTransaction;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class H2 implements Database {
    @Override
    public CompletableFuture<Void> initialize() {
        return null;
    }

    @Override
    public CompletableFuture<Void> insertShop(PlayerShop shop) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateShop(PlayerShop shop) {
        return null;
    }

    @Override
    public CompletableFuture<Void> deleteShop(UUID shopId) {
        return null;
    }

    @Override
    public CompletableFuture<Optional<PlayerShop>> findByLocation(String world, int x, int y, int z) {
        return null;
    }

    @Override
    public CompletableFuture<List<PlayerShop>> findAllShops() {
        return null;
    }

    @Override
    public CompletableFuture<Void> insertTransaction(UUID shopId, UUID playerUuid, String type, int amount, double price) {
        return null;
    }

    @Override
    public CompletableFuture<List<PlayerShopTransaction>> getTransactions(UUID shopId, int limit) {
        return null;
    }

    @Override
    public CompletableFuture<Optional<PlayerShopStorage>> loadStorage(UUID shopId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> saveStorage(PlayerShopStorage storage) {
        return null;
    }

    @Override
    public CompletableFuture<Void> deleteStorage(UUID shopId) {
        return null;
    }

    @Override
    public void shutdown() {

    }
}