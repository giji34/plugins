package com.github.giji34.plugins.spigot;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class BackupService {
  private final Path gbackupDirectory;
  private final Path gitDirectory;
  private final Path serverRootDirectory;
  private final JavaPlugin owner;
  private final AtomicBoolean needsBackup = new AtomicBoolean(false);

  static final int kBackupIntervalMinutes = 15;

  BackupService(Path gbackupDirectory, Path gitDirectory, JavaPlugin owner) {
    this.gbackupDirectory = gbackupDirectory.toAbsolutePath();
    this.gitDirectory = gitDirectory.toAbsolutePath();
    this.serverRootDirectory = owner.getDataFolder().getParentFile().getParentFile().toPath().toAbsolutePath();
    this.owner = owner;

    owner.getServer().getScheduler()
      .scheduleSyncDelayedTask(owner, this::timerCallback, TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes));
  }

  void timerCallback() {
    long numPlayers = owner.getServer().getOnlinePlayers().size();
    new Thread(() -> {
      if (!needsBackup.get()) {
        owner.getServer().getScheduler()
          .scheduleSyncDelayedTask(owner, this::timerCallback, TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes));
        return;
      }

      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmm");
      String commitMessage = numPlayers + ": " + now.format(formatter);

      try {
        ProcessBuilder pb = new ProcessBuilder(
          "bash",
          Path.of(gbackupDirectory.toString(), "backup").toString(),
          serverRootDirectory.toString(),
          gitDirectory.toString(),
          commitMessage);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();

        needsBackup.set(false);
      } catch (Throwable e) {
        e.printStackTrace();
      }

      owner.getServer().getScheduler()
        .scheduleSyncDelayedTask(owner, this::timerCallback, TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes));
    }).start();
  }

  void setNeedsBackup() {
    needsBackup.set(true);
  }
}
