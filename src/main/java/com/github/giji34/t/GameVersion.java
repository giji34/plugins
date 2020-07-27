package com.github.giji34.t;

import org.bukkit.Server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GameVersion {
    final int major;
    final int minor;
    final int bugfix;

    public GameVersion(int major, int minor, int bugfix) {
        this.major = major;
        this.minor = minor;
        this.bugfix = bugfix;
    }

    private static final Pattern kServerVersionRegex = Pattern.compile(".*\\(MC: ([0-9]*)[.]([0-9]*)[.]([0-9]*)\\).*");

    static GameVersion fromServer(Server server) throws Exception {
        // git-Paper-100 (MC: 1.16.1)
        String version = server.getVersion();
        Matcher matcher = kServerVersionRegex.matcher(version);
        if (!matcher.matches()) {
            throw new Exception("cannot get server version from string; input=" + version);
        }
        String majorString = matcher.group(1);
        String minorString = matcher.group(2);
        String bugfixString = matcher.group(3);
        int major = Integer.parseInt(majorString, 10);
        int minor = Integer.parseInt(minorString, 10);
        int bugfix = Integer.parseInt(bugfixString, 10);
        return new GameVersion(major, minor, bugfix);
    }
}
