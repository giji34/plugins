package com.github.giji34.plugins.spigot.command;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class PortalService implements HttpHandler {
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();
  private final Logger logger;
  private final int port;
  private final String rootPath = "/portal/";

  public PortalService(Logger logger, int port) {
    this.logger = logger;
    this.port = port;
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
    server.createContext(rootPath, this);
    server.start();
    logger.info("started rpc server on port: " + port);
  }

  public boolean reserveSpawnLocation(UUID player, int dimension, double x, double y, double z, float yaw) {
    synchronized (this) {
      ReservedSpawnLocation location = new ReservedSpawnLocation(dimension, x, y, z, yaw);
      this.reservation.put(player, location);
      return true;
    }
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
  public void handle(HttpExchange exchange) throws IOException {
    final String method = exchange.getRequestMethod();
    final String fullPath = exchange.getRequestURI().getPath();
    if (!fullPath.startsWith(rootPath)) {
      send404NotFound(exchange);
      return;
    }
    String path = fullPath.substring(rootPath.length());
    if (method.equalsIgnoreCase("post") && path.equalsIgnoreCase("reserve_spawn_location")) {
      handleReserveSpawnLocation(exchange);
    } else {
      send404NotFound(exchange);
    }
  }

  private void handleReserveSpawnLocation(HttpExchange t) throws IOException {
    InputStream is = t.getRequestBody();
    String uuidString;
    int dimension;
    double x;
    double y;
    double z;
    float yaw;
    try {
      DataInputStream dis = new DataInputStream(is);
      uuidString = dis.readUTF();
      dimension = dis.readInt();
      x = dis.readDouble();
      y = dis.readDouble();
      z = dis.readDouble();
      yaw = dis.readFloat();
    } catch (IOException e) {
      send500InternalError(t);
      return;
    }
    UUID uuid = UUID.fromString(uuidString);
    reserveSpawnLocation(uuid, dimension, x, y, z, yaw);
    t.sendResponseHeaders(200, 0);
    OutputStream os = t.getResponseBody();
    os.close();
  }

  private void send404NotFound(HttpExchange t) throws IOException {
    t.sendResponseHeaders(404, 0);
    OutputStream os = t.getResponseBody();
    os.close();
  }

  private void send500InternalError(HttpExchange t) throws IOException {
    t.sendResponseHeaders(500, 0);
    OutputStream os = t.getResponseBody();
    os.close();
  }
}
