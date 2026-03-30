package com.mongenscave.mcplayershop.database;

import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShopTransaction;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    CompletableFuture<Void> initialize();

    CompletableFuture<Void> insertShop(PlayerShop shop);
    CompletableFuture<Void> updateShop(PlayerShop shop);
    CompletableFuture<Void> deleteShop(UUID shopId);

    CompletableFuture<Optional<PlayerShop>> findByLocation(String world, int x, int y, int z);
    CompletableFuture<List<PlayerShop>> findAllShops();

    CompletableFuture<Void> insertTransaction(UUID shopId, UUID playerUuid, String type, int amount, double price);
    CompletableFuture<List<PlayerShopTransaction>> getTransactions(UUID shopId, int limit);

    CompletableFuture<Optional<PlayerShopStorage>> loadStorage(UUID shopId);
    CompletableFuture<Void> saveStorage(PlayerShopStorage storage);
    CompletableFuture<Void> deleteStorage(UUID shopId);

    void shutdown();
}