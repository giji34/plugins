package com.github.giji34.t;

import java.util.function.Function;

class SelectedBlockRange {
    public final Loc start;
    public final Loc end;

    SelectedBlockRange(Loc start, Loc end) {
        this.start = start.clone();
        this.end = end.clone();
    }

    int volume() {
        int dx = Math.abs(start.x - end.x) + 1;
        int dy = Math.abs(start.y - end.y) + 1;
        int dz = Math.abs(start.z - end.z) + 1;
        return dx * dy * dz;
    }

    void forEach(Function<Loc, Boolean> callback) {
        int x0 = Math.min(start.x, end.x);
        int x1 = Math.max(start.x, end.x);
        int y0 = Math.min(start.y, end.y);
        int y1 = Math.max(start.y, end.y);
        int z0 = Math.min(start.z, end.z);
        int z1 = Math.max(start.z, end.z);
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (!callback.apply(new Loc(x, y, z))) {
                        return;
                    }
                }
            }
        }
    }
}
