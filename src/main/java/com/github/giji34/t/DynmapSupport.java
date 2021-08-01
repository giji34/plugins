package com.github.giji34.t;

import org.bukkit.Location;
import org.dynmap.DynmapAPI;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;

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
}
