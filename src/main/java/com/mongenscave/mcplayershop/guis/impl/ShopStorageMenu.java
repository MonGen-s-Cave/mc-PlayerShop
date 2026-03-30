package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.item.ItemFactory;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ShopStorageMenu extends Menu {

    private final PlayerShop shop;
    private PlayerShopStorage storage;

    public ShopStorageMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        McPlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId(), 54)
                .thenAccept(storage -> {
                    this.storage = storage;
                    McPlayerShop.getScheduler().runTask(super::open);
                });
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        int raw = event.getRawSlot();
        int top = inventory.getSize();

        if (raw < top) {
            int amount = event.isShiftClick() ? getStackAmount(storage.getContents(), event.getSlot()) : 1;

            withdraw(player, event.getSlot(), amount);
            return;
        }

        if (shop.getMode() == ShopMode.SELL) {
            int amount = event.isShiftClick() ? countPlayerItems(player, shop.getItemStack()) : 1;
            deposit(player, amount);
        }
    }

    private void deposit(@NotNull Player player, int amount) {
        if (amount <= 0) return;

        ItemStack shopItem = shop.getItemStack();
        ItemStack[] contents = storage.getContents();

        int available = countPlayerItems(player, shopItem);
        if (available <= 0) return;

        int remaining = Math.min(amount, available);

        for (int i = 0; i < contents.length; i++) {
            if (remaining <= 0) break;

            ItemStack slotItem = contents[i];

            if (slotItem == null) {
                int take = Math.min(remaining, shopItem.getMaxStackSize());

                ItemStack newItem = shopItem.clone();
                newItem.setAmount(take);

                storage.set(i, newItem);
                remaining -= take;
                continue;
            }

            if (!slotItem.isSimilar(shopItem)) continue;

            int space = slotItem.getMaxStackSize() - slotItem.getAmount();
            if (space <= 0) continue;

            int take = Math.min(space, remaining);

            slotItem.setAmount(slotItem.getAmount() + take);
            storage.set(i, slotItem);

            remaining -= take;
        }

        int moved = Math.min(amount, available) - remaining;
        if (moved <= 0) return;

        removeExact(player, shopItem, moved);

        McPlayerShop.getInstance().getStorageManager().saveAsync(storage);
        updateMenuItems();
    }

    private void withdraw(@NotNull Player player, int slot, int amount) {
        ItemStack[] contents = storage.getContents();

        if (slot < 0 || slot >= contents.length) return;

        ItemStack item = contents[slot];
        if (item == null) return;

        int take = Math.min(amount, item.getAmount());

        ItemStack give = item.clone();
        give.setAmount(take);

        if (!player.getInventory().addItem(give).isEmpty()) return;

        if (item.getAmount() <= take) {
            storage.set(slot, null);
        } else {
            item.setAmount(item.getAmount() - take);
            storage.set(slot, item);
        }

        McPlayerShop.getInstance().getStorageManager().saveAsync(storage);
        updateMenuItems();
    }

    private void removeExact(@NotNull Player player, ItemStack base, int amount) {
        int remaining = amount;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;
            if (!content.isSimilar(base)) continue;

            int take = Math.min(remaining, content.getAmount());

            content.setAmount(content.getAmount() - take);
            remaining -= take;

            if (remaining <= 0) return;
        }
    }

    private int countPlayerItems(@NotNull Player player, @NotNull ItemStack base) {
        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(base)) continue;

            total += item.getAmount();
        }

        return total;
    }

    private int getStackAmount(ItemStack[] contents, int slot) {
        if (slot < 0 || slot >= contents.length) return 0;

        ItemStack item = contents[slot];
        return item == null ? 0 : item.getAmount();
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        ItemFactory.setItemsForMenu("shop-storage.items", inventory);

        ItemStack[] contents = storage.getContents();

        for (int i = 0; i < contents.length; i++) {
            inventory.setItem(i, contents[i]);
        }
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_STORAGE_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_STORAGE_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_STORAGE_TICK.getInt();
    }
}