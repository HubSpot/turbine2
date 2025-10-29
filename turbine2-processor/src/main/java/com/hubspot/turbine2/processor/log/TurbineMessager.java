package com.hubspot.turbine2.processor.log;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TurbineMessager implements Messager {

  private static final Logger LOG = LoggerFactory.getLogger(TurbineMessager.class);

  private final Messager messager;

  public TurbineMessager(Messager messager) {
    this.messager = messager;
  }

  @Override
  public void printMessage(Kind kind, CharSequence msg) {
    logAtLevel(kind, msg);
    messager.printMessage(kind, msg);
  }

  @Override
  public void printMessage(Kind kind, CharSequence msg, Element e) {
    try (MDC.MDCCloseable ignored = MDC.putCloseable("element", getElementName(e))) {
      logAtLevel(kind, msg);
      messager.printMessage(kind, msg, e);
    }
  }

  @Override
  public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
    try (MDC.MDCCloseable ignored = MDC.putCloseable("element", getElementName(e))) {
      logAtLevel(kind, msg);
      messager.printMessage(kind, msg, e, a);
    }
  }

  @Override
  public void printMessage(
    Kind kind,
    CharSequence msg,
    Element e,
    AnnotationMirror a,
    AnnotationValue v
  ) {
    try (MDC.MDCCloseable ignored = MDC.putCloseable("element", getElementName(e))) {
      logAtLevel(kind, msg);
      messager.printMessage(kind, msg, e, a, v);
    }
  }

  private static String getElementName(Element e) {
    if (e == null) {
      return "null";
    }

    if (e instanceof TypeElement) {
      return ((TypeElement) e).getQualifiedName().toString();
    }

    return e.getSimpleName().toString();
  }

  private static void logAtLevel(Kind kind, CharSequence message) {
    String msg = message.toString();
    switch (kind) {
      case ERROR:
        LOG.error(msg);
        break;
      case WARNING:
      case MANDATORY_WARNING:
        LOG.warn(msg);
        break;
      case NOTE:
        LOG.info(msg);
        break;
      case OTHER:
        LOG.debug(msg);
        break;
      default:
        LOG.trace(msg);
    }
  }
}
