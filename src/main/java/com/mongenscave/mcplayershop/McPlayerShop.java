package com.mongenscave.mcplayershop;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.database.DatabaseManager;
import com.mongenscave.mcplayershop.hooks.HookManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.CurrencyManager;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopManager;
import com.mongenscave.mcplayershop.shop.service.PlayerShopService;
import com.mongenscave.mcplayershop.shop.service.ShopLoader;
import com.mongenscave.mcplayershop.shop.service.ShopVisualService;
import com.mongenscave.mcplayershop.shop.manager.PlayerShopStorageManager;
import com.mongenscave.mcplayershop.shop.models.PlayerShopStorage;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import com.mongenscave.mcplayershop.utils.RegisterUtils;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class McPlayerShop extends ZapperJavaPlugin {

    @Getter private static McPlayerShop instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private Config language;
    @Getter private Config guis;
    @Getter private Config hooks;
    Config config;

    @Getter private PlayerShopManager shopManager;
    @Getter private ShopVisualService visualService;
    @Getter private PlayerShopService shopService;
    @Getter private PlayerShopStorageManager storageManager;
    @Getter private CurrencyManager currencyManager;
    @Getter private HookManager hookManager;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        initializeComponents();

        DatabaseManager.initialize();

        shopManager = new PlayerShopManager();
        visualService = new ShopVisualService();
        storageManager = new PlayerShopStorageManager();
        shopService = new PlayerShopService();
        currencyManager = new CurrencyManager();
        hookManager = new HookManager();

        ShopLoader.load(shopManager, visualService);

        RegisterUtils.registerCommands();
        RegisterUtils.registerListeners();

        LoggerUtils.printStartup();
        hookManager.load();
    }

    @Override
    public void onDisable() {
        if (visualService != null) visualService.removeAll();

        for (PlayerShopStorage storage : storageManager.getAll()) {
            storageManager.saveAsync(storage);
        }

        DatabaseManager.getDatabase().shutdown();
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .setVersioning(new BasicVersioning("version"))
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
        guis = loadConfig("guis.yml", generalSettings, loaderSettings, updaterSettings);
        hooks = loadConfig("hooks.yml", generalSettings, loaderSettings, updaterSettings);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}