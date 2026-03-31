package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.identifiers.keys.ItemKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.item.ItemFactory;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import com.mongenscave.mcplayershop.utils.AmountFormatUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class ShopMainMenu extends Menu {

    private final PlayerShop shop;
    private final Map<Integer, ItemKeys> slotMap = new ConcurrentHashMap<>();

    public ShopMainMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
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
                case SHOP_MAIN_TOGGLE_MODE -> {
                    toggleMode();
                    updateMenuItems();
                }
                case SHOP_MAIN_STORAGE -> new ShopStorageMenu(menuController, shop).open();
                case SHOP_MAIN_CLOSE -> menuController.owner().closeInventory();
                case SHOP_MAIN_TRANSACTIONS -> new ShopTransactionsMenu(menuController, shop).open();
                case SHOP_MAIN_CURRENCY -> new ShopCurrencyMenu(menuController, shop).open();
                case SHOP_MAIN_PRICE_CHANGE -> McPlayerShop.getInstance().getShopPriceService().openEditor(menuController.owner(), shop);
                default -> {}
            }

            return;
        }

        event.setCancelled(true);
    }

    private void toggleMode() {
        McPlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId(), 54)
                .thenAccept(storage -> {
                    if (!isEmpty(storage)) {
                        McPlayerShop.getScheduler().runTask(() -> menuController.owner().sendMessage(MessageKeys.SHOP_STORAGE_NOT_EMPTY.getMessage()));
                        return;
                    }

                    McPlayerShop.getScheduler().runTask(() -> {
                        shop.setMode(shop.getMode() == ShopMode.SELL ? ShopMode.BUY : ShopMode.SELL);
                        McPlayerShop.getInstance().getShopService().update(shop);

                        updateMenuItems();
                    });
                });
    }

    private boolean isEmpty(@NotNull PlayerShopStorage storage) {
        for (ItemStack item : storage.getContents()) {
            if (item != null && !item.getType().isAir()) return false;
        }

        return true;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotMap.clear();

        ItemFactory.setItemsForMenu("shop-main.items", inventory);

        Map<String, String> replacements = Map.of(
                "{mode}", shop.getMode().name(),
                "{currency}", MessageProcessor.process(getCurrencyValue(shop, "display-name")),
                "{price}", AmountFormatUtil.format(shop.getPrice())
        );

        setItem(ItemKeys.SHOP_MAIN_CURRENCY, replacements);
        setItem(ItemKeys.SHOP_MAIN_TOGGLE_MODE, replacements);
        setItem(ItemKeys.SHOP_MAIN_STORAGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_TRANSACTIONS, replacements);
        setItem(ItemKeys.SHOP_MAIN_PRICE_CHANGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_CLOSE, replacements);
    }

    @NotNull
    private String getCurrencyValue(@NotNull PlayerShop shop, @NotNull String key) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = McPlayerShop.getInstance().getHooks().getString(base + "." + key);

        return value != null ? value : "";
    }

    private void setItem(@NotNull ItemKeys key, @NotNull Map<String, String> replacements) {
        for (int slot : key.getSlots()) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            ItemStack item = key.getItem();
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack clone = item.clone();
            clone.editMeta(meta -> apply(meta, replacements));

            inventory.setItem(slot, clone);
            slotMap.put(slot, key);
        }
    }

    private void apply(@NotNull org.bukkit.inventory.meta.ItemMeta meta, Map<String, String> replacements) {
        String name = meta.getDisplayName();
        meta.setDisplayName(replace(name, replacements));

        if (meta.getLore() == null) return;

        meta.setLore(meta.getLore().stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    private String replace(String input, @NotNull Map<String, String> replacements) {
        String out = input;
        for (var e : replacements.entrySet()) out = out.replace(e.getKey(), e.getValue());

        return out;
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_MAIN_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_MAIN_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_MAIN_TICK.getInt();
    }
}