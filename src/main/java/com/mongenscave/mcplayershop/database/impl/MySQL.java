package com.mongenscave.mcplayershop.database.impl;

import com.mongenscave.mcplayershop.database.Database;
import com.mongenscave.mcplayershop.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.PlayerShopTransaction;
import com.mongenscave.mcplayershop.identifiers.ShopMode;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import com.mongenscave.mcplayershop.utils.ItemUtil;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class MySQL implements Database {

    private HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MySQL() {
        setupDataSource();
    }

    private void setupDataSource() {
        HikariConfig hikari = new HikariConfig();

        var mysql = ConfigKeys.DATABASE_MYSQL.getSection();

        hikari.setJdbcUrl("jdbc:mysql://" +
                mysql.getString("host") + ":" +
                mysql.getInt("port") + "/" +
                mysql.getString("database"));

        hikari.setUsername(mysql.getString("username"));
        hikari.setPassword(mysql.getString("password"));

        var pool = ConfigKeys.DATABASE_POOL.getSection();
        hikari.setMaximumPoolSize(pool.getInt("maximumPoolSize", 10));
        hikari.setMinimumIdle(pool.getInt("minimumIdle", 2));
        hikari.setConnectionTimeout(pool.getLong("connectionTimeout", 30000L));
        hikari.setIdleTimeout(pool.getLong("idleTimeout", 600000L));
        hikari.setMaxLifetime(pool.getLong("maxLifetime", 1800000L));

        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource = new HikariDataSource(hikari);
    }

    @Override
    @NotNull
    @Contract("-> new")
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                createTables();
            } catch (Exception exception) {
                LoggerUtils.error("MySQL initialize failed: " + exception.getMessage());
            }
        }, executor);
    }

    private void createTables() throws Exception {
        String shops = """
            CREATE TABLE IF NOT EXISTS playershops (
                shop_id VARCHAR(36) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                item_id MEDIUMTEXT NOT NULL,
                price DOUBLE NOT NULL,
                mode VARCHAR(10) NOT NULL,
                enabled BOOLEAN NOT NULL,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (shop_id),
                UNIQUE KEY unique_location (world, x, y, z),
                INDEX idx_owner (owner_uuid)
            )
        """;

        String transactions = """
            CREATE TABLE IF NOT EXISTS playershop_transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                shop_id VARCHAR(36) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                type VARCHAR(10) NOT NULL,
                amount INT NOT NULL,
                price DOUBLE NOT NULL,
                created_at BIGINT NOT NULL,
                INDEX idx_shop (shop_id),
                INDEX idx_player (player_uuid)
            )
        """;

        String storage = """
            CREATE TABLE IF NOT EXISTS playershop_storage (
                shop_id VARCHAR(36) NOT NULL,
                data MEDIUMTEXT NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (shop_id)
            )
        """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(shops);
            statement.execute(transactions);
            statement.execute(storage);
        }
    }

    @Override
    public CompletableFuture<Void> insertShop(PlayerShop shop) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO playershops (
                    shop_id, owner_uuid,
                    world, x, y, z,
                    item_id, price, mode, enabled,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

                ps.setLong(11, shop.getCreatedAt());
                ps.setLong(12, shop.getUpdatedAt());

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
                    updated_at = ?
                WHERE shop_id = ?
            """;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, shop.getItemId());
                ps.setDouble(2, shop.getPrice());
                ps.setString(3, shop.getMode().name());
                ps.setBoolean(4, shop.isEnabled());
                ps.setLong(5, System.currentTimeMillis());

                ps.setString(6, shop.getShopId().toString());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Update shop failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteShop(UUID shopId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM playershops WHERE shop_id = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, shopId.toString());
                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Delete shop failed: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<PlayerShop>> findByLocation(String world, int x, int y, int z) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM playershops WHERE world=? AND x=? AND y=? AND z=?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) return Optional.empty();

                return Optional.of(map(rs));

            } catch (Exception exception) {
                LoggerUtils.error("Find shop failed: " + exception.getMessage());
                return Optional.empty();
            }

        }, executor);
    }

    @Override
    public CompletableFuture<List<PlayerShop>> findAllShops() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerShop> list = new ArrayList<>();
            String sql = "SELECT * FROM playershops";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(map(rs));
                }

            } catch (Exception exception) {
                LoggerUtils.error("Load shops failed: " + exception.getMessage());
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

    @NotNull
    private PlayerShop map(@NotNull ResultSet rs) throws Exception {
        UUID shopId = UUID.fromString(rs.getString("shop_id"));
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));

        String world = rs.getString("world");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");

        Location location = new Location(Bukkit.getWorld(world), x, y, z);
        return new PlayerShop(
                shopId,
                owner,
                location,
                rs.getString("item_id"),
                rs.getDouble("price"),
                ShopMode.valueOf(rs.getString("mode")),
                rs.getBoolean("enabled"),
                rs.getLong("created_at"),
                rs.getLong("updated_at"));
    }

    public CompletableFuture<Optional<PlayerShopStorage>> loadStorage(UUID shopId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT data FROM playershop_storage WHERE shop_id=?";

            try (Connection con = dataSource.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, shopId.toString());
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) return Optional.empty();

                String data = rs.getString("data");
                ItemStack[] contents = ItemUtil.deserializeInventory(data);

                PlayerShopStorage storage = new PlayerShopStorage(shopId, contents.length);
                System.arraycopy(contents, 0, storage.getContents(), 0, contents.length);

                return Optional.of(storage);

            } catch (Exception e) {
                LoggerUtils.error("Load storage failed: " + e.getMessage());
                return Optional.empty();
            }
        }, executor);
    }

    public CompletableFuture<Void> saveStorage(PlayerShopStorage storage) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO playershop_storage (shop_id, data, updated_at)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE data=?, updated_at=?
            """;

            try (Connection con = dataSource.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                String serialized = ItemUtil.serializeInventory(storage.getContents());

                ps.setString(1, storage.getShopId().toString());
                ps.setString(2, serialized);
                ps.setLong(3, System.currentTimeMillis());

                ps.setString(4, serialized);
                ps.setLong(5, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (Exception e) {
                LoggerUtils.error("Save storage failed: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteStorage(UUID shopId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM playershop_storage WHERE shop_id=?";

            try (Connection con = dataSource.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, shopId.toString());
                ps.executeUpdate();

            } catch (Exception e) {
                LoggerUtils.error("Delete storage failed: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<PlayerShopTransaction>> getTransactions(UUID shopId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerShopTransaction> list = new ArrayList<>();

            String sql = """
                SELECT * FROM playershop_transactions
                WHERE shop_id=?
                ORDER BY created_at DESC
                LIMIT ?
            """;

            try (Connection con = dataSource.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, shopId.toString());
                ps.setInt(2, limit);

                ResultSet rs = ps.executeQuery();

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

            } catch (Exception e) {
                LoggerUtils.error("Load transactions failed: " + e.getMessage());
            }

            return list;
        }, executor);
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