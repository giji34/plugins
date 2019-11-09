package com.github.giji34.t;

import org.bukkit.util.Vector;

import java.util.UUID;

class Landmark {
    public final UUID worldUID;
    public final Vector location;

    public Landmark(Vector location, UUID worldUID) {
        this.location = location.clone();
        this.worldUID = worldUID;
    }
}
