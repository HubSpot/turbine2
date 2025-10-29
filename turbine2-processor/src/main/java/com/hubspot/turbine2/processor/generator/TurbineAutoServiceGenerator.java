package com.hubspot.turbine2.processor.generator;

import com.google.common.base.CharMatcher;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.turbine2.processor.TurbineGenerator;
import com.hubspot.turbine2.processor.annotations.AutoServiceAnnotationBinding;
import com.hubspot.turbine2.processor.exception.DeferProcessingException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@Singleton
public class TurbineAutoServiceGenerator implements TurbineGenerator {

  private static final String RESOURCE_FILE_PATH = "META-INF/services/";

  private final SortedSetMultimap<String, TypeElement> serviceElementsByType;

  private final Filer filer;
  private final Messager messager;
  private final Class<? extends Annotation> autoServiceAnnotation;

  @Inject
  TurbineAutoServiceGenerator(
    Filer filer,
    Messager messager,
    @AutoServiceAnnotationBinding Class<? extends Annotation> autoServiceAnnotation
  ) {
    this.filer = filer;
    this.messager = messager;
    this.autoServiceAnnotation = autoServiceAnnotation;
    this.serviceElementsByType =
      TreeMultimap.create(
        Comparator.naturalOrder(),
        Comparator.comparing(a -> a.getQualifiedName().toString())
      );
  }

  @Override
  public Set<Class<? extends Annotation>> supportedAnnotationTypes() {
    return Collections.singleton(autoServiceAnnotation);
  }

  @Override
  public Collection<DeferProcessingException> process(
    Collection<Element> elements,
    RoundEnvironment environment
  ) {
    if (elements.isEmpty()) {
      if (System.getenv("TURBINE_DEBUG") != null) {
        messager.printMessage(
          Kind.NOTE,
          "[TURBINE] saw no new elements annotated with @" + getAnnotationName()
        );
      }
      return Collections.emptyList();
    }

    messager.printMessage(
      Kind.NOTE,
      "[TURBINE] saw " +
      elements.size() +
      " new elements annotated with @" +
      getAnnotationName()
    );

    elements
      .stream()
      .map(e -> (TypeElement) e)
      .forEach(e -> serviceElementsByType.put(getServiceTypeName(e), e));

    return Collections.emptyList();
  }

  private String getAnnotationName() {
    return autoServiceAnnotation.getSimpleName();
  }

  @Override
  public void afterProcessing() {
    writeModuleResourceFile();
  }

  private void writeModuleResourceFile() {
    for (Entry<String, Collection<TypeElement>> entry : serviceElementsByType
      .asMap()
      .entrySet()) {
      try {
        writeModuleResourceFileInternal(entry.getKey(), entry.getValue());

        messager.printMessage(
          Kind.NOTE,
          "[TURBINE] wrote auto-discovery file with " +
          entry.getValue().size() +
          " services for type " +
          entry.getKey()
        );
      } catch (IOException e) {
        throw new RuntimeException(
          "unexpected exception writing resource file for TURBINE module auto-discovery for type: " +
          entry.getKey(),
          e
        );
      }
    }
  }

  private String getServiceTypeName(TypeElement typeElement) {
    return typeElement
      .getAnnotationMirrors()
      .stream()
      .filter(a ->
        a.getAnnotationType().toString().equals(autoServiceAnnotation.getName())
      )
      .flatMap(a -> a.getElementValues().entrySet().stream())
      .filter(e -> "type".equals(e.getKey().getSimpleName().toString()))
      .map(e -> extractClassName(e.getValue().getValue()))
      .findFirst()
      .orElseThrow(() ->
        new RuntimeException(
          "Could not find type() value for @" +
          getAnnotationName() +
          " annotation on " +
          typeElement
        )
      );
  }

  private String extractClassName(Object obj) {
    if (obj instanceof Class) {
      return ((Class<?>) obj).getCanonicalName();
    } else {
      return String.valueOf(obj);
    }
  }

  private void writeModuleResourceFileInternal(
    String type,
    Collection<TypeElement> elements
  ) throws IOException {
    Set<String> newServices = elements
      .stream()
      .map(element -> element.getQualifiedName().toString())
      .collect(Collectors.toSet());

    SortedSet<String> allServices = new TreeSet<>();
    // read the existing file.
    // this is needed because, during incremental compilation, we may not have
    // all the elements in our processor.
    // During a clean build this is a no-op
    try {
      FileObject existingFile = filer.getResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        RESOURCE_FILE_PATH + type
      );
      try (
        InputStream in = existingFile.openInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(
          in,
          StandardCharsets.UTF_8
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
      ) {
        Set<String> oldServices = bufferedReader
          .lines()
          .map(CharMatcher.whitespace()::trimFrom)
          .collect(Collectors.toSet());
        allServices.addAll(oldServices);
      }
    } catch (IOException ignored) {
      // there was no existing service file
    }
    if (!allServices.addAll(newServices)) {
      // no net-new services were added.
      return;
    }
    FileObject resourceFile = filer.createResource(
      StandardLocation.CLASS_OUTPUT,
      "",
      RESOURCE_FILE_PATH + type,
      elements.toArray(Element[]::new)
    );
    try (
      Writer writer = resourceFile.openWriter();
      BufferedWriter bufferedWriter = new BufferedWriter(writer)
    ) {
      for (String service : allServices) {
        bufferedWriter.write(service);
        bufferedWriter.write('\n');
      }
    }
  }
}
