package com.hubspot.turbine2.testing;

import static com.hubspot.turbine2.testing.FixtureConstants.JAVA_FILES_FIXTURES_DIR;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.testing.compile.JavaFileObjects;
import java.nio.file.Paths;
import javax.tools.JavaFileObject;

public final class FixtureLoader {

  private final String testName;

  FixtureLoader(String testName) {
    this.testName = testName;
  }

  public JavaFileObject loadFixtureObject(String fileName) {
    return JavaFileObjects.forResource(getResourceName(fileName));
  }

  public ByteSource loadFixtureByteSource(String fileName) {
    return Resources.asByteSource(Resources.getResource(getResourceName(fileName)));
  }

  private String getResourceName(String fileName) {
    return Paths.get(JAVA_FILES_FIXTURES_DIR, testName, fileName).toString();
  }
}
