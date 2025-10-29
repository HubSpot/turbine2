package com.hubspot.turbine2.processor.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.FileSize;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.hubspot.turbine2.processor.TurbineGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class TurbineLogBinder {

  private final Messager messager;

  @Inject
  public TurbineLogBinder(Messager messager) {
    this.messager = messager;
  }

  public void configureLogBack(
    Collection<Class<? extends TurbineGenerator>> generatorClasses
  ) {
    Optional<Path> logDir = getVbaPathMaybe()
      .or(() ->
        Optional
          .ofNullable(Strings.emptyToNull(System.getenv("TURBINE_LOG_DIR")))
          .map(Path::of)
      );

    if (logDir.isPresent()) {
      String fileName = logDir.get().resolve("turbine.log").toString();

      messager.printMessage(Kind.NOTE, "[TURBINE] Will write logs to " + fileName);

      ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
      if (!(iLoggerFactory instanceof LoggerContext ctx)) {
        throw new RuntimeException(
          String.format(
            "Unable to configure logback: expected %s, found %s. " +
            "Likely cause is 0 or 2+ implementations of SLF4JServiceProvider on classpath.",
            LoggerContext.class,
            iLoggerFactory.getClass()
          )
        );
      }

      ctx.getLogger("ROOT").detachAppender("STDOUT");

      FileAppender<ILoggingEvent> appender = new FileAppender<>();
      appender.setContext(ctx);
      appender.setName("turbine file appender");
      appender.setFile(fileName);
      appender.setBufferSize(FileSize.valueOf("1MB"));

      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(ctx);
      encoder.setPattern("%level %logger [%mdc] - %msg%n");
      encoder.start();

      appender.setEncoder(encoder);

      Level level = Optional
        .ofNullable(Strings.emptyToNull(System.getenv("TURBINE_LOG_LEVEL")))
        .map(Level::valueOf)
        .orElse(Level.INFO);

      appender.start();

      for (Class<?> generatorClass : generatorClasses) {
        String packageLogger = generatorClass.getPackageName();

        Logger logger = ctx.getLogger(packageLogger);
        logger.setAdditive(false);
        logger.setLevel(level);
        logger.addAppender(appender);
        ctx.getLogger(packageLogger).addAppender(appender);
      }

      ctx.getLogger("com.hubspot.turbine").addAppender(appender);
      ctx.getLogger("com.hubspot.turbine").setLevel(level);
      ctx.getLogger("com.hubspot.turbine").debug("Logging setup done");
    }
  }

  private Optional<Path> getVbaPathMaybe() {
    String viewableBuildArtifactsDir = System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR");
    if (viewableBuildArtifactsDir == null) {
      return Optional.empty();
    }

    Path vbaDir = Paths.get(viewableBuildArtifactsDir);

    if (!Files.exists(vbaDir)) {
      try {
        Files.createDirectories(vbaDir);
      } catch (IOException e) {
        throw new RuntimeException("failed to create " + viewableBuildArtifactsDir, e);
      }
    }

    return Optional.of(vbaDir);
  }
}
