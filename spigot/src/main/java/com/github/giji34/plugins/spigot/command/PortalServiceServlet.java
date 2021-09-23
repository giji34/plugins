package com.github.giji34.plugins.spigot.command;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PortalServiceServlet extends HttpServlet {
  private final PortalService service;
  private JsonRpcServer jsonRpcServer;

  public PortalServiceServlet(PortalService service) {
    this.service = service;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try {
      jsonRpcServer.handle(req, resp);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  public void init(ServletConfig config) {
    this.jsonRpcServer = new JsonRpcServer(this.service, PortalService.class);
  }
}
