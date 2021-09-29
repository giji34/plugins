package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PortalContext implements HttpHandler {
  private final ControllerService service;
  static final String kPath = "/portal/";

  PortalContext(ControllerService service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    final String method = exchange.getRequestMethod();
    final String fullPath = exchange.getRequestURI().getPath();
    if (!fullPath.startsWith(kPath)) {
      send404NotFound(exchange);
      return;
    }
    String path = fullPath.substring(kPath.length());
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
    service.reserveSpawnLocation(uuid, dimension, x, y, z, yaw);
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
