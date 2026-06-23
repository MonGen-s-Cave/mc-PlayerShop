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
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeObject(item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    public ItemStack deserialize(String data) {
        ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));

        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(input)) {
            return (ItemStack) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String serializeInventory(@NotNull ItemStack[] contents) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeInt(contents.length);

            for (ItemStack item : contents) {
                data.writeObject(item);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @NotNull
    public ItemStack[] deserializeInventory(String data) {
        ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));

        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(input)) {
            int size = stream.readInt();
            ItemStack[] contents = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                contents[i] = (ItemStack) stream.readObject();
            }

            return contents;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}