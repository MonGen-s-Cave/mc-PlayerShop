package com.mongenscave.mcplayershop.database;

import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShopTransaction;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import com.mongenscave.mcplayershop.utils.ItemUtil;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDatabase implements Database {

    protected final HikariDataSource dataSource;
    protected final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    protected AbstractDatabase() {
        this.dataSource = createDataSource();
    }

    protected abstract HikariDataSource createDataSource();

    protected abstract List<String> getTableDefinitions();

    protected abstract String getStorageUpsertSql();

    protected void applyPoolSettings(@NotNull HikariConfig hikari) {
        var pool = ConfigKeys.DATABASE_POOL.getSection();

        hikari.setMaximumPoolSize(pool.getInt("maximumPoolSize", 10));
        hikari.setMinimumIdle(pool.getInt("minimumIdle", 2));
        hikari.setConnectionTimeout(pool.getLong("connectionTimeout", 30000L));
        hikari.setIdleTimeout(pool.getLong("idleTimeout", 600000L));
        hikari.setMaxLifetime(pool.getLong("maxLifetime", 1800000L));

        if (pool.contains("setLeakDetectionThreshold")) {
            hikari.setLeakDetectionThreshold(pool.getLong("setLeakDetectionThreshold"));
        }
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                for (String ddl : getTableDefinitions()) {
                    stmt.execute(ddl);
                }

            } catch (SQLException exception) {
                LoggerUtils.error("Database initialize failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> insertShop(PlayerShop shop) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO playershops (
                    shop_id, owner_uuid,
                    world, x, y, z,
                    item_id, price, mode, enabled,
                    currency_id,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, shop.getShopId().toString());
                ps.setString(2, shop.getOwnerUuid().toString());

                ps.setString(3, shop.getWorld());
                ps.setInt(4, shop.getX());
                ps.setInt(5, shop.getY());
                ps.setInt(6, shop.getZ());

                ps.setString(7, shop.getItemId());
                ps.setDouble(8, shop.getPrice());
                ps.setString(9, shop.getMode().name());
                ps.setBoolean(10, shop.isEnabled());

                ps.setString(11, shop.getCurrencyId());
                ps.setLong(12, shop.getCreatedAt());
                ps.setLong(13, shop.getUpdatedAt());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Insert shop failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateShop(PlayerShop shop) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                UPDATE playershops SET
                    item_id = ?,
                    price = ?,
                    mode = ?,
                    enabled = ?,
                    currency_id = ?,
                    updated_at = ?
                WHERE shop_id = ?
            """;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, shop.getItemId());
                ps.setDouble(2, shop.getPrice());
                ps.setString(3, shop.getMode().name());
                ps.setBoolean(4, shop.isEnabled());
                ps.setString(5, shop.getCurrencyId());
                ps.setLong(6, System.currentTimeMillis());
                ps.setString(7, shop.getShopId().toString());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Update shop failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteShop(UUID shopId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "DELETE FROM playershops WHERE shop_id = ?"
                 )) {

                ps.setString(1, shopId.toString());
                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Delete shop failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<PlayerShop>> findAllShops() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerShop> list = new ArrayList<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT * FROM playershops");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    PlayerShop shop = map(rs);
                    if (shop != null) list.add(shop);
                }

            } catch (Exception exception) {
                LoggerUtils.error("Load shops failed: " + exception.getMessage());
            }

            return list;
        }, executor);
    }

    @Override
    public CompletableFuture<List<PlayerShop>> findShopsByWorld(String world) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerShop> list = new ArrayList<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT * FROM playershops WHERE world = ?"
                 )) {

                ps.setString(1, world);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerShop shop = map(rs);
                        if (shop != null) list.add(shop);
                    }
                }

            } catch (Exception exception) {
                LoggerUtils.error("findShopsByWorld failed: " + exception.getMessage());
            }

            return list;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> insertTransaction(UUID shopId, UUID playerUuid, String type, int amount, double price) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO playershop_transactions (
                    shop_id, player_uuid, type, amount, price, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
            """;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, shopId.toString());
                ps.setString(2, playerUuid.toString());
                ps.setString(3, type);
                ps.setInt(4, amount);
                ps.setDouble(5, price);
                ps.setLong(6, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Insert transaction failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<PlayerShopTransaction>> getTransactions(UUID shopId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerShopTransaction> list = new ArrayList<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("""
                 SELECT * FROM playershop_transactions
                 WHERE shop_id=?
                 ORDER BY created_at DESC
                 LIMIT ?
             """)) {

                ps.setString(1, shopId.toString());
                ps.setInt(2, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new PlayerShopTransaction(
                                UUID.fromString(rs.getString("shop_id")),
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("type"),
                                rs.getInt("amount"),
                                rs.getDouble("price"),
                                rs.getLong("created_at")
                        ));
                    }
                }

            } catch (Exception exception) {
                LoggerUtils.error("Load transactions failed: " + exception.getMessage());
            }

            return list;
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<PlayerShopStorage>> loadStorage(UUID shopId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT data FROM playershop_storage WHERE shop_id=?"
                 )) {

                ps.setString(1, shopId.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();

                    String data = rs.getString("data");
                    ItemStack[] contents = ItemUtil.deserializeInventory(data);

                    PlayerShopStorage storage = new PlayerShopStorage(shopId);

                    for (ItemStack item : contents) {
                        if (item != null) {
                            storage.getContents().add(item);
                        }
                    }

                    return Optional.of(storage);
                }

            } catch (Exception exception) {
                LoggerUtils.error("Load storage failed: " + exception.getMessage());
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveStorage(PlayerShopStorage storage) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(getStorageUpsertSql())) {

                String serialized = ItemUtil.serializeInventory(storage.getContents().toArray(new ItemStack[0]));

                ps.setString(1, storage.getShopId().toString());
                ps.setString(2, serialized);
                ps.setLong(3, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Save storage failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteStorage(UUID shopId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "DELETE FROM playershop_storage WHERE shop_id=?"
                 )) {

                ps.setString(1, shopId.toString());
                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Delete storage failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Nullable
    protected PlayerShop map(@NotNull ResultSet rs) throws Exception {
        UUID shopId = UUID.fromString(rs.getString("shop_id"));
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));

        String worldName = rs.getString("world");
        var world = Bukkit.getWorld(worldName);

        if (world == null) {
            LoggerUtils.warn("World not loaded: " + worldName + " -> skipping shop " + shopId);
            return null;
        }

        Location location = new Location(
                world,
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"));

        String currencyId;
        try {
            currencyId = rs.getString("currency_id");
            if (currencyId == null || currencyId.isEmpty()) {
                currencyId = "vault";
            }
        } catch (Exception ex) {
            currencyId = "vault";
        }

        ShopMode mode;
        try {
            mode = ShopMode.valueOf(rs.getString("mode"));
        } catch (Exception ex) {
            LoggerUtils.warn("Invalid mode for shop " + shopId + " -> default SELL");
            mode = ShopMode.SELL;
        }

        return new PlayerShop(
                shopId,
                owner,
                location,
                rs.getString("item_id"),
                rs.getDouble("price"),
                mode,
                rs.getBoolean("enabled"),
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                currencyId);
    }

    @Override
    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}