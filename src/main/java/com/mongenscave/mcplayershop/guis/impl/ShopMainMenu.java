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
import com.mongenscave.mcplayershop.utils.SoundUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public void open() {
        super.open();
        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_OPEN.getString());
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
                case SHOP_MAIN_SELL_MODE, SHOP_MAIN_BUY_MODE -> toggleMode();

                case SHOP_MAIN_STORAGE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopStorageMenu(menuController, shop).open();
                }

                case SHOP_MAIN_CLOSE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    menuController.owner().closeInventory();
                }

                case SHOP_MAIN_TRANSACTIONS -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopTransactionsMenu(menuController, shop).open();
                }

                case SHOP_MAIN_CURRENCY -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopCurrencyMenu(menuController, shop).open();
                }

                case SHOP_MAIN_PRICE_CHANGE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    McPlayerShop.getInstance().getShopPriceService().openEditor(menuController.owner(), shop);
                }

                default -> {}
            }

            return;
        }

        event.setCancelled(true);
    }

    private void toggleMode() {
        McPlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId())
                .thenAccept(storage -> {
                    if (!isEmpty(storage)) {
                        McPlayerShop.getScheduler().runTask(() -> {
                            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ERROR.getString());
                            menuController.owner().sendMessage(MessageKeys.SHOP_STORAGE_NOT_EMPTY.getMessage());
                        });
                        return;
                    }

                    McPlayerShop.getScheduler().runTask(() -> {
                        shop.setMode(shop.getMode() == ShopMode.SELL ? ShopMode.BUY : ShopMode.SELL);
                        McPlayerShop.getInstance().getShopService().update(shop);

                        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());

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
                "{mode}", resolveMode(shop),
                "{currency}", MessageProcessor.process(getCurrencyValue(shop)),
                "{price}", AmountFormatUtil.format(shop.getPrice())
        );

        setItem(ItemKeys.SHOP_MAIN_CURRENCY, replacements);
        setItem(ItemKeys.SHOP_MAIN_STORAGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_TRANSACTIONS, replacements);
        setItem(ItemKeys.SHOP_MAIN_PRICE_CHANGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_CLOSE, replacements);

        setModeItem(replacements);
    }

    private void setModeItem(@NotNull Map<String, String> replacements) {
        if (shop.getMode() == ShopMode.SELL) {
            setSingleItem("shop-main.sell-mode", ItemKeys.SHOP_MAIN_SELL_MODE, replacements);
        } else {
            setSingleItem("shop-main.buy-mode", ItemKeys.SHOP_MAIN_BUY_MODE, replacements);
        }
    }

    private void setSingleItem(@NotNull String path, @NotNull ItemKeys key, @NotNull Map<String, String> replacements) {
        Section section = McPlayerShop.getInstance().getGuis().getSection(path);
        if (section == null) return;

        Optional<ItemStack> item = ItemFactory.buildItem(section, path);

        if (item.isEmpty() || item.get().getType() == Material.AIR) return;

        ItemStack clone = item.get().clone();
        clone.editMeta(meta -> apply(meta, replacements));

        List<Integer> itemSlot;
        if (shop.getMode() == ShopMode.BUY) {
            itemSlot = McPlayerShop.getInstance().getGuis().getIntList("shop-main.buy-mode.slot");
        } else {
            itemSlot = McPlayerShop.getInstance().getGuis().getIntList("shop-main.sell-mode.slot");
        }

        for (int slot : itemSlot) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            inventory.setItem(slot, clone);
            slotMap.put(slot, key);
        }
    }

    @NotNull
    private String getCurrencyValue(@NotNull PlayerShop shop) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = McPlayerShop.getInstance().getHooks().getString(base + "." + "display-name");

        return value != null ? value : "";
    }

    @NotNull
    private String resolveMode(@NotNull PlayerShop shop) {
        return shop.getMode() == ShopMode.SELL
                ? MessageKeys.SHOP_MODE_SELL.getMessage()
                : MessageKeys.SHOP_MODE_BUY.getMessage();
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