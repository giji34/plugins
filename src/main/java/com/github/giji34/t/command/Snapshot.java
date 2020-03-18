package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

interface Snapshot {
    @NotNull BlockRange getRange();
    @Nullable BlockData blockAt(Loc loc, Server server);
    @Nullable String getErrorMessage();
}
