package com.github.giji34.plugins.spigot;

import org.bukkit.Server;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class BiomeHelper {
  public static @Nullable Biome Resolve(String name, Server server) {
    GameVersion version;
    try {
      version = GameVersion.fromServer(server);
    } catch (Exception e) {
      System.out.println(e);
      return null;
    }
    name = migrateBiomeName(name, version);
    if (name.startsWith("minecraft:")) {
      name = name.substring(10);
    }
    try {
      Biome b = Biome.valueOf(name.toUpperCase());
      return b;
    } catch (Exception e) {
      return null;
    }
  }

  private static String migrateBiomeName(String name, @NotNull GameVersion running) {
    GameVersion v1_16 = new GameVersion(1, 16, 0);
    if (running.graterOrEqual(v1_16)) {
      if (name.equals("minecraft:nether")) {
        return "minecraft:nether_wastes";
      }
    }
    return name;
  }
}
