package com.github.giji34.spigot.command;

import com.github.giji34.spigot.Loc;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.util.List;
import java.util.UUID;

public class IntraServerPortal extends Portal {
  public final double x;
  public final double y;
  public final double z;
  public final float yaw;

  IntraServerPortal(String name, UUID worldUuid, List<Loc> blocks, double x, double y, double z, float yaw) {
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
    this.x = ((Number) xObj).doubleValue();
    this.y = ((Number) yObj).doubleValue();
    this.z = ((Number) zObj).doubleValue();
    final Object yawObj = destination.get("yaw");
    if (yawObj instanceof Number) {
      this.yaw = ((Number) yawObj).floatValue();
    } else {
      this.yaw = 0;
    }
  }

  @Override
  public void apply(Player player, JavaPlugin source) {
    Location location = player.getLocation();
    UUID uuid = player.getWorld().getUID();
    if (!uuid.equals(this.worldUuid)) {
      return;
    }
    location.setX(this.x);
    location.setY(this.y);
    location.setZ(this.z);
    location.setYaw(this.yaw);
    player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
  }

  @Override
  public void save(BufferedWriter br) throws Exception {
    super.save(br);
    br.write("  type: \"intra\"");
    br.newLine();
    br.write("  data:");
    br.newLine();
    br.write("    destination:");
    br.newLine();
    br.write("      x: " + this.x);
    br.newLine();
    br.write("      y: " + this.y);
    br.newLine();
    br.write("      z: " + this.z);
    br.newLine();
    br.write("      yaw: " + this.yaw);
    br.newLine();
  }
}
