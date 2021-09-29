package com.github.giji34.plugins.spigot.controller;

public class ReservedSpawnLocation {
  public final int dimension;
  public final double x;
  public final double y;
  public final double z;
  public final float yaw;

  ReservedSpawnLocation(int dimension, double x, double y, double z, float yaw) {
    this.dimension = dimension;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
  }
}
