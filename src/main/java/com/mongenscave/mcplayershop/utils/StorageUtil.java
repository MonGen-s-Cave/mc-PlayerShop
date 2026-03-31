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

    public static boolean add(@NotNull PlayerShopStorage storage, @NotNull ItemStack stack) {
        int remaining = stack.getAmount();
        int maxStack = stack.getMaxStackSize();

        ItemStack[] contents = storage.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack existing = contents[i];

            if (existing == null) continue;
            if (!existing.isSimilar(stack)) continue;

            int space = maxStack - existing.getAmount();
            if (space <= 0) continue;

            int move = Math.min(space, remaining);

            existing.setAmount(existing.getAmount() + move);
            storage.set(i, existing);

            remaining -= move;
            if (remaining == 0) return true;
        }

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) continue;

            int move = Math.min(maxStack, remaining);

            ItemStack newStack = stack.clone();
            newStack.setAmount(move);

            storage.set(i, newStack);

            remaining -= move;
            if (remaining <= 0) return true;
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