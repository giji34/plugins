package com.github.giji34.plugins.shared;

import java.util.Optional;

public class ChannelNames {
  public static final String kSpigotPluginChannel = "giji34:portal_v1";
  public static final String kRedisDialChannelPrefix = "giji34:portal:dial_v1:";
  public static final String kRedisCallbackChannelPrefix = "giji34:portal:callback_v1:";

  private ChannelNames() {
  }

  public static String getRedisDialChannelName(String server) {
    return kRedisDialChannelPrefix + server;
  }

  public static String getRedisCallbackChannelName(String server) {
    return kRedisCallbackChannelPrefix + server;
  }

  public static Optional<String> getServerName(String channel) {
    if (channel.startsWith(kRedisDialChannelPrefix)) {
      return Optional.of(channel.substring(kRedisDialChannelPrefix.length()));
    } else if (channel.startsWith(kRedisCallbackChannelPrefix)) {
      return Optional.of(channel.substring(kRedisCallbackChannelPrefix.length()));
    } else {
      return Optional.empty();
    }
  }
}
