package com.github.giji34.plugins.velocity;

import com.github.giji34.plugins.shared.ChannelNames;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import redis.clients.jedis.JedisPubSub;

import java.util.Optional;
import java.util.UUID;

public class ConnectionRequestDispatcher extends JedisPubSub {
  private final ProxyServer server;

  ConnectionRequestDispatcher(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void onPMessage(String pattern, String channel, String message) {
    Optional<String> serverName = ChannelNames.getServerName(channel);
    if (serverName.isEmpty()) {
      return;
    }
    UUID playerUuid = UUID.fromString(message);
    Optional<Player> player = server.getPlayer(playerUuid);
    if (player.isEmpty()) {
      return;
    }
    Optional<RegisteredServer> destinationServer = server.getServer(serverName.get());
    if (destinationServer.isEmpty()) {
      return;
    }
    ConnectionRequestBuilder builder = player.get().createConnectionRequest(destinationServer.get());
    builder.fireAndForget();
  }
}
