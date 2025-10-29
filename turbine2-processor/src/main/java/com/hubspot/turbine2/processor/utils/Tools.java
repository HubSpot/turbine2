package com.hubspot.turbine2.processor.utils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.turbine2.processor.guice.ProcessorOptions;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

public class Tools {

  private final Messager messager;
  private final Filer filer;
  private final Elements elements;
  private final Types types;
  private final ImmutableMap<String, String> processorOptions;

  @Inject
  Tools(
    Messager messager,
    Filer filer,
    Elements elements,
    Types types,
    @ProcessorOptions ImmutableMap<String, String> processorOptions
  ) {
    this.messager = messager;
    this.filer = filer;
    this.elements = elements;
    this.types = types;
    this.processorOptions = processorOptions;
  }

  public Messager messager() {
    return messager;
  }

  public Filer filer() {
    return filer;
  }

  public Elements elements() {
    return elements;
  }

  public Types types() {
    return types;
  }

  public ImmutableMap<String, String> getProcessorOptions() {
    return processorOptions;
  }

  public IllegalStateException printAndThrow(IllegalStateException e) {
    throw printAndThrow(e, null);
  }

  public IllegalStateException printAndThrow(String message) {
    throw printAndThrow(message, null);
  }

  public IllegalStateException printAndThrow(String message, Element sourceElement) {
    throw printAndThrow(new IllegalStateException(message), sourceElement);
  }

  public IllegalStateException printAndThrow(
    IllegalStateException e,
    Element sourceElement
  ) {
    messager.printMessage(Kind.ERROR, e.getMessage(), sourceElement);
    throw e;
  }
}
