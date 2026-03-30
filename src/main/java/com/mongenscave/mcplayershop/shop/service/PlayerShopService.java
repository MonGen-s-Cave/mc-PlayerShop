package com.mongenscave.mcplayershop.shop.service;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.Currency;
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


    private void setupBarrel(@NotNull Location location) {
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

        storageManager.getOrLoad(shopId, 54).thenAccept(storage -> {
            DatabaseManager.getDatabase().deleteShop(shopId);
            storageManager.remove(shopId);

            McPlayerShop.getScheduler().runTask(() -> {
                player.getInventory().addItem(new ItemStack(Material.BARREL));

                ItemStack shopItem = ItemUtil.deserialize(shop.getItemId());
                player.getInventory().addItem(shopItem);

                for (ItemStack item : storage.getContents()) {
                    if (item != null) player.getInventory().addItem(item);
                }
            });
        });
    }

    public void update(@NotNull PlayerShop shop) {
        shop.setUpdatedAt(System.currentTimeMillis());
        DatabaseManager.getDatabase().updateShop(shop);
    }

    public boolean buy(@NotNull PlayerShop shop, Player buyer, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId(), 54);

        ItemStack shopItem = shop.getItemStack();
        int available = StorageUtil.count(storage, shopItem);

        if (available < amount) return false;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return false;

        double total = shop.getPrice() * amount;

        if (!currency.has(buyer, total)) return false;

        ItemStack give = shopItem.clone();
        give.setAmount(amount);

        if (!buyer.getInventory().addItem(give).isEmpty()) {
            return false;
        }

        if (!currency.withdraw(buyer, total)) {
            buyer.getInventory().removeItem(give);
            return false;
        }

        Player owner = Bukkit.getPlayer(shop.getOwnerUuid());
        if (owner != null) currency.deposit(owner, total);

        StorageUtil.remove(storage, shopItem, amount);
        storageManager.saveAsync(storage);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                buyer.getUniqueId(),
                "BUY",
                amount,
                total
        );

        return true;
    }

    public boolean sell(@NotNull PlayerShop shop, Player player, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId(), 54);

        if (StorageUtil.isFull(storage)) return false;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return false;

        double total = shop.getPrice() * amount;

        Player owner = Bukkit.getPlayer(shop.getOwnerUuid());
        if (owner != null && !currency.has(owner, total)) return false;

        ItemStack base = shop.getItemStack();
        if (!player.getInventory().containsAtLeast(base, amount)) return false;

        ItemStack toStore = base.clone();
        toStore.setAmount(amount);

        if (!StorageUtil.add(storage, toStore)) return false;

        if (!removeExact(player, base, amount)) {
            StorageUtil.remove(storage, base, amount);
            return false;
        }

        if (owner != null) {
            if (!currency.withdraw(owner, total)) {
                StorageUtil.remove(storage, base, amount);
                return false;
            }
        }

        currency.deposit(player, total);

        storageManager.saveAsync(storage);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                player.getUniqueId(),
                "SELL",
                amount,
                total);

        return true;
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