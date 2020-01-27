package com.github.giji34.t;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

class ReplaceData {
    final Material material;
    @Nullable
    final String data;

    ReplaceData(Material material, @Nullable String data) {
        this.material = material;
        if (data != null && !data.isEmpty()) {
            this.data = data;
        } else {
            this.data = null;
        }
    }

    ReplaceData(String blockData) {
        int begin = blockData.indexOf("[");
        int end = blockData.indexOf("]");
        if (begin > 0 && end > 0) {
            String name = blockData.substring(0, begin);
            this.material = Material.matchMaterial(name);
            this.data = blockData.substring(begin + 1, end);
        } else {
            this.material = Material.matchMaterial(blockData);
            this.data = null;
        }
    }

    String getAsString() {
        if (this.data == null) {
            return this.material.createBlockData().getAsString(true);
        } else {
            return this.material.createBlockData("[" + this.data + "]").getAsString(true);
        }
    }
}

class ReplaceOperation {
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
