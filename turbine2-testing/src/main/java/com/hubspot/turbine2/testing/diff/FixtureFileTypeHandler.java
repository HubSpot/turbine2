package com.hubspot.turbine2.testing.diff;

import com.hubspot.turbine2.testing.FileDiffRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.tools.StandardLocation;

public interface FixtureFileTypeHandler {
  boolean isApplicable(StandardLocation fileLocation, String sourceName);

  void checkFileDiff(FileDiffRequest request) throws IOException;

  default Optional<String> maybeGetCompilationString(FileDiffRequest request) {
    try {
      return Optional.of(request.getCompilationOutput().getCharContent(false).toString());
    } catch (Throwable ignored) {
      return Optional.empty();
    }
  }

  default Optional<String> maybeGetFixtureString(FileDiffRequest request) {
    try {
      return Optional.of(
        request
          .getFixtureLoader()
          .loadFixtureByteSource(request.getFixtureFileName())
          .asCharSource(StandardCharsets.UTF_8)
          .read()
      );
    } catch (Throwable ignored) {
      return Optional.empty();
    }
  }

  default boolean shouldDisambiguateFile(String name) {
    return false;
  }

  default void postProcessGeneratedFile(File outputFile) throws IOException {}
}
