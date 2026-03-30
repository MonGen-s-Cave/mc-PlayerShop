package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.identifiers.keys.ItemKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.item.ItemFactory;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopTradeMenu extends Menu {

    private final PlayerShop shop;
    private final Map<Integer, ItemKeys> slotMap = new ConcurrentHashMap<>();

    private int amount = 1;

    public ShopTradeMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        int raw = event.getRawSlot();
        int top = inventory.getSize();

        if (raw < top) {
            event.setCancelled(true);

            ItemKeys key = slotMap.get(raw);
            if (key == null) return;

            switch (key) {
                case SHOP_TRADE_ADD_1 -> modify(+1);
                case SHOP_TRADE_ADD_10 -> modify(+10);
                case SHOP_TRADE_ADD_64 -> modify(+64);

                case SHOP_TRADE_REMOVE_1 -> modify(-1);
                case SHOP_TRADE_REMOVE_10 -> modify(-10);
                case SHOP_TRADE_REMOVE_64 -> modify(-64);

                case SHOP_TRADE_CONFIRM -> confirm();

                case SHOP_TRADE_CLOSE -> menuController.owner().closeInventory();

                default -> {}
            }

            return;
        }

        event.setCancelled(true);
    }

    private void modify(int delta) {
        amount += delta;

        if (amount < 1) amount = 1;
        if (amount > 2304) amount = 2304;

        updateMenuItems();
    }

    private void confirm() {
        Player player = menuController.owner();

        boolean success;

        if (shop.getMode() == ShopMode.SELL) {
            success = McPlayerShop.getInstance().getShopService().buy(shop, player, amount);
        } else {
            success = McPlayerShop.getInstance().getShopService().sell(shop, player, amount);
        }

        if (!success) {
            player.sendMessage(MessageKeys.SHOP_TRANSACTION_FAILED.getMessage());
            return;
        }

        player.sendMessage(MessageKeys.SHOP_TRANSACTION_SUCCESS.getMessage());
        player.closeInventory();
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotMap.clear();

        ItemFactory.setItemsForMenu("shop-trade.items", inventory);

        Map<String, String> replacements = Map.of(
                "{amount}", String.valueOf(amount),
                "{price_total}", String.valueOf(amount * shop.getPrice()),
                "{mode}", shop.getMode().name()
        );

        for (ItemKeys key : ItemKeys.values()) {
            if (!key.name().startsWith("SHOP_TRADE")) continue;

            for (int slot : key.getSlots()) {
                if (slot < 0 || slot >= inventory.getSize()) continue;

                ItemStack base = key.getItem();
                if (base == null) continue;

                ItemStack item = base.clone();
                item.editMeta(meta -> apply(meta, replacements));

                inventory.setItem(slot, item);
                slotMap.put(slot, key);
            }
        }
    }

    private void apply(ItemMeta meta, Map<String, String> replacements) {
        if (meta.getDisplayName() != null) {
            meta.setDisplayName(replace(meta.getDisplayName(), replacements));
        }

        if (meta.getLore() == null) return;

        meta.setLore(meta.getLore().stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    private String replace(String input, Map<String, String> replacements) {
        String out = input;

        for (var e : replacements.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }

        return out;
    }

    @Override public @NotNull String getMenuName() { return MenuKeys.SHOP_TRADE_TITLE.getString(); }
    @Override public int getSlots() { return MenuKeys.SHOP_TRADE_SIZE.getInt(); }
    @Override public int getMenuTick() { return MenuKeys.SHOP_TRADE_TICK.getInt(); }
}