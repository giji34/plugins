package com.github.giji34.spigot;

import org.bukkit.World.Environment;
import org.bukkit.util.Vector;

public class Landmark {
    public final String name;
    public final Environment dimension;
    public final Vector location;

    public Landmark(String name, Vector location, Environment dimension) {
        this.name = name;
        this.location = location.clone();
        this.dimension = dimension;
    }
}
