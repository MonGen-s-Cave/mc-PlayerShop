package com.mongenscave.mcplayershop.identifiers.keys;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.item.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ItemKeys {

    SHOP_MAIN_TOGGLE_MODE("shop-main.items.toggle-mode"),
    SHOP_MAIN_STORAGE("shop-main.items.storage"),
    SHOP_MAIN_TRANSACTIONS("shop-main.items.transactions"),
    SHOP_MAIN_CLOSE("shop-main.items.close"),
    SHOP_MAIN_CURRENCY("shop-main.items.currency"),
    SHOP_MAIN_PRICE_CHANGE("shop-main.items.price-change"),

    SHOP_TRANSACTION_ITEM("shop-transactions.items.transaction-item"),
    SHOP_TRANSACTIONS_EMPTY("shop-transactions.items.empty"),
    SHOP_TRANSACTIONS_BACK("shop-transactions.items.back"),
    SHOP_TRANSACTIONS_NEXT("shop-transactions.items.next"),
    SHOP_TRANSACTIONS_PREVIOUS("shop-transactions.items.previous"),
    SHOP_TRANSACTIONS_PAGE_INFO("shop-transactions.items.page-info"),

    SHOP_STORAGE_BACK("shop-storage.items.back"),

    SHOP_CURRENCY_BACK("shop-currency.items.back"),
    SHOP_CURRENCY_TEMPLATE("shop-currency.items.currency-template"),

    SHOP_TRADE_ADD_1("shop-trade.items.add-1"),
    SHOP_TRADE_ADD_10("shop-trade.items.add-10"),
    SHOP_TRADE_ADD_64("shop-trade.items.add-64"),

    SHOP_TRADE_REMOVE_1("shop-trade.items.remove-1"),
    SHOP_TRADE_REMOVE_10("shop-trade.items.remove-10"),
    SHOP_TRADE_REMOVE_64("shop-trade.items.remove-64"),

    SHOP_TRADE_CONFIRM("shop-trade.items.confirm"),
    SHOP_TRADE_CLOSE("shop-trade.items.close");

    private final String path;

    ItemKeys(@NotNull final String path) {
        this.path = path;
    }

    public int getSlot() {
        return McPlayerShop.getInstance().getGuis().getInt(path + ".slot");
    }

    public List<Integer> getSlots() {
        var config = McPlayerShop.getInstance().getGuis();
        Object raw = config.get(path + ".slot");

        if (raw instanceof Integer i) {
            return List.of(i);
        }

        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .toList();
        }

        return List.of();
    }

    public ItemStack getItem() {
        return ItemFactory.createItemFromString(path, McPlayerShop.getInstance().getGuis()).orElse(null);
    }
}