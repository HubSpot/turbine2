package com.hubspot.turbine2.processor.models;

import com.google.common.collect.ImmutableList;

public record ProcessorTimings(
  String processorName,
  String timeUnit,
  long totalTime,
  long wallTime,
  ImmutableList<Long> roundTimings
) {}
