package com.github.giji34.plugins.spigot;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class BackupService {
  private final String gbackupDirectory;
  private final String gitDirectory;
  private final String serverRootDirectory;
  private final JavaPlugin owner;
  private final AtomicBoolean needsBackup = new AtomicBoolean(false);
  private @Nullable Integer backupTimerTask = null;
  private @Nullable Long lastPlayerQuitTimeMillis = null;
  private @Nullable Thread backupWorker = null;

  static final int kBackupIntervalMinutes = 15;
  static final int kShutdownAfterLastPlayerQuitMinutes = 10;

  BackupService(String gbackupDirectory, String gitDirectory, JavaPlugin owner) {
    this.gbackupDirectory = gbackupDirectory;
    this.gitDirectory = gitDirectory;
    File dataFolder = owner.getDataFolder().getAbsoluteFile();
    this.serverRootDirectory = dataFolder.getParentFile().getParentFile().getAbsolutePath();
    this.owner = owner;
  }

  void backupTimerCallback() {
    if (backupWorker != null) {
      owner.getLogger().warning("backup process still running");
      return;
    }
    if (!needsBackup.get()) {
      scheduleBackupTimer();
      return;
    }

    long numPlayers = owner.getServer().getOnlinePlayers().size();

    (backupWorker = new Thread(() -> {
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmm");
      String commitMessage = numPlayers + ": " + now.format(formatter);

      try {
        ProcessBuilder pb = new ProcessBuilder(
          "bash",
          Path.of(gbackupDirectory, "backup").toString(),
          serverRootDirectory,
          gitDirectory,
          commitMessage,
          Path.of(gbackupDirectory, "stdout.log").toString(),
          Path.of(gbackupDirectory, "stderr.log").toString());
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
          owner.getLogger().warning("backup script exit with code: " + code);
        }

        needsBackup.set(false);
      } catch (Throwable e) {
        owner.getLogger().warning("backup script throws exception: " + e.getMessage());
        e.printStackTrace();
      }

      this.owner.getServer().getScheduler()
        .scheduleSyncDelayedTask(this.owner, () -> {
          this.backupWorker = null;
          this.scheduleBackupTimer();
        }, 0);
    })).start();
  }

  private void playerActivityTimerCallback() {
    Server server = owner.getServer();

    int numPlayers = server.getOnlinePlayers().size();
    if (numPlayers > 0) {
      lastPlayerQuitTimeMillis = null;
    } else if (lastPlayerQuitTimeMillis == null) {
      lastPlayerQuitTimeMillis = System.currentTimeMillis();
    }

    if (lastPlayerQuitTimeMillis != null && backupWorker == null) {
      long elapsedMillis = System.currentTimeMillis() - lastPlayerQuitTimeMillis;
      if (elapsedMillis >= TimeUnit.MINUTES.toMillis(kShutdownAfterLastPlayerQuitMinutes)) {
        if (backupTimerTask != null) {
          server.getScheduler().cancelTask(backupTimerTask);
          backupTimerTask = null;
        }
        lastPlayerQuitTimeMillis = null;

        owner.getLogger().info(elapsedMillis / 1000.0 + " seconds elapsed after last player quit. Schedule shutting down the server");
        // backup should be done after shutdown, typically on the last lines of server startup script
        server.shutdown();
      }
    }
  }

  private long ServerTicksFromMinutes(int minutes) {
    return TimeUnit.MINUTES.toMillis(minutes) / 50;
  }

  void scheduleBackupTimer() {
    if (!owner.getServer().isPrimaryThread()) {
      owner.getLogger().severe("scheduleBackupTimer called from sub-thread");
      return;
    }
    int taskId = owner.getServer().getScheduler()
      .scheduleSyncDelayedTask(owner, this::backupTimerCallback, ServerTicksFromMinutes(kBackupIntervalMinutes));
    backupTimerTask = taskId;
  }

  private void initializePlayerActivityTimer() {
    owner.getServer().getScheduler()
      .scheduleSyncRepeatingTask(owner, this::playerActivityTimerCallback, 0, 20);
  }

  void onEnable() {
    lastPlayerQuitTimeMillis = System.currentTimeMillis();
    scheduleBackupTimer();
    initializePlayerActivityTimer();
  }

  void onPlayerJoin() {
    needsBackup.set(true);
  }
}
