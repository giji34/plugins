package com.github.giji34.plugins.spigot;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class Border {
  final Path2D path = new Path2D.Double();
  final World.Environment dimension;

  Border(ConfigurationSection section) throws Exception {
    java.util.List<Double> x = section.getDoubleList("x");
    List<Double> z = section.getDoubleList("z");
    String world = section.getString("world", "");
    int size = Math.min(x.size(), z.size());
    ArrayList<Point> list = new ArrayList<>();
    switch (world) {
      case "world_the_end":
        this.dimension = World.Environment.THE_END;
        break;
      case "world_nether":
        this.dimension = World.Environment.NETHER;
        break;
      case "world":
        this.dimension = World.Environment.NORMAL;
        break;
      default:
        throw new Exception("dimension unknown for world: " + world);
    }
    for (int i = 0; i < size; i++) {
      int px = (int) (double) x.get(i);
      int pz = (int) (double) z.get(i);
      list.add(new Point(px, pz));
    }

    if (list.isEmpty()) {
      return;
    }
    this.path.moveTo(list.get(0).x, list.get(0).y);
    for (int i = 1; i < list.size(); i++) {
      Point p = list.get(i);
      this.path.lineTo(p.x, p.y);
    }
    this.path.closePath();
  }

  boolean contains(Point p) {
    return this.path.contains(p);
  }
}
