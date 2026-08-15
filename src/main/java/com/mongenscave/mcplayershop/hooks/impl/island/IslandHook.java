package com.mongenscave.mcplayershop.hooks.impl.island;

import com.mongenscave.mcplayershop.identifiers.IslandAccess;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IslandHook {
    @NotNull IslandAccess check(@NotNull Player player, @NotNull Location location);

    boolean isLocked(@NotNull Location location);
}