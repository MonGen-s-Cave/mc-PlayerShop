package com.mongenscave.mcplayershop.hooks;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.hooks.impl.currency.CurrencyManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.CoinsEngineHook;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.LiteEcoHook;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.PlayerPointsHook;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.VaultHook;
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
    }

    private void loadProviders(Section section) {
        if (section == null) return;

        if (section.getBoolean("vault.enabled", false)) {
            if (isPluginPresent("Vault")) {
                var economy = plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

                if (economy != null) {
                    currencyManager.register(new VaultHook(economy.getProvider()));
                    LoggerUtils.info("\u001B[32m   [Hook] Vault successfully enabled.\u001B[0m");
                } else {
                    LoggerUtils.warn("   [Hook] [Hook] Vault found but no Economy provider.");
                }
            } else {
                LoggerUtils.warn("   [Hook] [Hook] Vault not found, skipping.");
            }
        }

        if (section.getBoolean("playerpoints.enabled", false)) {
            if (isPluginPresent("PlayerPoints")) {
                LoggerUtils.info("\u001B[32m   [Hook] PlayerPoints successfully enabled.\u001B[0m");
                currencyManager.register(new PlayerPointsHook());
            } else {
                LoggerUtils.warn("   [Hook] PlayerPoints not found, skipping.");
            }
        }

        if (section.getBoolean("coinsengine.enabled", false)) {
            if (isPluginPresent("CoinsEngine")) {
                LoggerUtils.info("\u001B[32m   [Hook] CoinsEngine successfully enabled.\u001B[0m");
            } else {
                LoggerUtils.warn("   [Hook] CoinsEngine not found, skipping.");
            }
        }

        if (section.getBoolean("liteeco.enabled", false)) {
            if (isPluginPresent("LiteEco")) {
                LoggerUtils.info("\u001B[32m   [Hook] LiteEco successfully enabled.\u001B[0m");
            } else {
                LoggerUtils.warn("   [Hook] LiteEco not found, skipping.");
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

                    currencyManager.register(new CoinsEngineHook(id));
                    LoggerUtils.info("Currency registered: " + key + " (CoinsEngine:" + id + ")");
                }

                case "playerpoints" -> {
                    if (!isPluginPresent("PlayerPoints")) continue;

                    currencyManager.register(new PlayerPointsHook());
                    LoggerUtils.info("Currency registered: " + key + " (PlayerPoints)");
                }

                case "liteeco" -> {
                    if (!isPluginPresent("LiteEco")) continue;

                    String id = plugin.getHooks().getString(base + ".currency-id");
                    if (id == null) {
                        LoggerUtils.warn("Missing currency-id for: " + key);
                        continue;
                    }

                    currencyManager.register(new LiteEcoHook(id));
                    LoggerUtils.info("Currency registered: " + key + " (LiteEco:" + id + ")");
                }
            }
        }
    }

    private boolean isPluginPresent(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }
}