package com.hubspot.turbine2.testing;

public enum FixtureTestMode {
  EVALUATE, // run the comparison against fixtures
  GENERATE_FIXTURES, // generate the fixtures, disabling assertions.
}
