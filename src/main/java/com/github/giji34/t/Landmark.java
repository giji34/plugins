package com.github.giji34.t;

import org.bukkit.util.Vector;

import java.util.UUID;

class Landmark {
    public final String name;
    public final UUID worldUID;
    public final Vector location;

    public Landmark(String name, Vector location, UUID worldUID) {
        this.name = name;
        this.location = location.clone();
        this.worldUID = worldUID;
    }
}
