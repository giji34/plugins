package com.github.giji34.t;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;

class ReplaceOperation {
    private HashMap<Loc, Material> ops;

    ReplaceOperation() {
        ops = new HashMap<>();
    }

    ReplaceOperation apply(World world) {
        final ReplaceOperation undo = new ReplaceOperation();
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
}
