package com.github.giji34.plugins.velocity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Config {
  public String redisHost = "";
  public int redisPort = -1;

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
    redis.host=host
    redis.port=port
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
      if (key.equals("redis.host")) {
        redisHost = value.trim();
      } else if (key.equals("redis.port")) {
        redisPort = Integer.parseInt(value, 10);
      }
    }
  }
}
