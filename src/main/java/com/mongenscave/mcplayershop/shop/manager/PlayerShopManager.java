package com.mongenscave.mcplayershop.shop.manager;

import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerShopManager {

    private final Map<String, PlayerShop> shops = new ConcurrentHashMap<>();

    @NotNull
    private static String key(@NotNull Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @NotNull
    public Optional<PlayerShop> get(Location location) {
        return Optional.ofNullable(shops.get(key(location)));
    }

    public void register(PlayerShop shop) {
        shops.put(key(shop.getLocation()), shop);
    }

    public void unregister(Location location) {
        shops.remove(key(location));
    }

    @NotNull
    public @UnmodifiableView Collection<PlayerShop> getAll() {
        return Collections.unmodifiableCollection(shops.values());
    }

    @NotNull
    public List<PlayerShop> getByChunk(@NotNull Chunk chunk) {
        List<PlayerShop> result = new ArrayList<>();

        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (PlayerShop shop : shops.values()) {
            if (!shop.getWorld().equals(worldName)) continue;
            if ((shop.getX() >> 4) != chunkX) continue;
            if ((shop.getZ() >> 4) != chunkZ) continue;

            result.add(shop);
        }

        return result;
    }

    public int getShopCount(UUID owner) {
        int count = 0;

        for (PlayerShop shop : shops.values()) {
            if (shop.getOwnerUuid().equals(owner)) {
                count++;
            }
        }

        return count;
    }
}