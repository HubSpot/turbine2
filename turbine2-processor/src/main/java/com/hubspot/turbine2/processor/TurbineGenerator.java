package com.hubspot.turbine2.processor;

import com.hubspot.turbine2.processor.exception.DeferProcessingException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import org.slf4j.MDC;

public interface TurbineGenerator {
  Set<Class<? extends Annotation>> supportedAnnotationTypes();

  default void process(Element element, RoundEnvironment roundEnvironment) {
    throw new UnsupportedOperationException(
      "Please implement a process(Element) or process(Collection<Element>) method"
    );
  }

  default Collection<DeferProcessingException> process(
    Collection<Element> elements,
    RoundEnvironment environment
  ) {
    List<DeferProcessingException> deferrals = new ArrayList<>();
    for (Element element : elements) {
      try (
        MDC.MDCCloseable ignored = MDC.putCloseable(
          "element",
          element.getSimpleName().toString()
        )
      ) {
        process(element, environment);
      } catch (DeferProcessingException deferProcessingException) {
        if (deferProcessingException.getRootElement().isEmpty()) {
          deferrals.add(new DeferProcessingException(deferProcessingException, element));
        } else {
          deferrals.add(deferProcessingException);
        }
      }
    }

    return deferrals;
  }

  /**
   * Called in the final round of processing, even if no elements were processed by this processor. This will always be called after all calls to {@link TurbineGenerator#process(Element, RoundEnvironment)}
   *
   * Can be used to aggregate across all rounds and generate output at the end of processing.
   */
  default void afterProcessing() {}
}
