package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
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
      ControllerService.CloseHttpExchange(exchange, 404);
      return;
    }
    String path = fullPath.substring(kPath.length());
    if (method.equalsIgnoreCase("post") && path.equalsIgnoreCase("reserve_spawn_location")) {
      handleReserveSpawnLocation(exchange);
    } else if (method.equalsIgnoreCase("get") && path.equalsIgnoreCase("is_ready")) {
      handleIsReady(exchange);
    } else {
      ControllerService.CloseHttpExchange(exchange, 404);
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
      ControllerService.CloseHttpExchange(t, 500);
      return;
    }
    UUID uuid = UUID.fromString(uuidString);
    service.reserveSpawnLocation(uuid, dimension, x, y, z, yaw);
    ControllerService.CloseHttpExchange(t, 200);
  }

  private void handleIsReady(HttpExchange t) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(buffer);
    dos.writeBoolean(service.isServerReady());
    dos.close();
    t.sendResponseHeaders(200, buffer.size());
    OutputStream os = t.getResponseBody();
    os.write(buffer.toByteArray());
    os.close();
  }
}
