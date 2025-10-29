package com.hubspot.turbine2.testing.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.turbine2.testing.FileDiffRequest;
import java.io.IOException;
import java.util.Optional;
import javax.tools.StandardLocation;

/**
 * The default checker is just going to assert that the contents
 * are exactly the same.
 */
public class DefaultFixtureFileHandler implements FixtureFileTypeHandler {

  @Override
  public boolean isApplicable(StandardLocation fileLocation, String sourceName) {
    return true;
  }

  @Override
  public void checkFileDiff(FileDiffRequest request) throws IOException {
    try {
      request.assertSubject().hasContents(request.loadFixtureAsByteSource());
    } catch (AssertionError error) {
      Optional<String> maybeCompilationOutput = maybeGetCompilationString(request);
      Optional<String> maybeFixture = maybeGetFixtureString(request);
      // encoding error. possibly a binary file so we just throw the assertion
      if (maybeCompilationOutput.isEmpty() || maybeFixture.isEmpty()) {
        throw error;
      }
      // otherwise we can diff the string to get a nicer error message
      assertThat(maybeCompilationOutput.get())
        .as("Content for %s is the same as fixture.", request.getSourceName())
        .isEqualTo(maybeFixture.get());
    }
  }
}
