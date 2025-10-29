package com.hubspot.turbine2.example;

import com.hubspot.turbine2.processor.exception.DeferProcessingException;
import com.hubspot.turbine2.processor.generator.TypeGenerator;
import com.hubspot.turbine2.processor.utils.Tools;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;

public class TurbineExampleGenerator extends TypeGenerator {

  private final Messager messager;

  @Inject
  TurbineExampleGenerator(Tools tools, Messager messager) {
    super(tools);
    this.messager = messager;
  }

  @Override
  public Set<Class<? extends Annotation>> supportedAnnotationTypes() {
    return Collections.singleton(TurbineExample.class);
  }

  @Override
  public TypeSpec generate(Element element, RoundEnvironment roundEnvironment) {
    TypeElement typeElement = ((TypeElement) element);

    List<ExecutableElement> methods = element
      .getEnclosedElements()
      .stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .map(e -> ((ExecutableElement) e))
      .collect(Collectors.toList());

    Set<MethodSpec> impls = new HashSet<>();
    for (ExecutableElement method : methods) {
      if (method.getReturnType().getKind() == TypeKind.ERROR) {
        throw new DeferProcessingException("Could not get immutable return type", method);
      }

      DeclaredType declaredType = ((DeclaredType) method.getReturnType());
      messager.printMessage(
        Kind.NOTE,
        "Got return type " + declaredType.asElement().getSimpleName(),
        method
      );

      impls.add(
        MethodSpec.overriding(method).addCode(CodeBlock.of("return null;")).build()
      );
    }

    return TypeSpec
      .classBuilder(element.getSimpleName() + "Test")
      .addSuperinterface(ClassName.get(typeElement))
      .addMethods(impls)
      .build();
  }
}
