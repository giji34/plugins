package com.github.giji34.spigot;

import org.bukkit.util.Vector;

import java.util.Objects;

public class Loc {
  public final int x;
  public final int y;
  public final int z;

  public Loc(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  static Loc fromVectorFloored(Vector v) {
    return new Loc((int) Math.floor(v.getX()), (int) Math.floor(v.getY()), (int) Math.floor(v.getZ()));
  }

  @Override
  public String toString() {
    return "[" + x + ", " + y + ", " + z + "]";
  }

  @Override
  public Loc clone() {
    return new Loc(x, y, z);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Loc)) {
      return false;
    }
    Loc a = (Loc) obj;
    return a.x == this.x && a.y == this.y && a.z == this.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }
}
