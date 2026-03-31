package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.identifiers.keys.ItemKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.item.ItemFactory;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ShopStorageMenu extends Menu {

    private final PlayerShop shop;
    private PlayerShopStorage storage;

    private List<Integer> contentSlots = List.of();

    public ShopStorageMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        contentSlots = MenuKeys.SHOP_STORAGE_SLOTS.getIntList();

        McPlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId(), 54)
                .thenAccept(storage -> {
                    this.storage = storage;
                    McPlayerShop.getScheduler().runTask(super::open);
                });
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int raw = event.getRawSlot();
        int topSize = inventory.getSize();

        Player player = (Player) event.getWhoClicked();

        if (raw >= topSize && event.getClickedInventory() == player.getInventory() && shop.getMode() == ShopMode.SELL) {
            int amount = event.isShiftClick()
                    ? countPlayerItems(player, shop.getItemStack())
                    : 1;

            deposit(player, amount);
            return;
        }

        if (raw >= topSize) return;

        if (ItemKeys.SHOP_STORAGE_BACK.getSlots().contains(raw)) {
            new ShopMainMenu(menuController, shop).open();
            return;
        }

        if (contentSlots.isEmpty()) return;
        if (!contentSlots.contains(raw)) return;

        int storageIndex = contentSlots.indexOf(raw);

        int amount = event.isShiftClick()
                ? getStackAmount(storage.getContents(), storageIndex)
                : 1;

        withdraw(player, storageIndex, amount);
    }

    private void deposit(@NotNull Player player, int amount) {
        if (amount <= 0) return;

        ItemStack shopItem = shop.getItemStack();
        ItemStack[] contents = storage.getContents();

        int available = countPlayerItems(player, shopItem);
        if (available <= 0) return;

        int remaining = Math.min(amount, available);

        for (Integer contentSlot : contentSlots) {
            if (remaining <= 0) break;

            int index = contentSlot;
            ItemStack slotItem = contents[index];

            if (slotItem == null) {
                int take = Math.min(remaining, shopItem.getMaxStackSize());

                ItemStack newItem = shopItem.clone();
                newItem.setAmount(take);

                storage.set(index, newItem);
                remaining -= take;
                continue;
            }

            if (!slotItem.isSimilar(shopItem)) continue;

            int space = slotItem.getMaxStackSize() - slotItem.getAmount();
            if (space <= 0) continue;

            int take = Math.min(space, remaining);

            slotItem.setAmount(slotItem.getAmount() + take);
            storage.set(index, slotItem);

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

        if (contentSlots.isEmpty()) {
            McPlayerShop.getInstance().getLogger().warning("storage-slots is empty! Using fallback.");
            contentSlots = List.of(0,1,2,3,4,5,6,7,8);
        }

        ItemStack[] contents = storage.getContents();

        for (int i = 0; i < contentSlots.size(); i++) {
            if (i >= contents.length) break;

            int slot = contentSlots.get(i);
            inventory.setItem(slot, contents[i]);
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