package com.github.giji34.plugins.velocity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Optional;

public class Config {
  private final HashMap<String, Integer> rpcPorts = new HashMap<>();
  private final HashMap<String, String> instanceIds = new HashMap<>();
  private final HashMap<String, String> serverStatusFiles = new HashMap<>();

  public void load(File file) {
    try {
      unsafeLoad(file);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

  private void unsafeLoad(File file) throws Exception {
    /*
    rpc[]=server:port
    instance_id[]=server:i-aaaaaaaaaaaaaaaaa
    server_status_file[]=server:path_to_status_file
    */
    BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    while ((line = br.readLine()) != null) {
      String[] tokens = line.split("=");
      if (tokens.length != 2) {
        continue;
      }
      String key = tokens[0];
      String value = tokens[1];
      if (key.equals("rpc[]")) {
        String[] s = value.split(":");
        if (s.length != 2) {
          continue;
        }
        String server = s[0];
        int port = Integer.parseInt(s[1], 10);
        rpcPorts.put(server, port);
      } else if (key.equals("instance_id[]")) {
        String[] s = value.split(":");
        if (s.length != 2) {
          continue;
        }
        String server = s[0];
        String id = s[1];
        instanceIds.put(server, id);
      } else if (key.equals("server_status_file[]")) {
        String[] s = value.split(":");
        if (s.length != 2) {
          continue;
        }
        String server = s[0];
        String statusFile = s[1];
        serverStatusFiles.put(server, statusFile);
      }
    }
  }

  public Optional<Integer> getRpcPort(String server) {
    if (rpcPorts.containsKey(server)) {
      return Optional.of(rpcPorts.get(server));
    } else {
      return Optional.empty();
    }
  }

  public Optional<String> getInstanceId(String server) {
    if (instanceIds.containsKey(server)) {
      return Optional.of(instanceIds.get(server));
    } else {
      return Optional.empty();
    }
  }

  public Optional<String> getServerStatusFile(String server) {
    if (serverStatusFiles.containsKey(server)) {
      return Optional.of(serverStatusFiles.get(server));
    } else {
      return Optional.empty();
    }
  }
}
