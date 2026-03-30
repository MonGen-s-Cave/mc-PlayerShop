package com.mongenscave.mcplayershop.guis.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.guis.Menu;
import com.mongenscave.mcplayershop.identifiers.keys.ItemKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.item.ItemFactory;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShopTransaction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class ShopTransactionsMenu extends Menu {

    private final PlayerShop shop;
    private List<PlayerShopTransaction> transactions = List.of();

    public ShopTransactionsMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        DatabaseManager.getDatabase()
                .getTransactions(shop.getShopId(), 45)
                .thenAccept(list -> {
                    this.transactions = list;
                    McPlayerShop.getScheduler().runTask(super::open);
                });
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        ItemFactory.setItemsForMenu("shop-transactions.items", inventory);

        if (transactions.isEmpty()) {
            ItemStack item = ItemKeys.SHOP_TRANSACTIONS_EMPTY.getItem();

            if (item != null && !ItemKeys.SHOP_TRANSACTIONS_EMPTY.getSlots().isEmpty()) {
                inventory.setItem(ItemKeys.SHOP_TRANSACTIONS_EMPTY.getSlots().getFirst(), item);
            }

            return;
        }

        int slot = 0;

        for (PlayerShopTransaction tx : transactions) {
            if (slot >= inventory.getSize()) break;

            Map<String, String> replacements = Map.of(
                    "{player}", resolveName(tx.playerUuid()),
                    "{amount}", String.valueOf(tx.amount()),
                    "{price}", String.valueOf(tx.price()),
                    "{time}", formatTime(tx.createdAt()),
                    "{type}", tx.type()
            );

            ItemStack base = ItemKeys.SHOP_TRANSACTION_ITEM.getItem();
            if (base == null) continue;

            ItemStack item = base.clone();

            item.editMeta(meta -> applyReplacements(meta, replacements));

            inventory.setItem(slot++, item);
        }
    }

    @NotNull
    private String resolveName(@NotNull UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        String name = player.getName();
        if (name != null) return name;

        return uuid.toString().substring(0, 8);
    }

    @NotNull
    private String formatTime(long time) {
        long diff = System.currentTimeMillis() - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return seconds + "s ago";
    }

    private void applyReplacements(@NotNull ItemMeta meta, @NotNull Map<String, String> replacements) {
        String name = meta.getDisplayName();
        if (!name.isEmpty()) meta.setDisplayName(replace(name, replacements));

        List<String> lore = meta.getLore();

        if (lore == null || lore.isEmpty()) return;

        meta.setLore(lore.stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    @NotNull
    private String replace(@NotNull String input, @NotNull Map<String, String> replacements) {
        String out = input;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return out;
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_TRANSACTIONS_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_TRANSACTIONS_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_TRANSACTIONS_TICK.getInt();
    }
}