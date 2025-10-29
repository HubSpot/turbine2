package com.hubspot.turbine2.processor.exception;

import java.util.Optional;
import javax.lang.model.element.Element;

/**
 *
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DeferProcessingException extends RuntimeException {

  // Root element represents the element that initiated processing
  private final Optional<Element> rootElement;
  // Source element represents the element that actually failed, i.e. a method on a class.
  private final Optional<Element> sourceElement;

  public DeferProcessingException(String message, Element sourceElement) {
    super(message);
    this.sourceElement = Optional.of(sourceElement);
    this.rootElement = Optional.empty();
  }

  public DeferProcessingException(
    String message,
    Element sourceElement,
    Element rootElement
  ) {
    super(message);
    this.sourceElement = Optional.of(sourceElement);
    this.rootElement = Optional.of(rootElement);
  }

  public DeferProcessingException(String message) {
    super(message);
    this.sourceElement = Optional.empty();
    this.rootElement = Optional.empty();
  }

  public DeferProcessingException(DeferProcessingException other, Element rootElement) {
    super(other);
    this.sourceElement = other.getSourceElement();
    this.rootElement = Optional.ofNullable(rootElement);
  }

  public DeferProcessingException(
    DeferProcessingException other,
    Element sourceElement,
    Element rootElement
  ) {
    super(other);
    this.sourceElement = Optional.of(sourceElement);
    this.rootElement = Optional.of(rootElement);
  }

  public Optional<Element> getSourceElement() {
    return sourceElement;
  }

  public Optional<Element> getRootElement() {
    return rootElement;
  }
}
