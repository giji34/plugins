package com.github.giji34.t.command;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class Portal {
    public final String name;
    public final @Nullable Location returnLoc;
    public final String destination;

    Portal(String name, @Nullable Location returnLoc, String destination) {
        this.name = name;
        this.returnLoc = returnLoc;
        this.destination = destination;
    }
}
