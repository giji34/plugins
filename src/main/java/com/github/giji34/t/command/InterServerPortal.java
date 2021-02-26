package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.UUID;

public class InterServerPortal extends Portal {
    public final @Nullable Location returnLoc;
    public final String destination;

    InterServerPortal(String name, UUID worldUuid, List<Loc> blocks, @Nullable Location returnLoc, String destination) {
        super(name, worldUuid, blocks);
        this.returnLoc = returnLoc;
        this.destination = destination;
    }

    InterServerPortal(String name, UUID worldUuid, List<Loc> blocks, ConfigurationSection section) throws Exception {
        super(name, worldUuid, blocks);

        // data
        final Object dataObj = section.get("data");
        if (!(dataObj instanceof ConfigurationSection)) {
            throw new Exception("\"data\" section does not exist");
        }
        final ConfigurationSection data = (ConfigurationSection)dataObj;

        // destination
        final Object destinationObj = data.get("destination");
        if (!(destinationObj instanceof String)) {
            throw new Exception("\"destination\" section does not exist");
        }
        this.destination = (String) destinationObj;

        // return_loc
        final Object returnLocObj = data.get("return_loc");
        if (returnLocObj instanceof ConfigurationSection) {
            ConfigurationSection returnLocSection = (ConfigurationSection) returnLocObj;
            Object xObj = returnLocSection.get("x");
            Object yObj = returnLocSection.get("y");
            Object zObj = returnLocSection.get("z");
            Object yawObj = returnLocSection.get("yaw");
            if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof  Number)) {
                throw new Exception("return_loc must contain x, y, and z");
            }
            double x = ((Number)xObj).doubleValue();
            double y = ((Number)yObj).doubleValue();
            double z = ((Number)zObj).doubleValue();
            if (xObj instanceof Integer) {
                x += 0.5;
            }
            if (zObj instanceof Integer) {
                z += 0.5;
            }
            float yaw = 0;
            if (yawObj instanceof Number) {
                yaw = ((Number)yawObj).floatValue();
            }
            this.returnLoc = new Location(null, x, y, z, yaw, 0);
        } else {
            this.returnLoc = null;
        }
    }

    @Override
    public void save(BufferedWriter br) throws Exception {
        super.save(br);
        br.write("  type: \"inter\"");
        br.newLine();
        br.write("  data:");
        br.newLine();
        if (this.returnLoc != null) {
            br.write("    return_loc:");
            br.newLine();
            br.write("      x: " + this.returnLoc.getX());
            br.newLine();
            br.write("      y: " + this.returnLoc.getY());
            br.newLine();
            br.write("      z: " + this.returnLoc.getZ());
            br.newLine();
            br.write("      yaw: " + this.returnLoc.getYaw());
            br.newLine();
        }
        br.write("    destination: \"" + this.destination + "\"");
    }

    @Override
    public void apply(Player player, JavaPlugin source) {
        Connect(player, this.destination, source);
    }

    public static void Connect(Player player, String destination, JavaPlugin source) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("Connect");
            dos.writeUTF(destination);
            player.sendPluginMessage(source, "BungeeCord", baos.toByteArray());
            baos.close();
            dos.close();
        } catch (Exception e) {
            source.getLogger().warning("InterServerPortal.Connect; io error: e=" + e);
        }
    }
}
