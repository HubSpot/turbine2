package com.hubspot.turbine2.processor.generator;

import com.google.common.base.Strings;
import com.hubspot.turbine2.processor.TurbineGenerator;
import com.hubspot.turbine2.processor.utils.Tools;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

public abstract class TypeGenerator implements TurbineGenerator {

  private static final boolean IS_BLAZAR = System.getenv("BLAZAR_COORDINATES") != null;

  private final Tools tools;

  private final Optional<AnnotationSpec> extraGeneratedAnnotation;

  public TypeGenerator(Tools tools) {
    this.tools = tools;
    this.extraGeneratedAnnotation = Optional.empty();
  }

  public TypeGenerator(Tools tools, AnnotationSpec extraGeneratedAnnotation) {
    this.tools = tools;
    this.extraGeneratedAnnotation = Optional.of(extraGeneratedAnnotation);
  }

  public TypeSpec generate(Element element, RoundEnvironment roundEnvironment) {
    throw new IllegalStateException("Please implement generate or generateMany");
  }

  public Collection<TypeSpec> generateMany(
    Element element,
    RoundEnvironment roundEnvironment
  ) {
    return Collections.singleton(generate(element, roundEnvironment));
  }

  @Override
  public void process(Element element, RoundEnvironment roundEnvironment) {
    for (TypeSpec typeSpec : generateMany(element, roundEnvironment)) {
      String generatedMessage =
        "by " + getClass().getCanonicalName() + " from " + element.getSimpleName();
      TypeSpec.Builder specWithGeneratedBuilder = typeSpec
        .toBuilder()
        .addAnnotation(
          AnnotationSpec
            .builder(Generated.class)
            .addMember("value", "$S", getClass().getCanonicalName())
            .addMember("comments", "$S", generatedMessage)
            .build()
        );

      extraGeneratedAnnotation.ifPresent(specWithGeneratedBuilder::addAnnotation);

      String packageName = tools
        .elements()
        .getPackageOf(element)
        .getQualifiedName()
        .toString();

      try {
        JavaFile
          .builder(packageName, specWithGeneratedBuilder.build())
          .build()
          .writeTo(tools.filer());
      } catch (IOException e) {
        if (
          !IS_BLAZAR &&
          Strings
            .nullToEmpty(e.getMessage())
            .contains("Attempt to recreate a file for type")
        ) {
          tools
            .messager()
            .printMessage(
              Kind.WARNING,
              "Ignoring file re-creation error because we're not in Blazar; message was: " +
              Strings.nullToEmpty(e.getMessage()),
              element
            );
        } else {
          throw tools.printAndThrow("Failed to generate class: " + e, element);
        }
      }
    }
  }
}
