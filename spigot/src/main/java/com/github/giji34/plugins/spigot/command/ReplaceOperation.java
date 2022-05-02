package com.github.giji34.plugins.spigot.command;

import com.github.giji34.plugins.spigot.BiomeHelper;
import com.github.giji34.plugins.spigot.BoundingBox;
import com.github.giji34.plugins.spigot.DynmapSupport;
import com.github.giji34.plugins.spigot.Loc;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class ReplaceOperation {
  private HashMap<Loc, ReplaceData> ops;
  private final UUID worldUUID;

  ReplaceOperation(World world) {
    ops = new HashMap<>();
    worldUUID = world.getUID();
  }

  @Nullable
  ReplaceOperation apply(Server server, World world, boolean applyPhysics, DynmapSupport dynmap) {
    if (!worldUUID.equals(world.getUID())) {
      return null;
    }
    final ReplaceOperation undo = new ReplaceOperation(world);
    final BoundingBox bb = new BoundingBox();
    ops.forEach((loc, data) -> {
      String beforeBiomeName = null;
      if (data.biome != null) {
        Biome beforeBiome;
        try {
          beforeBiome = world.getBiome(loc.x, loc.y, loc.z);
        } catch (NoSuchMethodError e) {
          beforeBiome = world.getBiome(loc.x, loc.z);
        }
        Biome afterBiome = BiomeHelper.Resolve(data.biome, server);
        if (afterBiome != beforeBiome) {
          beforeBiomeName = beforeBiome.name();
          world.setBiome(loc.x, loc.y, loc.z, afterBiome);
          bb.add(loc);
        }
      }

      Block block = world.getBlockAt(loc.x, loc.y, loc.z);
      BlockData after = server.createBlockData(data.getAsString());
      if (block.getBlockData().matches(after) && beforeBiomeName == null) {
        return;
      }

      ReplaceData d = new ReplaceData(block.getBlockData().getAsString(true), beforeBiomeName);
      block.setBlockData(after, applyPhysics);
      undo.register(loc, d);
      bb.add(loc);
    });
    bb.use((Loc min, Loc max) -> {
      dynmap.triggerRender(new Location(world, min.x, min.y, min.z), new Location(world, max.x, max.y, max.z));
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
