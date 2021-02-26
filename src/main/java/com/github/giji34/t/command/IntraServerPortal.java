package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class IntraServerPortal extends Portal {
    public final double x;
    public final double y;
    public final double z;
    @Nullable
    public final Float yaw;

    IntraServerPortal(String name, UUID worldUuid, List<Loc> blocks, double x, double y, double z, @Nullable Float yaw) {
        super(name, worldUuid, blocks);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    IntraServerPortal(String name, UUID worldUuid, List<Loc> blocks, ConfigurationSection section) throws Exception {
        super(name, worldUuid, blocks);
        final Object dataObj = section.get("data");
        if (!(dataObj instanceof ConfigurationSection)) {
            throw new Exception("\"data\" section does not exist");
        }
        final ConfigurationSection data = (ConfigurationSection) dataObj;

        final Object destinationObj = data.get("destination");
        if (!(destinationObj instanceof ConfigurationSection)) {
            throw new Exception("\"destination\" section does not exist");
        }
        final ConfigurationSection destination = (ConfigurationSection) destinationObj;
        final Object xObj = destination.get("x");
        final Object yObj = destination.get("y");
        final Object zObj = destination.get("z");
        if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) {
            throw new Exception("destination must contain x, y, and z");
        }
        this.x = ((Number)xObj).doubleValue();
        this.y = ((Number)yObj).doubleValue();
        this.z = ((Number)zObj).doubleValue();
        final Object yawObj = destination.get("yaw");
        if (yawObj instanceof Number) {
            this.yaw = ((Number) yawObj).floatValue();
        } else {
            this.yaw = null;
        }
    }

    @Override
    public void apply(Player player, JavaPlugin source) {
        //TODO:
    }
}
