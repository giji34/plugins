package com.github.giji34.plugins.spigot.command;

public class ReservedSpawnLocation {
  public final int dimension;
  public final double x;
  public final double y;
  public final double z;

  ReservedSpawnLocation(int dimension, double x, double y, double z) {
    this.dimension = dimension;
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
