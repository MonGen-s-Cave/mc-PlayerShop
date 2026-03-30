package com.mongenscave.mcplayershop.shop.service;

import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.utils.ItemUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class ShopVisualService {

    private final Map<UUID, List<Entity>> visuals = new ConcurrentHashMap<>();

    public void spawn(@NotNull PlayerShop shop) {
        Location base = shop.getLocation().clone().add(0.5, 1.1, 0.5);
        ItemStack stack = ItemUtil.deserialize(shop.getItemId());

        Location textLoc = base.clone();

        TextDisplay text = textLoc.getWorld().spawn(textLoc, TextDisplay.class);
        text.setText(buildText(stack, shop));
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        text.setShadowed(true);
        text.setBillboard(Display.Billboard.VERTICAL);
        text.setPersistent(false);

        Location itemLoc = base.clone().add(0, 0.75, 0);

        ItemDisplay item = itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class);
        item.setItemStack(stack);
        item.setBillboard(Display.Billboard.VERTICAL);
        item.setPersistent(false);

        item.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                item.getTransformation().getLeftRotation(),
                new Vector3f(0.3f, 0.3f, 0.3f),
                item.getTransformation().getRightRotation()
        ));

        visuals.put(shop.getShopId(), List.of(item, text));
    }

    public void remove(UUID shopId) {
        List<Entity> list = visuals.remove(shopId);
        if (list == null) return;

        list.forEach(Entity::remove);
    }

    @NotNull
    private String buildText(@NotNull ItemStack item, @NotNull PlayerShop shop) {
        String name;
        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            name = meta.getDisplayName();
        } else {
            name = formatMaterial(item.getType().name());
        }

        return "§a" + name + "\n§e" + shop.getPrice() + "$";
    }

    @NotNull
    private String formatMaterial(@NotNull String material) {
        String lower = material.toLowerCase().replace("_", " ");
        String[] parts = lower.split(" ");

        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }

    public void removeAll() {
        visuals.values().forEach(list -> list.forEach(Entity::remove));
        visuals.clear();
    }
}