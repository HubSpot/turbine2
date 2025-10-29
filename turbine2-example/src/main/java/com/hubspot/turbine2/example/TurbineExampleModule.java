package com.hubspot.turbine2.example;

import com.google.inject.AbstractModule;
import java.util.Objects;

public final class TurbineExampleModule extends AbstractModule {

  public TurbineExampleModule() {}

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TurbineExampleModule;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(TurbineExampleModule.class);
  }
}
