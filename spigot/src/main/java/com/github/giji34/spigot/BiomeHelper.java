package com.github.giji34.spigot;

import org.bukkit.Server;
import org.bukkit.block.Biome;
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

  private static String migrateBiomeName(String name, GameVersion running) {
    if (running.major == 1 && running.minor == 16) {
      if (name.equals("minecraft:nether")) {
        return "minecraft:nether_wastes";
      }
    }
    return name;
  }
}
