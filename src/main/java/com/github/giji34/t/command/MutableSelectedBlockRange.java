package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class MutableSelectedBlockRange {
    public Loc start;
    public Loc end;
    private UUID worldUUID;

    MutableSelectedBlockRange() {
    }

    void setStart(Loc start, World world) {
        if (start == null || world == null) {
            return;
        }
        resetWorldUUIDIfNeeded(world);
        this.start = start.clone();
    }

    void setEnd(Loc end, World world) {
        if (end == null || world == null) {
            return;
        }
        resetWorldUUIDIfNeeded(world);
        this.end = end.clone();
    }

    private void resetWorldUUIDIfNeeded(World world) {
        if (worldUUID == null) {
            worldUUID = world.getUID();
        }
        if (!worldUUID.equals(world.getUID())) {
            this.start = null;
            this.end = null;
            worldUUID = world.getUID();
        }
    }

    @Nullable
    SelectedBlockRange isolate() {
        if (this.start == null || this.end == null) {
            return null;
        }
        return new SelectedBlockRange(this.start, this.end);
    }
}
