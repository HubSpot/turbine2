package com.hubspot.turbine.testing;

import com.hubspot.immutables.style.HubSpotImmutableStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@HubSpotImmutableStyle
public interface BasicImmutableIF {
  String getString();
}
