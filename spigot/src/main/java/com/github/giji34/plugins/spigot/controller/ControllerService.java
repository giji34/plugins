package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ControllerService {
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();
  private final JavaPlugin owner;
  private final Logger logger;
  private final int port;
  private final PortalContext portalContext;
  private final AtomicBoolean serverReady = new AtomicBoolean(false);

  public ControllerService(JavaPlugin owner, int port) {
    this.owner = owner;
    this.logger = owner.getLogger();
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

  static void CloseHttpExchange(HttpExchange t, int status) throws IOException {
    t.sendResponseHeaders(status, 0);
    OutputStream os = t.getResponseBody();
    os.close();
  }

  public void setServerReady() {
    serverReady.set(true);
  }

  public boolean isServerReady() {
    return serverReady.get();
  }
}
