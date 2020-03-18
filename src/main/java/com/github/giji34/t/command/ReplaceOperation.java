package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class ReplaceOperation {
    private HashMap<Loc, ReplaceData> ops;
    private UUID worldUUID;

    ReplaceOperation(World world) {
        ops = new HashMap<>();
        worldUUID = world.getUID();
    }

    @Nullable
    ReplaceOperation apply(Server server, World world, boolean applyPhisics) {
        if (!worldUUID.equals(world.getUID())) {
            return null;
        }
        final ReplaceOperation undo = new ReplaceOperation(world);
        ops.forEach((loc, data) -> {
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            BlockData after = server.createBlockData(data.getAsString());
            if (block.getBlockData().matches(after)) {
                return;
            }
            ReplaceData d = new ReplaceData(block.getBlockData().getAsString(true));
            block.setBlockData(after, applyPhisics);
            undo.register(loc, d);
        });
        return undo;
    }

    void register(Loc loc, ReplaceData data) {
        ops.put(loc, data);
    }

    int count() {
        return ops.size();
    }

    boolean isIdenticalWorld(World world) {
        return worldUUID.equals(world.getUID());
    }

    void clear() {
        ops.clear();
    }
}
