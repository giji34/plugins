package com.github.giji34.t;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;

public class Border {
    final Path2D path = new Path2D.Double();
    final World.Environment dimension;

    Border(ConfigurationSection section) throws Exception {
        /*
         border1:
          dimension: 0
          points:
            - x: 123
              z: 456
            - x: 789
              z: 124
        */
        ArrayList<Point> list = new ArrayList<>();
        int dimension = section.getInt("dimension", -99);
        if (dimension < -1 || 1 < dimension) {
            throw new Exception("invalid dimension");
        }
        switch (dimension) {
            case 1:
                this.dimension = World.Environment.THE_END;
                break;
            case -1:
                this.dimension = World.Environment.NETHER;
                break;
            default:
                this.dimension = World.Environment.NORMAL;
                break;
        }

        Object pointsObj = section.get("points");
        if (!(pointsObj instanceof ArrayList)) {
            throw new Exception("points is not an array" + pointsObj);
        }
        ArrayList<?> points = (ArrayList<?>) pointsObj;
        for (Object pointObj : points) {
            if (!(pointObj instanceof HashMap)) {
                throw new Exception("invalid type in points section" + pointObj.getClass());
            }
            HashMap<?, ?> point = (HashMap<?, ?>) pointObj;
            if (!point.containsKey("x") || !point.containsKey("z")) {
                throw new Exception("object must have x or z in points section");
            }
            Object xObj = point.get("x");
            Object zObj = point.get("z");
            if (!(xObj instanceof Integer) || !(zObj instanceof Integer)) {
                throw new Exception("invalid type for coordinate");
            }
            int x = (Integer)xObj;
            int z = (Integer)zObj;
            list.add(new Point(x, z));
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
