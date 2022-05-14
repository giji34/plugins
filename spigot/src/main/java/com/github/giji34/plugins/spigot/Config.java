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
  public final int rpcPort;
  public final String gbackupToolDirectory;
  public final String gbackupGitDirectoyr;

  private Config(
    String snapshotServerHost,
    int snapshotServerPort,
    boolean isSightSeeing,
    String[] monitoringFilesystemMountPoints,
    int rpcPort,
    String gbackupToolDirectory,
    String gbackupGitDirectoyr) {
    this.snapshotServerHost = snapshotServerHost;
    this.snapshotServerPort = snapshotServerPort;
    this.isSightSeeing = isSightSeeing;
    this.monitoringFilesystemMountPoints = monitoringFilesystemMountPoints;
    this.rpcPort = rpcPort;
    this.gbackupToolDirectory = gbackupToolDirectory;
    this.gbackupGitDirectoyr = gbackupGitDirectoyr;
  }

  static Config Load(Logger logger, File pluginDirectory) {
    File config = new File(pluginDirectory, "config.properties");
    String snapshotServerHost = "";
    int snapshotServerPort = 0;
    boolean sightseeing = true;
    ArrayList<String> monitoringFilesystemMountPoints = new ArrayList<>();
    int rpcPort = -1;
    String gbackupToolDirectory = "";
    String gbackupGitDirectory = "";
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
        } else if (key.equals("rpc.port")) {
          rpcPort = Integer.parseInt(value, 10);
        } else if (key.equals("gbackup.tool_directory")) {
          gbackupToolDirectory = value;
        } else if (key.equals("gbackup.git_directory")) {
          gbackupGitDirectory = value;
        }
      }
    } catch (Exception e) {
      logger.warning("config.properties がありません");
    }
    return new Config(
      snapshotServerHost,
      snapshotServerPort,
      sightseeing,
      monitoringFilesystemMountPoints.toArray(new String[0]),
      rpcPort,
      gbackupToolDirectory,
      gbackupGitDirectory);
  }
}
