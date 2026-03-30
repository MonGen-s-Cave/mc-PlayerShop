package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ShopCurrencyMenu extends Menu {

    private final PlayerShop shop;
    private final List<String> currencies = new ArrayList<>();

    public ShopCurrencyMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        currencies.clear();

        var manager = McPlayerShop.getInstance().getCurrencyManager();

        int slot = 0;

        for (var provider : manager.getAll()) {
            if (slot >= inventory.getSize()) break;

            String id = provider.id();
            currencies.add(id);

            ItemStack item = new ItemStack(Material.GOLD_NUGGET);

            item.editMeta(meta -> {
                meta.setDisplayName("§6" + id);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Click to select");

                if (shop.getCurrencyId().equalsIgnoreCase(id)) {
                    lore.add("§aCurrently selected");
                }

                meta.setLore(lore);
            });

            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        int raw = event.getRawSlot();
        int top = inventory.getSize();

        if (raw < top) {
            event.setCancelled(true);

            if (raw >= currencies.size()) return;

            String selected = currencies.get(raw);

            McPlayerShop.getInstance().getStorageManager()
                    .getOrLoad(shop.getShopId(), 54)
                    .thenAccept(storage -> {
                        if (!isEmpty(storage)) {
                            McPlayerShop.getScheduler().runTask(() ->
                                    menuController.owner().sendMessage("§cStorage must be empty!")
                            );
                            return;
                        }

                        McPlayerShop.getScheduler().runTask(() -> {
                            shop.setCurrencyId(selected);
                            McPlayerShop.getInstance().getShopService().update(shop);

                            menuController.owner().sendMessage("§aCurrency updated to: " + selected);

                            new ShopMainMenu(menuController, shop).open();
                        });
                    });

            return;
        }

        event.setCancelled(true);
    }

    private boolean isEmpty(@NotNull com.mongenscave.mcplayershop.shop.models.PlayerShopStorage storage) {
        for (ItemStack item : storage.getContents()) {
            if (item != null && !item.getType().isAir()) return false;
        }

        return true;
    }

    @Override
    public @NotNull String getMenuName() {
        return "Select Currency";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public int getMenuTick() {
        return 0;
    }
}