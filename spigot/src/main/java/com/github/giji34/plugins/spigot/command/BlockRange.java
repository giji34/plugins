package com.github.giji34.plugins.spigot.command;

import com.github.giji34.plugins.spigot.Loc;

import java.util.Optional;
import java.util.function.Function;

class BlockRange {
  public final Loc start;
  public final Loc end;

  BlockRange(Loc start, Loc end) {
    this.start = start.clone();
    this.end = end.clone();
  }

  int volume() {
    int dx = Math.abs(start.x - end.x) + 1;
    int dy = Math.abs(start.y - end.y) + 1;
    int dz = Math.abs(start.z - end.z) + 1;
    return dx * dy * dz;
  }

  Optional<String> forEach(Function<Loc, Optional<String>> callback) {
    int x0 = getMinX();
    int x1 = getMaxX();
    int y0 = getMinY();
    int y1 = getMaxY();
    int z0 = getMinZ();
    int z1 = getMaxZ();
    try {
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          for (int x = x0; x <= x1; x++) {
            Optional<String> err = callback.apply(new Loc(x, y, z));
            if (err.isPresent()) {
              return err;
            }
          }
        }
      }
    } catch (Exception e) {
      return Optional.of(e.getMessage());
    }
    return Optional.empty();
  }

  int getMinX() {
    return Math.min(start.x, end.x);
  }

  int getMaxX() {
    return Math.max(start.x, end.x);
  }

  int getMinY() {
    return Math.min(start.y, end.y);
  }

  int getMaxY() {
    return Math.max(start.y, end.y);
  }

  int getMinZ() {
    return Math.min(start.z, end.z);
  }

  int getMaxZ() {
    return Math.max(start.z, end.z);
  }

  boolean contains(int x, int y, int z) {
    return getMinX() <= x && x <= getMaxX()
      && getMinY() <= y && y <= getMaxY()
      && getMinZ() <= z && z <= getMaxZ();
  }
}
