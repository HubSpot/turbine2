package com.hubspot.turbine2.testing.diff;

import com.hubspot.turbine2.testing.FileDiffRequest;
import java.io.IOException;
import javax.tools.StandardLocation;

public class JavaFileHandler implements FixtureFileTypeHandler {

  @Override
  public boolean isApplicable(StandardLocation fileLocation, String sourceName) {
    return (
      StandardLocation.SOURCE_OUTPUT.equals(fileLocation) && sourceName.endsWith(".java")
    );
  }

  @Override
  public void checkFileDiff(FileDiffRequest request) throws IOException {
    request.assertSubject().hasSourceEquivalentTo(request.loadFixtureAsJavaSource());
  }
}
