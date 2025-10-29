package com.hubspot.turbine2.example;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.hubspot.turbine2.processor.TurbineGenerator;
import com.hubspot.turbine2.processor.TurbineProcessorBase;
import java.lang.annotation.Annotation;
import java.util.Map;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TurbineExampleProcessor extends TurbineProcessorBase {

  @Override
  public Module getGuiceModule() {
    return new TurbineExampleModule();
  }

  @Override
  public Map<Class<? extends Annotation>, Class<? extends TurbineGenerator>> getGenerators() {
    return ImmutableMap.of(TurbineExample.class, TurbineExampleGenerator.class);
  }
}
