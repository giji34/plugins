package com.github.giji34.plugins.shared;

import java.util.UUID;

public interface PortalService {
  public boolean reserveSpawnLocation(UUID player, int dimension, double x, double y, double z);
}
