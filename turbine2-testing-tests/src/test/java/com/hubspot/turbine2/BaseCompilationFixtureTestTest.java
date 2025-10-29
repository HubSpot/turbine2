package com.hubspot.turbine2;

import com.hubspot.turbine2.testing.BaseCompilationFixtureTest;
import com.hubspot.turbine2.testing.FixtureTestMode;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BaseCompilationFixtureTestTest extends BaseCompilationFixtureTest {

  @Override
  protected FixtureTestMode getTestMode() {
    return FixtureTestMode.EVALUATE;
  }

  @Override
  protected Iterable<? extends Processor> getProcessors() {
    return List.of(new org.immutables.value.internal.$processor$.$Processor());
  }

  @Override
  protected boolean includeGeneratedSource(String sourceContent) {
    return true;
  }

  private static Stream<Arguments> providesExpectedCompilation() {
    return Stream.of(Arguments.of(list("BasicImmutableIF.java")));
  }

  @ParameterizedTest
  @MethodSource("providesExpectedCompilation")
  void itShouldMatchExpectedCompilation(List<String> inputFiles) throws IOException {
    runFixtureTest(inputFiles);
  }
}
