package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.jetbrains.annotations.Nullable;

public class Portal {
    public final String name;
    public final @Nullable Loc returnLoc;
    public final String destination;

    Portal(String name, @Nullable Loc returnLoc, String destination) {
        this.name = name;
        this.returnLoc = returnLoc;
        this.destination = destination;
    }
}

