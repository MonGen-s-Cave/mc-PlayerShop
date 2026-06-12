package com.mongenscave.mcplayershop.shop.service;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
import com.mongenscave.mcplayershop.identifiers.ShopTransactionResult;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopManager;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopStorageManager;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import com.mongenscave.mcplayershop.utils.ItemUtil;
import com.mongenscave.mcplayershop.utils.StorageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerShopService {

    private final McPlayerShop plugin = McPlayerShop.getInstance();
    private final PlayerShopManager manager = plugin.getShopManager();
    private final ShopVisualService visuals = plugin.getVisualService();
    private final PlayerShopStorageManager storageManager = plugin.getStorageManager();

    public void create(@NotNull Player player, @NotNull Location location, @NotNull ItemStack item) {
        ItemStack base = item.clone();
        base.setAmount(1);

        String defaultCurrency = plugin.getHooks().getString("hooks.currency.default", "vault");

        PlayerShop shop = new PlayerShop(
                UUID.randomUUID(),
                player.getUniqueId(),
                location,
                ItemUtil.serialize(base),
                0,
                ShopMode.SELL,
                false,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                defaultCurrency
        );

        manager.register(shop);
        DatabaseManager.getDatabase().insertShop(shop);

        setupBarrel(location);
        visuals.spawn(shop);
    }


    public void setupBarrel(@NotNull Location location) {
        Block block = location.getBlock();

        if (!(block.getBlockData() instanceof Barrel barrel)) return;

        barrel.setFacing(BlockFace.UP);
        barrel.setOpen(true);

        block.setBlockData(barrel, true);
    }

    public void remove(@NotNull Player player, @NotNull PlayerShop shop) {
        if (!shop.getOwnerUuid().equals(player.getUniqueId())) return;

        UUID shopId = shop.getShopId();

        manager.unregister(shop.getLocation());
        visuals.remove(shopId);

        storageManager.getOrLoad(shopId).thenAccept(storage -> {
            DatabaseManager.getDatabase().deleteShop(shopId);
            storageManager.remove(shopId);

            McPlayerShop.getScheduler().runTask(() -> {
                player.getInventory().addItem(new ItemStack(Material.BARREL));

                for (ItemStack item : storage.getContents()) {
                    if (item != null) player.getInventory().addItem(item);
                }
            });
        });
    }

    public void update(@NotNull PlayerShop shop) {
        shop.setUpdatedAt(System.currentTimeMillis());
        DatabaseManager.getDatabase().updateShop(shop);
        plugin.getVisualService().update(shop);
    }

    public @NotNull ShopTransactionResult buy(@NotNull PlayerShop shop, Player buyer, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId());

        ItemStack shopItem = shop.getItemStack();
        int available = StorageUtil.count(storage, shopItem);

        if (available < amount) return ShopTransactionResult.SHOP_EMPTY;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return ShopTransactionResult.CURRENCY_ERROR;

        double total = shop.getPrice() * amount;

        if (!currency.has(buyer, total)) return ShopTransactionResult.NOT_ENOUGH_MONEY;

        ItemStack give = shopItem.clone();
        give.setAmount(amount);

        if (!buyer.getInventory().addItem(give).isEmpty()) {
            return ShopTransactionResult.INVENTORY_FULL;
        }

        if (!currency.withdraw(buyer, total)) {
            buyer.getInventory().removeItem(give);
            return ShopTransactionResult.CURRENCY_ERROR;
        }

        Player owner = Bukkit.getPlayer(shop.getOwnerUuid());
        if (owner != null) currency.deposit(owner, total);

        StorageUtil.remove(storage, shopItem, amount);
        storageManager.saveAsync(storage);

        visuals.update(shop);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                buyer.getUniqueId(),
                "BUY",
                amount,
                total
        );

        return ShopTransactionResult.SUCCESS;
    }

    public @NotNull ShopTransactionResult sell(@NotNull PlayerShop shop, Player player, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId());

        int capacity = StorageUtil.getRemainingCapacity(storage);
        if (capacity < amount) return ShopTransactionResult.STORAGE_FULL;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return ShopTransactionResult.CURRENCY_ERROR;

        double total = shop.getPrice() * amount;

        Player owner = Bukkit.getPlayer(shop.getOwnerUuid());
        if (owner != null && !currency.has(owner, total)) return ShopTransactionResult.OWNER_NO_MONEY;

        ItemStack base = shop.getItemStack();
        if (!player.getInventory().containsAtLeast(base, amount)) return ShopTransactionResult.NOT_ENOUGH_ITEMS;

        ItemStack toStore = base.clone();
        toStore.setAmount(amount);

        if (!StorageUtil.add(storage, toStore)) return ShopTransactionResult.STORAGE_FULL;

        if (!removeExact(player, base, amount)) {
            StorageUtil.remove(storage, base, amount);
            return ShopTransactionResult.NOT_ENOUGH_ITEMS;
        }

        if (owner != null) {
            if (!currency.withdraw(owner, total)) {
                StorageUtil.remove(storage, base, amount);
                return ShopTransactionResult.OWNER_NO_MONEY;
            }
        }

        currency.deposit(player, total);

        storageManager.saveAsync(storage);

        visuals.update(shop);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                player.getUniqueId(),
                "SELL",
                amount,
                total
        );

        return ShopTransactionResult.SUCCESS;
    }

    private boolean removeExact(@NotNull Player player, ItemStack base, int amount) {
        int remaining = amount;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;

            if (!content.isSimilar(base)) continue;

            int take = Math.min(remaining, content.getAmount());

            content.setAmount(content.getAmount() - take);
            remaining -= take;

            if (remaining <= 0) break;
        }

        return remaining <= 0;
    }
}