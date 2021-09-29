package com.github.giji34.plugins.spigot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Config {
  public final String snapshotServerHost;
  public final int snapshotServerPort;
  public final boolean isSightSeeing;
  public final String[] monitoringFilesystemMountPoints;
  public final String redisHost;
  public final int redisPort;
  public final String serverName;

  private Config(String serverName, String snapshotServerHost, int snapshotServerPort, boolean isSightSeeing, String[] monitoringFilesystemMountPoints, String redisHost, int redisPort) {
    this.serverName = serverName;
    this.snapshotServerHost = snapshotServerHost;
    this.snapshotServerPort = snapshotServerPort;
    this.isSightSeeing = isSightSeeing;
    this.monitoringFilesystemMountPoints = monitoringFilesystemMountPoints;
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }

  static Config Load(Logger logger, File pluginDirectory) {
    File config = new File(pluginDirectory, "config.properties");
    String serverName = "";
    String snapshotServerHost = "";
    int snapshotServerPort = 0;
    boolean sightseeing = true;
    ArrayList<String> monitoringFilesystemMountPoints = new ArrayList<>();
    String redisHost = "";
    int redisPort = -1;
    try {
      FileInputStream fis = new FileInputStream(config);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      String line;
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split("=");
        if (tokens.length != 2) {
          continue;
        }
        String key = tokens[0];
        String value = tokens[1];
        if (key.equals("snapshotserver.host")) {
          snapshotServerHost = value;
        } else if (key.equals("snapshotserver.port")) {
          snapshotServerPort = Integer.parseInt(value, 10);
        } else if (key.equals("sightseeing")) {
          sightseeing = !value.equals("false");
        } else if (key.equals("fs.mountpoints[]")) {
          monitoringFilesystemMountPoints.add(value.trim());
        } else if (key.equals("redis.host")) {
          redisHost = value.trim();
        } else if (key.equals("redis.port")) {
          redisPort = Integer.parseInt(value, 10);
        } else if (key.equals("name")) {
          serverName = value.trim();
        }
      }
    } catch (Exception e) {
      logger.warning("config.properties がありません");
    }
    return new Config(serverName, snapshotServerHost, snapshotServerPort, sightseeing, monitoringFilesystemMountPoints.toArray(new String[0]), redisHost, redisPort);
  }
}
