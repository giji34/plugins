package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

interface Snapshot {
    @NotNull BlockRange getRange();
    @Nullable String blockAt(Loc loc);
    @Nullable String biomeAt(Loc loc);
    Optional<Integer> versionAt(Loc loc);
    @Nullable String getErrorMessage();
}
