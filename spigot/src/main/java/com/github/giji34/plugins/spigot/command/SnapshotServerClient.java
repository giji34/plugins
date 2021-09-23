package com.github.giji34.plugins.spigot.command;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;

class SnapshotServerClient {
  final String host;
  final int port;

  SnapshotServerClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * 指定したバージョンでチャンク生成直後のブロックの状態を取得する.
   *
   * @param version
   * @param dimension
   * @param range
   * @return
   */
  @NotNull Snapshot getWildSnapshot(String version, int dimension, BlockRange range) {
    MutableSnapshot snapshot = new MutableSnapshot(range);
    try {
      URL url = createURL("wild", null, version, dimension, range);
      retrieve(url, snapshot);
    } catch (Exception e) {
      snapshot.setErrorMessage(e.getMessage());
    }
    return snapshot;
  }

  /**
   * サーバーのバックアップから, 指定した日時のブロックの状態を取得する.
   *
   * @param date
   * @param dimension
   * @param range
   * @return
   */
  @NotNull Snapshot getBackupSnapshot(OffsetDateTime date, int dimension, BlockRange range) {
    MutableSnapshot snapshot = new MutableSnapshot(range);
    try {
      URL url = createURL("history", date, null, dimension, range);
      retrieve(url, snapshot);
    } catch (Exception e) {
      snapshot.setErrorMessage(e.getMessage());
    }
    return snapshot;
  }

  private URL createURL(String type, @Nullable OffsetDateTime date, @Nullable String version, int dimension, BlockRange range) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("http://").append(host).append(":").append(port);
    sb.append("/").append(type);
    sb.append("?dimension=").append(dimension);
    if (version != null) {
      sb.append("&version=").append(version);
    }
    if (date != null) {
      sb.append("&time=").append(date.toEpochSecond());
    }
    sb.append("&minX=").append(range.getMinX()).append("&maxX=").append(range.getMaxX());
    sb.append("&minY=").append(range.getMinY()).append("&maxY=").append(range.getMaxY());
    sb.append("&minZ=").append(range.getMinZ()).append("&maxZ=").append(range.getMaxZ());
    return new URL(sb.toString());
  }

  private void retrieve(URL url, MutableSnapshot snapshot) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    final int status = connection.getResponseCode();
    if (status != HttpURLConnection.HTTP_OK) {
      snapshot.setErrorMessage("status=" + status);
      return;
    }
    InputStream in = connection.getInputStream();
    Gson gson = new Gson();
    SnapshotServerResponse response = gson.fromJson(new InputStreamReader(in), SnapshotServerResponse.class);
    if (!response.status.equals("ok")) {
      snapshot.setErrorMessage(response.status);
      return;
    }
    final BlockRange range = snapshot.range;
    int idx = 0;
    for (int y = range.getMinY(); y <= range.getMaxY(); y++) {
      for (int z = range.getMinZ(); z <= range.getMaxZ(); z++) {
        for (int x = range.getMinX(); x <= range.getMaxX(); x++) {
          final String blockData = response.block.palette[response.block.indices[idx]];
          final String biome = response.biome.palette[response.biome.indices[idx]];
          final int version = response.version.palette[response.version.indices[idx]];
          snapshot.set(x, y, z, ResolveNamespacedId(blockData), ResolveNamespacedId(biome), version);
          idx++;
        }
      }
    }
  }

  private static String ResolveNamespacedId(String s) {
    // snapshot-server/server の仕様.
    // air => minecraft:air
    // :somenamespace:blockname => somenamespace:blockname
    if (s.startsWith(":")) {
      return s.substring(1);
    } else {
      return "minecraft:" + s;
    }
  }
}
