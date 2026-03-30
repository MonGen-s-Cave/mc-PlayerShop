package com.mongenscave.mcplayershop.shop.manager;

import com.mongenscave.mcplayershop.database.Database;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerShopStorageManager {

    private final Map<UUID, PlayerShopStorage> cache = new ConcurrentHashMap<>();
    private final Database repository = DatabaseManager.getDatabase();

    public CompletableFuture<PlayerShopStorage> getOrLoad(UUID shopId, int size) {
        PlayerShopStorage cached = cache.get(shopId);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return repository.loadStorage(shopId).thenApply(optional ->
                cache.computeIfAbsent(shopId, id -> optional.orElseGet(() -> new PlayerShopStorage(id, size))));
    }

    public PlayerShopStorage getOrLoadSync(UUID shopId, int size) {
        return cache.computeIfAbsent(shopId, id -> repository.loadStorage(id).join()
                .orElse(new PlayerShopStorage(id, size)));
    }

    public void saveAsync(PlayerShopStorage storage) {
        repository.saveStorage(storage);
    }

    public void remove(UUID shopId) {
        cache.remove(shopId);
        repository.deleteStorage(shopId);
    }

    @NotNull
    @Contract(pure = true)
    public Collection<PlayerShopStorage> getAll() {
        return cache.values();
    }
}