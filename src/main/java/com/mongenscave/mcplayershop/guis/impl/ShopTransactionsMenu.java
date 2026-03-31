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
import com.mongenscave.mcplayershop.utils.AmountFormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class ShopTransactionsMenu extends Menu {

    private final PlayerShop shop;
    private List<PlayerShopTransaction> transactions = List.of();

    private int page = 0;
    private List<Integer> contentSlots = List.of();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public ShopTransactionsMenu(@NotNull MenuController controller, @NotNull PlayerShop shop) {
        super(controller);
        this.shop = shop;
    }

    private void loadSlots() {
        this.contentSlots = MenuKeys.SHOP_TRANSACTIONS_SLOTS.getIntList();
    }

    @Override
    public void open() {
        page = 0;
        loadSlots();

        DatabaseManager.getDatabase()
                .getTransactions(shop.getShopId(), 1000)
                .thenAccept(list -> {
                    this.transactions = list;
                    McPlayerShop.getScheduler().runTask(super::open);
                });
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int raw = event.getRawSlot();
        int topSize = inventory.getSize();

        if (raw >= topSize) return;

        if (ItemKeys.SHOP_TRANSACTIONS_BACK.getSlots().contains(raw)) {
            new ShopMainMenu(menuController, shop).open();
            return;
        }

        if (ItemKeys.SHOP_TRANSACTIONS_NEXT.getSlots().contains(raw)) {
            if (page < getMaxPage()) {
                page++;
                setMenuItems();
            }
            return;
        }

        if (ItemKeys.SHOP_TRANSACTIONS_PREVIOUS.getSlots().contains(raw)) {
            if (page > 0) {
                page--;
                setMenuItems();
            }
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        ItemFactory.setItemsForMenu("shop-transactions.items", inventory);

        if (!transactions.isEmpty()) {
            for (int slot : ItemKeys.SHOP_TRANSACTIONS_EMPTY.getSlots()) {
                inventory.setItem(slot, null);
            }
        }

        if (transactions.isEmpty()) {
            ItemStack item = ItemKeys.SHOP_TRANSACTIONS_EMPTY.getItem();

            if (item != null && !ItemKeys.SHOP_TRANSACTIONS_EMPTY.getSlots().isEmpty()) {
                inventory.setItem(ItemKeys.SHOP_TRANSACTIONS_EMPTY.getSlots().getFirst(), item);
            }

            return;
        }

        page = clampPage(page);

        ItemStack pageItem = ItemKeys.SHOP_TRANSACTIONS_PAGE_INFO.getItem();
        if (pageItem != null && !ItemKeys.SHOP_TRANSACTIONS_PAGE_INFO.getSlots().isEmpty()) {
            ItemStack cloned = pageItem.clone();

            Map<String, String> replacements = Map.of(
                    "{page}", String.valueOf(page + 1),
                    "{max}", String.valueOf(getMaxPage() + 1)
            );

            cloned.editMeta(meta -> applyReplacements(meta, replacements));

            inventory.setItem(ItemKeys.SHOP_TRANSACTIONS_PAGE_INFO.getSlots().getFirst(), cloned);
        }

        if (contentSlots.isEmpty()) {
            contentSlots = List.of(0,1,2,3,4,5,6,7,8);
        }

        int start = page * contentSlots.size();
        int end = Math.min(start + contentSlots.size(), transactions.size());

        int index = 0;

        for (int i = start; i < end; i++) {
            PlayerShopTransaction tx = transactions.get(i);

            Map<String, String> replacements = Map.of(
                    "{player}", resolveName(tx.playerUuid()),
                    "{amount}", AmountFormatUtil.format(tx.amount()),
                    "{price}", String.valueOf(tx.price()),
                    "{time}", formatTransactionTime(tx.createdAt()),
                    "{type}", tx.type()
            );

            ItemStack base = ItemKeys.SHOP_TRANSACTION_ITEM.getItem();
            if (base == null) continue;

            ItemStack item = base.clone();
            item.editMeta(meta -> applyReplacements(meta, replacements));

            if (index >= contentSlots.size()) break;

            int slot = contentSlots.get(index++);
            inventory.setItem(slot, item);
        }
    }

    @NotNull
    private String resolveName(@NotNull UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        String name = player.getName();
        if (name != null) return name;

        return uuid.toString().substring(0, 8);
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
    private String formatTransactionTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(FORMATTER);
    }

    @NotNull
    private String replace(@NotNull String input, @NotNull Map<String, String> replacements) {
        String out = input;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return out;
    }

    private int getMaxPage() {
        if (transactions.isEmpty() || contentSlots.isEmpty()) return 0;
        return (int) Math.ceil((double) transactions.size() / contentSlots.size()) - 1;
    }

    private int clampPage(int page) {
        return Math.max(0, Math.min(page, getMaxPage()));
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