package com.github.giji34.t.command;

import com.github.giji34.t.Loc;

public class Portal {
    public final String name;
    public final Loc returnLoc;
    public final String destination;

    Portal(String name, Loc returnLoc, String destination) {
        this.name = name;
        this.returnLoc = returnLoc;
        this.destination = destination;
    }
}

