package com.mongenscave.mcplayershop.database.impl;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.database.Database;
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
import org.jetbrains.annotations.Contract;
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

public final class H2 implements Database {

    private HikariDataSource dataSource;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public H2() {
        setupDataSource();
    }

    private void setupDataSource() {
        HikariConfig hikari = new HikariConfig();

        String dbPath = McPlayerShop.getInstance().getDataFolder().getAbsolutePath() + "/data";
        hikari.setJdbcUrl("jdbc:h2:file:" + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
        hikari.setDriverClassName("org.h2.Driver");

        var pool = ConfigKeys.DATABASE_POOL.getSection();
        hikari.setMaximumPoolSize(pool.getInt("maximumPoolSize", 10));
        hikari.setMinimumIdle(pool.getInt("minimumIdle", 2));
        hikari.setConnectionTimeout(pool.getLong("connectionTimeout", 30000L));
        hikari.setIdleTimeout(pool.getLong("idleTimeout", 600000L));
        hikari.setMaxLifetime(pool.getLong("maxLifetime", 1800000L));

        if (pool.contains("setLeakDetectionThreshold")) {
            hikari.setLeakDetectionThreshold(pool.getLong("setLeakDetectionThreshold"));
        }

        hikari.setAutoCommit(true);
        hikari.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikari.addDataSourceProperty("useLocalSessionState", "true");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");

        this.dataSource = new HikariDataSource(hikari);
    }

    @NotNull
    @Contract(" -> new")
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                createTables();
            } catch (SQLException exception) {
                LoggerUtils.error(exception.getMessage());
            }
        }, virtualThreadExecutor);
    }

    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS playershops (
                shop_id VARCHAR(36) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                item_id CLOB NOT NULL,
                price DOUBLE NOT NULL,
                mode VARCHAR(10) NOT NULL,
                enabled BOOLEAN NOT NULL,
                currency_id VARCHAR(32) NOT NULL DEFAULT 'vault',
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (shop_id),
                UNIQUE KEY unique_location (world, x, y, z),
                INDEX idx_owner (owner_uuid),
                INDEX idx_world (world)
            )
        """);

            stmt.execute("""
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
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS playershop_storage (
                shop_id VARCHAR(36) NOT NULL,
                data CLOB NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (shop_id)
            )
        """);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
    }

    @Override
    public CompletableFuture<Void> saveStorage(PlayerShopStorage storage) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                MERGE INTO playershop_storage (shop_id, data, updated_at)
                KEY(shop_id)
                VALUES (?, ?, ?)
            """;

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                String serialized = ItemUtil.serializeInventory(storage.getContents().toArray(new ItemStack[0]));

                ps.setString(1, storage.getShopId().toString());
                ps.setString(2, serialized);
                ps.setLong(3, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (Exception exception) {
                LoggerUtils.error("Save storage failed: " + exception.getMessage());
            }
        }, virtualThreadExecutor);
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
        }, virtualThreadExecutor);
    }

    @Nullable
    private PlayerShop map(@NotNull ResultSet rs) throws Exception {
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
            virtualThreadExecutor.shutdown();
            if (!virtualThreadExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}