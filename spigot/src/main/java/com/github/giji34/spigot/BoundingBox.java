package com.github.giji34.spigot;

import java.util.Optional;
import java.util.function.BiConsumer;

public class BoundingBox {
    private Optional<Loc> min = Optional.empty();
    private Optional<Loc> max = Optional.empty();

    public void add(Loc loc) {
        if (min.isPresent() && max.isPresent()) {
            int minX = Math.min(min.get().x, loc.x);
            int minY = Math.min(min.get().y, loc.y);
            int minZ = Math.min(min.get().z, loc.z);

            int maxX = Math.max(max.get().x, loc.x);
            int maxY = Math.max(max.get().y, loc.y);
            int maxZ = Math.max(max.get().z, loc.z);

            min = Optional.of(new Loc(minX, minY, minZ));
            max = Optional.of(new Loc(maxX, maxY, maxZ));
        } else {
            min = Optional.of(new Loc(loc.x, loc.y, loc.z));
            max = Optional.of(new Loc(loc.x, loc.y, loc.z));
        }
    }

    public void use(BiConsumer<Loc, Loc> callback) {
        if (min.isPresent() && max.isPresent()) {
            callback.accept(min.get(), max.get());
        }
    }
}
