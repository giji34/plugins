package com.github.giji34.t;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Config {
    public final String snapshotServerHost;
    public final int snapshotServerPort;

    private Config(String snapshotServerHost, int snapshotServerPort) {
        this.snapshotServerHost = snapshotServerHost;
        this.snapshotServerPort = snapshotServerPort;
    }

    static Config Load(Logger logger, File pluginDirectory) {
        File config = new File(pluginDirectory, "config.properties");
        String snapshotServerHost = "";
        int snapshotServerPort = 0;
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
                }
            }
        } catch (Exception e) {
            logger.warning("config.properties がありません");
        }
        return new Config(snapshotServerHost, snapshotServerPort);
    }
}
