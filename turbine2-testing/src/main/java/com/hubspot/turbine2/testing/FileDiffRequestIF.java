package com.hubspot.turbine2.testing;

import com.google.common.io.ByteSource;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjectSubject;
import com.hubspot.immutables.style.HubSpotStyle;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.immutables.value.Value;

@HubSpotStyle
@Value.Immutable
public interface FileDiffRequestIF {
  Compilation getCompilation();
  StandardLocation getSourceLocation();
  String getSourceName();
  FixtureLoader getFixtureLoader();
  String getFixtureFileName();
  JavaFileObject getCompilationOutput();

  default JavaFileObjectSubject assertSubject() {
    return CompilationSubject
      .assertThat(getCompilation())
      .generatedFile(getSourceLocation(), getSourceName());
  }

  default JavaFileObject loadFixtureAsJavaSource() {
    return getFixtureLoader().loadFixtureObject(getFixtureFileName());
  }

  default ByteSource loadFixtureAsByteSource() {
    return getFixtureLoader().loadFixtureByteSource(getFixtureFileName());
  }
}
