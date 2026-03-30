package com.mongenscave.mcplayershop.hooks;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.hooks.impl.currency.CurrencyManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.CoinsEngineCurrencyProvider;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.VaultCurrencyProvider;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class HookManager {

    private final McPlayerShop plugin = McPlayerShop.getInstance();
    private final CurrencyManager currencyManager = plugin.getCurrencyManager();

    public void load() {
        Section root = plugin.getHooks().getSection("hooks.currency");

        if (root == null) {
            LoggerUtils.warn("Hooks config missing 'hooks.currency' section.");
            return;
        }

        loadProviders(root.getSection("providers"));
        loadCurrencies(root.getSection("currencies"));

        LoggerUtils.info("Currency hooks loaded: " + currencyManager.getAll().size());
    }

    private void loadProviders(Section section) {
        if (section == null) return;

        if (section.getBoolean("vault.enabled", false)) {
            if (isPluginPresent("Vault")) {
                var economy = plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

                if (economy != null) {
                    currencyManager.register(new VaultCurrencyProvider(economy.getProvider()));
                    LoggerUtils.info("Hook enabled: Vault");
                } else {
                    LoggerUtils.warn("Vault found but no Economy provider.");
                }
            } else {
                LoggerUtils.warn("Vault not found, skipping.");
            }
        }

        if (section.getBoolean("coinsengine.enabled", false)) {
            if (isPluginPresent("CoinsEngine")) {
                LoggerUtils.info("Hook enabled: CoinsEngine");
            } else {
                LoggerUtils.warn("CoinsEngine not found, skipping.");
            }
        }
    }

    private void loadCurrencies(Section section) {
        if (section == null) return;

        for (String key : section.getRoutesAsStrings(false)) {
            String base = "currencies." + key;

            String provider = plugin.getHooks().getString(base + ".provider");

            if (provider == null) continue;

            switch (provider.toLowerCase()) {

                case "vault" -> {
                    if (currencyManager.get("vault") == null) continue;

                    LoggerUtils.info("Currency registered: " + key + " (Vault)");
                }

                case "coinsengine" -> {
                    if (!isPluginPresent("CoinsEngine")) continue;

                    String id = plugin.getHooks().getString(base + ".currency-id");

                    if (id == null) {
                        LoggerUtils.warn("Missing currency-id for: " + key);
                        continue;
                    }

                    currencyManager.register(new CoinsEngineCurrencyProvider(id));
                    LoggerUtils.info("Currency registered: " + key + " (CoinsEngine:" + id + ")");
                }
            }
        }
    }

    private boolean isPluginPresent(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }
}