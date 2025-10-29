package com.hubspot.turbine2.testing.diff;

import com.google.common.base.Splitter;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class SourceLocations {

  private SourceLocations() {
    throw new IllegalArgumentException();
  }

  public static StandardLocation getLocation(JavaFileObject javaFileObject) {
    String baseLocation = Splitter
      .on('/')
      .omitEmptyStrings()
      .split(javaFileObject.getName())
      .iterator()
      .next();
    return (StandardLocation) StandardLocation.locationFor(baseLocation);
  }
}
