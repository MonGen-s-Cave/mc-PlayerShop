package com.mongenscave.mcplayershop.listener;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.impl.ShopMainMenu;
import com.mongenscave.mcplayershop.guis.impl.ShopTradeMenu;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.service.PlayerShopService;
import com.mongenscave.mcplayershop.utils.ShopLimitUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public final class ShopListener implements Listener {

    private final PlayerShopService service = McPlayerShop.getInstance().getShopService();

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARREL) return;

        Player player = event.getPlayer();
        var manager = McPlayerShop.getInstance().getShopManager();

        var optional = manager.get(block.getLocation());

        if (optional.isPresent()) {
            event.setCancelled(true);

            PlayerShop shop = optional.get();

            if (shop.getOwnerUuid().equals(player.getUniqueId())) {
                openOwnerMenu(player, shop);
                return;
            }

            handleCustomerInteract(player, shop);
            return;
        }

        if (!player.isSneaking()) return;

        int limit = ShopLimitUtil.getLimit(player);
        int current = manager.getShopCount(player.getUniqueId());

        if (limit > 0 && current >= limit) {
            player.sendMessage(MessageKeys.SHOP_LIMIT_REACHED.getMessage()
                    .replace("{limit}", String.valueOf(limit))
                    .replace("{current}", String.valueOf(current)));

            event.setCancelled(true);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        service.create(player, block.getLocation(), item);

        event.setCancelled(true);
    }

    private void openOwnerMenu(@NotNull Player player, @NotNull PlayerShop shop) {
        new ShopMainMenu(MenuController.getMenuUtils(player), shop).open();
    }

    private void handleCustomerInteract(@NotNull Player player, @NotNull PlayerShop shop) {
        new ShopTradeMenu(MenuController.getMenuUtils(player), shop).open();
    }

    @EventHandler
    public void onBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARREL) return;

        Player player = event.getPlayer();

        var manager = McPlayerShop.getInstance().getShopManager();
        var optional = manager.get(block.getLocation());

        if (optional.isEmpty()) return;

        PlayerShop shop = optional.get();
        if (!shop.getOwnerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (!player.isSneaking()) {
            event.setCancelled(true);
            return;
        }

        service.remove(player, shop);

        event.setDropItems(false);
    }
}