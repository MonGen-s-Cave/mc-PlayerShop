package com.mongenscave.mcplayershop.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;

@UtilityClass
@SuppressWarnings("deprecation")
public final class ItemUtil {

    public String serialize(ItemStack item) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(output);
            data.writeObject(item);
            data.close();

            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ItemStack deserialize(String data) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream stream = new BukkitObjectInputStream(input);

            return (ItemStack) stream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String serializeInventory(ItemStack[] contents) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(output);

            data.writeInt(contents.length);

            for (ItemStack item : contents) {
                data.writeObject(item);
            }

            data.close();

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public ItemStack[] deserializeInventory(String data) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream stream = new BukkitObjectInputStream(input);

            int size = stream.readInt();
            ItemStack[] contents = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                contents[i] = (ItemStack) stream.readObject();
            }

            stream.close();

            return contents;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}