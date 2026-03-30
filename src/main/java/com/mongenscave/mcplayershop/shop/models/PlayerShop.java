package com.mongenscave.mcplayershop.shop.models;

import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.utils.ItemUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public final class PlayerShop {

    private final UUID shopId;
    private final UUID ownerUuid;
    private final Location location;

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    @Setter private String itemId;
    @Setter private double price;
    @Setter private ShopMode mode;

    @Setter private boolean enabled;

    private final long createdAt;
    @Setter private long updatedAt;

    private transient ItemStack cachedItem;

    public PlayerShop(UUID shopId, UUID ownerUuid, @NotNull Location location, String itemId,
                      double price, ShopMode mode, boolean enabled, long createdAt, long updatedAt) {
        this.shopId = shopId;
        this.ownerUuid = ownerUuid;
        this.location = location;

        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();

        this.itemId = itemId;
        this.price = price;
        this.mode = mode;
        this.enabled = enabled;

        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        this.cachedItem = ItemUtil.deserialize(itemId);
    }

    @NotNull
    public ItemStack getItemStack() {
        if (cachedItem == null) cachedItem = ItemUtil.deserialize(itemId);
        return cachedItem.clone();
    }
}