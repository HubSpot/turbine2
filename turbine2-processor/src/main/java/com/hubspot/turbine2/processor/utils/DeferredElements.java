package com.hubspot.turbine2.processor.utils;

import com.hubspot.turbine2.processor.exception.DeferProcessingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeferredElements {

  private static final Logger LOG = LoggerFactory.getLogger(DeferredElements.class);

  private final Map<Element, DeferProcessingException> deferredElements;

  public DeferredElements() {
    this.deferredElements = new HashMap<>();
  }

  public DeferProcessingException put(Element key, DeferProcessingException value) {
    LOG.warn("Deferring {} due to: {}", getElementName(key), value.getMessage());
    return deferredElements.put(key, value);
  }

  private static String getElementName(Element element) {
    if (element == null) {
      return "null";
    }

    if (element instanceof TypeElement) {
      return ((TypeElement) element).getQualifiedName().toString();
    }

    return element.getSimpleName().toString();
  }

  public void remove(Element key) {
    deferredElements.remove(key);
  }

  public Collection<Element> all() {
    return deferredElements.keySet();
  }

  public int size() {
    return deferredElements.size();
  }

  public Set<Entry<Element, DeferProcessingException>> entrySet() {
    return deferredElements.entrySet();
  }
}
