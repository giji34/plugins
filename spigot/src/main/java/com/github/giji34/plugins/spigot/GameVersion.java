package com.github.giji34.plugins.spigot;

import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameVersion {
  final int major;
  final int minor;
  final int bugfix;

  public GameVersion(int major, int minor, int bugfix) {
    this.major = major;
    this.minor = minor;
    this.bugfix = bugfix;
  }

  private static final Pattern kServerVersionRegex = Pattern.compile(".*\\(MC: ([0-9]+)[.]([0-9]+)([.][0-9]+)?\\).*");

  public static @NotNull GameVersion fromServer(Server server) throws Exception {
    // git-Paper-100 (MC: 1.16.1)
    // git-Paper-39 (MC: 1.19)
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
    int bugfix = 0;
    if (bugfixString != null) {
      bugfix = Integer.parseInt(bugfixString.substring(1), 10);
    }
    return new GameVersion(major, minor, bugfix);
  }

  public static @Nullable GameVersion fromChunkDataVersion(int version) {
    if (version <= 1519) {
      return new GameVersion(1, 13, 0);
    } else if (version <= 1628) {
      return new GameVersion(1, 13, 1);
    } else if (version <= 1631) {
      return new GameVersion(1, 13, 2);
    } else if (version <= 1952) {
      return new GameVersion(1, 14, 0);
    } else if (version <= 1957) {
      return new GameVersion(1, 14, 1);
    } else if (version <= 1963) {
      return new GameVersion(1, 14, 2);
    } else if (version <= 1968) {
      return new GameVersion(1, 14, 3);
    } else if (version <= 1976) {
      return new GameVersion(1, 14, 4);
    } else if (version <= 2225) {
      return new GameVersion(1, 15, 0);
    } else if (version <= 2230) {
      return new GameVersion(1, 15, 2);
    } else if (version <= 2566) {
      return new GameVersion(1, 16, 0);
    } else if (version <= 2567) {
      return new GameVersion(1, 16, 1);
    } else if (version <= 2578) {
      return new GameVersion(1, 16, 2);
    } else if (version <= 2580) {
      return new GameVersion(1, 16, 3);
    } else if (version <= 2584) {
      return new GameVersion(1, 16, 4);
    } else if (version <= 2586) {
      return new GameVersion(1, 16, 5);
    } else if (version <= 2724) {
      return new GameVersion(1, 17, 0);
    } else if (version <= 2730) {
      return new GameVersion(1, 17, 1);
    } else if (version <= 2860) {
      return new GameVersion(1, 18, 0);
    } else if (version <= 2865) {
      return new GameVersion(1, 18, 1);
    } else if (version <= 2975) {
      return new GameVersion(1, 18, 2);
    } else if (version <= 3105) {
      return new GameVersion(1, 19, 0);
    } else if (version <= 3117) {
      return new GameVersion(1, 19, 1);
    } else if (version <= 3120) {
      return new GameVersion(1, 19, 2);
    }
    System.err.println("Unknown chunk data version: " + version);
    return null;
  }

  public boolean less(@NotNull GameVersion other) {
    if (this.major > other.major) {
      return false;
    } else if (this.major < other.major) {
      return true;
    }
    if (this.minor > other.minor) {
      return false;
    } else if (this.minor < other.minor) {
      return true;
    }
    return this.bugfix < other.bugfix;
  }

  public boolean lessOrEqual(@NotNull GameVersion other) {
    return this.less(other) || this.equals(other);
  }

  public boolean grater(@NotNull GameVersion other) {
    return other.less(this);
  }

  public boolean graterOrEqual(@NotNull GameVersion other) {
    return this.grater(other) || this.equals(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GameVersion that = (GameVersion) o;

    if (major != that.major) return false;
    if (minor != that.minor) return false;
    return bugfix == that.bugfix;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + bugfix;
    return result;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + bugfix;
  }
}
