package com.mongenscave.mcplayershop.utils;

import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class StorageUtil {

    public static int count(@NotNull PlayerShopStorage storage, ItemStack target) {
        int total = 0;

        for (ItemStack item : storage.getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(target)) continue;

            total += item.getAmount();
        }

        return total;
    }

    public static void remove(@NotNull PlayerShopStorage storage, ItemStack target, int amount) {
        for (int i = 0; i < storage.getContents().length; i++) {
            ItemStack item = storage.getContents()[i];
            if (item == null) continue;
            if (!item.isSimilar(target)) continue;

            int remove = Math.min(item.getAmount(), amount);

            item.setAmount(item.getAmount() - remove);

            if (item.getAmount() <= 0) storage.set(i, null);

            amount -= remove;
            if (amount <= 0) return;
        }
    }

    public static boolean add(@NotNull PlayerShopStorage storage, ItemStack stack) {
        for (int i = 0; i < storage.getContents().length; i++) {
            ItemStack existing = storage.getContents()[i];

            if (existing == null) continue;
            if (!existing.isSimilar(stack)) continue;

            int max = existing.getMaxStackSize();
            int space = max - existing.getAmount();

            if (space <= 0) continue;

            int move = Math.min(space, stack.getAmount());

            existing.setAmount(existing.getAmount() + move);
            stack.setAmount(stack.getAmount() - move);

            storage.set(i, existing);

            if (stack.getAmount() <= 0) return true;
        }

        for (int i = 0; i < storage.getContents().length; i++) {
            if (storage.getContents()[i] == null) {
                storage.set(i, stack);
                return true;
            }
        }

        return false;
    }

    @Contract(pure = true)
    public static boolean isFull(@NotNull PlayerShopStorage storage) {
        for (ItemStack item : storage.getContents()) {
            if (item == null) return false;
        }

        return true;
    }
}