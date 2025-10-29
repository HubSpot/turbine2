package com.hubspot.turbine2.processor.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.hubspot.turbine2.processor.annotations.AutoServiceAnnotationBinding;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;

public class TurbineProcessorModule extends AbstractModule {

  private final ProcessingEnvironment environment;
  private final Optional<Class<? extends Annotation>> autoServiceAnnotation;

  public TurbineProcessorModule(
    ProcessingEnvironment environment,
    Optional<Class<? extends Annotation>> autoServiceAnnotation
  ) {
    this.environment = environment;
    this.autoServiceAnnotation = autoServiceAnnotation;
  }

  @Override
  protected void configure() {
    install(new ProcessingEnvModule(environment));
    autoServiceAnnotation.ifPresent(annotation ->
      bind(new TypeLiteral<Class<? extends Annotation>>() {})
        .annotatedWith(AutoServiceAnnotationBinding.class)
        .toInstance(annotation)
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof TurbineProcessorModule that) {
      return Objects.equals(this.environment, that.environment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment);
  }
}
