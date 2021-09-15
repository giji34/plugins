package com.github.giji34.t;

import org.bukkit.Location;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.jetbrains.annotations.NotNull;

public class DynmapSupport extends DynmapCommonAPIListener {
    private DynmapAPI api = null;

    @Override
    public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
        if (dynmapCommonAPI instanceof DynmapAPI) {
            this.api = (DynmapAPI) dynmapCommonAPI;
        }
    }

    public void triggerRender(Location min, Location max) {
        if (this.api == null) {
            return;
        }
        this.api.triggerRenderOfVolume(min, max);
    }

    public boolean isRenderJobActive(World world) {
        String normalizedName = NormalizeWorldName(world.getName());
        return this.api.isRenderJobActive(normalizedName);
    }

    private static String NormalizeWorldName(@NotNull String n) {
        return n.replace('/', '-').replace('[', '_').replace(']', '_');
    }
}
