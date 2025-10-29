package com.hubspot.turbine2.example;

import com.hubspot.immutables.style.HubSpotStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@HubSpotStyle
public interface OtherImmutableIF {
  String getString();
}
