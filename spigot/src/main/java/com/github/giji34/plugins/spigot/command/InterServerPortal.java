package com.github.giji34.plugins.spigot.command;

import com.github.giji34.plugins.spigot.Loc;
import org.bukkit.Location;
import org.bukkit.World;
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
  public final String server;
  public final int dimension;
  public final Location location;

  public static final String kPortalPluginChannel = "giji34:portal_v0";

  InterServerPortal(String name, UUID worldUuid, List<Loc> blocks, String server, int dimension, Location location) {
    super(name, worldUuid, blocks);
    this.server = server;
    this.dimension = dimension;
    this.location = location;
  }

  InterServerPortal(String name, UUID worldUuid, List<Loc> blocks, ConfigurationSection section) throws Exception {
    super(name, worldUuid, blocks);

    // data
    final Object dataObj = section.get("data");
    if (!(dataObj instanceof ConfigurationSection)) {
      throw new Exception("\"data\" section does not exist");
    }
    final ConfigurationSection data = (ConfigurationSection) dataObj;

    // data/server
    final Object serverObj = data.get("server");
    if (!(serverObj instanceof String)) {
      throw new Exception("\"server\" section does not exist");
    }
    this.server = (String) serverObj;

    // data/dimension
    final Object dimensionObj = data.get("dimension");
    if (!(dimensionObj instanceof Number)) {
      throw new Exception("\"dimension\" must be an integer");
    }
    this.dimension = ((Number) dimensionObj).intValue();

    // data/location
    final Object locationObj = data.get("location");
    if (!(locationObj instanceof ConfigurationSection)) {
      throw new Exception("\"location\" section does not exists");
    }
    final ConfigurationSection locationSec = (ConfigurationSection) locationObj;
    final Object xObj = locationSec.get("x");
    final Object yObj = locationSec.get("y");
    final Object zObj = locationSec.get("z");
    final Object yawObj = locationSec.get("yaw");
    if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) {
      throw new Exception("\"location\" section must contains x, y, and z");
    }
    double x = ((Number) xObj).doubleValue();
    double y = ((Number) yObj).doubleValue();
    double z = ((Number) zObj).doubleValue();
    float yaw = 0.0f;
    if (yawObj instanceof Number) {
      yaw = ((Number) yawObj).floatValue();
    }
    this.location = new Location(null, x, y, z, yaw, 0);
  }

  @Override
  public void save(BufferedWriter br) throws Exception {
    super.save(br);
    br.write("  type: \"inter\"");
    br.newLine();
    br.write("  data:");
    br.newLine();
    br.write("    server: " + this.server);
    br.newLine();
    br.write("    dimension: " + this.dimension);
    br.newLine();
    br.write("    location:");
    br.newLine();
    br.write("      x: " + this.location.getX());
    br.newLine();
    br.write("      y: " + this.location.getY());
    br.newLine();
    br.write("      z: " + this.location.getZ());
    br.newLine();
    br.write("      yaw: " + this.location.getYaw());
    br.newLine();
  }

  @Override
  public void apply(Player player, int rpcPort, JavaPlugin source) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeUTF("portal");
      dos.writeUTF(this.server);
      dos.writeInt(rpcPort);
      dos.writeInt(this.dimension);
      dos.writeDouble(this.location.getX());
      dos.writeDouble(this.location.getY());
      dos.writeDouble(this.location.getZ());
      dos.writeFloat(this.location.getYaw());

      player.sendPluginMessage(source, kPortalPluginChannel, baos.toByteArray());
      baos.close();
      dos.close();
    } catch (Exception e) {
      source.getLogger().warning("InterServerPortal.Connect; io error: e=" + e);
    }
  }

  public static void Connect(Player player, String destination, JavaPlugin source) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeUTF("connect");
      dos.writeUTF(destination);

      player.sendPluginMessage(source, kPortalPluginChannel, baos.toByteArray());
      baos.close();
      dos.close();
    } catch (Exception e) {
      source.getLogger().warning("InterServerPortal.Connect; io error: e=" + e);
    }
  }
}
