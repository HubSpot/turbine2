package com.hubspot.turbine.example;

import com.hubspot.turbine2.example.TurbineExampleProcessor;
import com.hubspot.turbine2.testing.BaseCompilationFixtureTest;
import com.hubspot.turbine2.testing.FixtureTestMode;
import java.io.IOException;
import java.util.List;
import javax.annotation.processing.Processor;
import org.junit.jupiter.api.Test;

public class TurbineExampleFixtureTest extends BaseCompilationFixtureTest {

  private final TurbineExampleProcessor exampleProcessor = new TurbineExampleProcessor();

  // This is using the fully qualified class name to avoid the 'BanHiddenImport' error-prone check
  private final org.immutables.value.internal.$processor$.$Processor immutableProcessor =
    new org.immutables.value.internal.$processor$.$Processor();

  @Override
  protected Iterable<? extends Processor> getProcessors() {
    return List.of(immutableProcessor, exampleProcessor);
  }

  @Override
  protected FixtureTestMode getTestMode() {
    return FixtureTestMode.GENERATE_FIXTURES;
  }

  @Test
  public void itBuildsAccordingToFixture() throws IOException {
    runFixtureTest(
      "ExampleAnnotatedClass.java",
      "ExampleImmutableIF.java",
      "OtherAnnotatedClass.java",
      "OtherImmutableIF.java"
    );
  }
}
