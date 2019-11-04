package com.github.giji34.t;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.UUID;

class ReplaceOperation {
    private HashMap<Loc, Material> ops;
    private UUID worldUUID;

    ReplaceOperation(World world) {
        ops = new HashMap<>();
        worldUUID = world.getUID();
    }

    ReplaceOperation apply(World world) {
        if (!worldUUID.equals(world.getUID())) {
            return null;
        }
        final ReplaceOperation undo = new ReplaceOperation(world);
        ops.forEach((loc, material) -> {
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            Material before = block.getType();
            if (block.getType() != material) {
                block.setType(material);
                undo.register(loc, before);
            }
        });
        return undo;
    }

    void register(Loc loc, Material after) {
        ops.put(loc, after);
    }

    int count() {
        return ops.size();
    }

    boolean isIdenticalWorld(World world) {
        return worldUUID.equals(world.getUID());
    }
}
