package com.hubspot.turbine2.processor.guice;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.hubspot.turbine2.processor.log.TurbineMessager;
import java.util.Map.Entry;
import java.util.Objects;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class ProcessingEnvModule extends AbstractModule {

  private final Messager messager;
  private final Filer filer;
  private final Elements elementUtils;
  private final Types typeUtils;
  private final ProcessingEnvironment processingEnvironment;
  private final ImmutableMap<String, String> options;

  public ProcessingEnvModule(ProcessingEnvironment environment) {
    this.messager = new TurbineMessager(environment.getMessager());
    this.filer = environment.getFiler();
    this.elementUtils = environment.getElementUtils();
    this.typeUtils = environment.getTypeUtils();
    this.processingEnvironment = environment;
    this.options =
      environment
        .getOptions()
        .entrySet()
        .stream()
        .filter(e -> e.getValue() != null)
        .collect(
          ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, (a, b) -> b)
        );
  }

  @Override
  public void configure() {
    bind(Messager.class).toInstance(messager);
    bind(Filer.class).toInstance(filer);
    bind(Elements.class).toInstance(elementUtils);
    bind(Types.class).toInstance(typeUtils);
    bind(ProcessingEnvironment.class).toInstance(processingEnvironment);
    bind(new TypeLiteral<ImmutableMap<String, String>>() {})
      .annotatedWith(ProcessorOptions.class)
      .toInstance(options);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ProcessingEnvModule that) {
      return Objects.equals(processingEnvironment, that.processingEnvironment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(processingEnvironment);
  }
}
