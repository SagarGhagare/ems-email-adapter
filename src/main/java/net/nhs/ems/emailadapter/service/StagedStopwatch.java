package net.nhs.ems.emailadapter.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class StagedStopwatch {

  private final LambdaLogger logger;
  private Instant stageStart = Instant.now();

  private StagedStopwatch(LambdaLogger logger) {
    this.logger = logger;
    logger.log("Timing started at " + stageStart + '\n');
  }

  private String getTimeUntilNow(Instant now) {
    var seconds = stageStart.until(now, ChronoUnit.SECONDS);
    var millis = stageStart
        .minus(seconds, ChronoUnit.SECONDS)
        .until(now, ChronoUnit.MILLIS);

    return String.format("%d.%ds", seconds, millis);
  }

  public void finishStage(String stageName) {
    var now = Instant.now();
    logger.log(String.format("finished %s in %s\n", stageName, getTimeUntilNow(now)));
    stageStart = now;
  }

  public static StagedStopwatch start(LambdaLogger logger) {
    return new StagedStopwatch(logger);
  }
}
