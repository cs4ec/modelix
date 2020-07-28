package org.modelix.model.mpsplugin;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.apache.log4j.Level;
import java.util.concurrent.TimeUnit;

public class SharedExecutors {
  private static final Logger LOG = LogManager.getLogger(SharedExecutors.class);
  public static final ExecutorService FIXED = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
  public static final ScheduledExecutorService SCHEDULED = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);

  public static void shutdownAll() {
    SCHEDULED.shutdown();
    FIXED.shutdown();
  }

  public static ScheduledFuture<?> fixDelay(int milliSeconds, final Runnable r) {
    return SCHEDULED.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        try {
          r.run();
        } catch (Exception ex) {
          if (LOG.isEnabledFor(Level.ERROR)) {
            LOG.error("", ex);
          }
        }
      }
    }, milliSeconds, milliSeconds, TimeUnit.MILLISECONDS);
  }
}