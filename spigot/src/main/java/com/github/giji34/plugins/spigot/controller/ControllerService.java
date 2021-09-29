package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class ControllerService {
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();
  private final Logger logger;
  private final int port;
  private final PortalContext portalContext;

  public ControllerService(Logger logger, int port) {
    this.logger = logger;
    this.port = port;
    this.portalContext = new PortalContext(this);
  }

  public void start() {
    try {
      unsafeStart();
    } catch (Throwable e) {
      logger.warning(e.getMessage());
    }
  }

  private void unsafeStart() throws Throwable {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(PortalContext.kPath, portalContext);
    server.start();
    logger.info("started rpc server on port: " + port);
  }

  void reserveSpawnLocation(UUID player, int dimension, double x, double y, double z, float yaw) {
    synchronized (this) {
      ReservedSpawnLocation location = new ReservedSpawnLocation(dimension, x, y, z, yaw);
      this.reservation.put(player, location);
    }
  }

  public Optional<ReservedSpawnLocation> drainReservedSpawnLocation(UUID player) {
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
}
