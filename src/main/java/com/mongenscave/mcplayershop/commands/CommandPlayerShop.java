package com.mongenscave.mcplayershop.commands;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.utils.AmountFormatUtil;
import com.mongenscave.mcplayershop.utils.TimeFormatUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"mcplayershop", "playershop", "mc-playershop"})
@CommandPermission("mcplayershop.admin")
@SuppressWarnings("unused")
public class CommandPlayerShop {

    private final McPlayerShop plugin = McPlayerShop.getInstance();

    @Subcommand("reload")
    private void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        plugin.getGuis().reload();
        plugin.getHooks().reload();

        plugin.getHookManager().load();

        AmountFormatUtil.reload();
        TimeFormatUtil.reload();

        plugin.getShopManager().getAll().forEach(playerShop -> plugin.getVisualService().update(playerShop));

        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }
}
