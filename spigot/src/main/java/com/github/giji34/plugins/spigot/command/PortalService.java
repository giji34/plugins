package com.github.giji34.plugins.spigot.command;

import com.github.giji34.plugins.shared.ChannelNames;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class PortalService extends BinaryJedisPubSub {
  private final Jedis subscribingClient;
  private final Jedis publisherClient;
  private final Logger logger;
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();
  private final String dialChannel;
  private final String callbackChannel;
  private Thread redisSubscribingThread;

  public PortalService(Logger logger, String serverName, String redisHost, int redisPort) {
    subscribingClient = new Jedis(redisHost, redisPort);
    publisherClient = new Jedis(redisHost, redisPort);
    this.logger = logger;

    dialChannel = ChannelNames.getRedisDialChannelName(serverName);
    callbackChannel = ChannelNames.getRedisCallbackChannelName(serverName);
    redisSubscribingThread = new Thread(() -> {
      subscribingClient.subscribe(this, dialChannel.getBytes(StandardCharsets.UTF_8));
    });
    redisSubscribingThread.start();
  }

  public boolean reserveSpawnLocation(UUID player, int dimension, double x, double y, double z, float yaw) {
    synchronized (this) {
      ReservedSpawnLocation location = new ReservedSpawnLocation(dimension, x, y, z, yaw);
      this.reservation.put(player, location);
    }
    String callbackMessage = player.toString().toLowerCase();
    this.publisherClient.publish(callbackChannel, callbackMessage);
    return true;
  }

  public Optional<ReservedSpawnLocation> drainReservation(UUID player) {
    synchronized (this) {
      if (this.reservation.containsKey(player)) {
        ReservedSpawnLocation location = this.reservation.get(player);
        this.reservation.remove(player);
        return Optional.of(location);
      } else {
        return Optional.empty();
      }
    }
  }

  @Override
  public void onMessage(byte[] channel, byte[] message) {
    ByteArrayInputStream bais = new ByteArrayInputStream(message);
    String uuidString;
    int dimension;
    double x;
    double y;
    double z;
    float yaw;
    try {
      DataInputStream dis = new DataInputStream(bais);
      uuidString = dis.readUTF();
      dimension = dis.readInt();
      x = dis.readDouble();
      y = dis.readDouble();
      z = dis.readDouble();
      yaw = dis.readFloat();
    } catch (IOException e) {
      e.printStackTrace();
      logger.warning(e.getMessage());
      return;
    }
    UUID uuid = UUID.fromString(uuidString);
    reserveSpawnLocation(uuid, dimension, x, y, z, yaw);
  }
}
