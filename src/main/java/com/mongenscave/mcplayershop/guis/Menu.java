package com.mongenscave.mcplayershop.guis;

import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.service.MenuService;
import com.mongenscave.mcplayershop.processor.MenuProcessor;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("deprecation")
public abstract class Menu implements InventoryHolder {
    public MenuController menuController;
    protected Inventory inventory;

    private MenuProcessor updater;

    public Menu(@NotNull MenuController menuController) {
        this.menuController = menuController;
    }

    public abstract void handleMenu(final @NotNull InventoryClickEvent event);

    public abstract void setMenuItems();

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract int getMenuTick();

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        UUID uuid = menuController.owner().getUniqueId();

        if (!MenuService.canOpen(uuid)) return;
        if (!menuController.canOpen(getClass())) return;

        inventory = Bukkit.createInventory(this, getSlots(), MessageProcessor.process(getMenuName()));

        this.setMenuItems();

        menuController.owner().openInventory(inventory);

        int tick = Math.max(0, getMenuTick());
        if (tick > 0) {
            if (updater == null) updater = new MenuProcessor(this);
            updater.start(tick);
        }
    }

    public void close() {
        if (updater != null) {
            updater.stop();
            updater = null;
        }

        MenuController.remove(menuController.owner());
        inventory = null;
    }

    public void updateMenuItems() {
        if (inventory == null) return;

        setMenuItems();
        menuController.owner().updateInventory();
    }
}
