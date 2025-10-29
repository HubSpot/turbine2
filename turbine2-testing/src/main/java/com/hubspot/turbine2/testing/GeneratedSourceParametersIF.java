package com.hubspot.turbine2.testing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.hubspot.immutables.style.HubSpotImmutableStyle;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@HubSpotImmutableStyle
@Immutable
public interface GeneratedSourceParametersIF {
  @Parameter(order = 0)
  ImmutableSet<String> getInputFiles();

  @Parameter(order = 1)
  ImmutableSet<String> getExpectedOutputFiles();

  @Check
  default GeneratedSourceParametersIF normalize() {
    if (
      Ordering.natural().isOrdered(getInputFiles()) &&
      Ordering.natural().isOrdered(getExpectedOutputFiles())
    ) {
      return this;
    }
    return GeneratedSourceParameters.of(
      ImmutableSet.copyOf(ImmutableList.sortedCopyOf(getInputFiles())),
      ImmutableSet.copyOf(ImmutableList.sortedCopyOf(getExpectedOutputFiles()))
    );
  }
}
