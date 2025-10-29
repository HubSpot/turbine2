package com.hubspot.turbine2.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.hubspot.turbine2.processor.exception.DeferProcessingException;
import com.hubspot.turbine2.processor.generator.TurbineAutoServiceGenerator;
import com.hubspot.turbine2.processor.guice.TurbineProcessorModule;
import com.hubspot.turbine2.processor.log.TurbineLogBinder;
import com.hubspot.turbine2.processor.models.ProcessorTimings;
import com.hubspot.turbine2.processor.utils.DeferredElements;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "BanInjectorCreation", "DisallowPrintStackTrace" })
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public abstract class TurbineProcessorBase extends AbstractProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(TurbineProcessorBase.class);

  private static final String IMMUTABLE_GENERATED_ANNOTATION =
    "org.immutables.value.Generated";

  private final AtomicInteger roundCounter = new AtomicInteger(0); // TODO: does this need to be thread safe?
  private final Map<Class<? extends TurbineGenerator>, DeferredElements> deferredElementsByGenerator;
  private final List<Long> roundTimings = new ArrayList<>();
  private final Stopwatch wallTimeStopwatch = Stopwatch.createUnstarted();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private Injector injector;
  private ProcessingEnvironment environment;
  private Messager messager;

  public TurbineProcessorBase() {
    this.deferredElementsByGenerator = new ConcurrentHashMap<>(); // Just in case
  }

  public abstract Module getGuiceModule();

  public abstract Map<Class<? extends Annotation>, Class<? extends TurbineGenerator>> getGenerators();

  protected final Injector getInjector() {
    return injector;
  }

  /**
   * Override to use autoservice in your project.
   * First, you'll have to implement a simple annotation with
   * two parameters: type() and value() each with a Class type.
   * E.g.:
   * <pre>{@code
   * &#34;interface MySpecialAutoService {
   *   Class&lt;?&gt; type();  // the type we're binding
   *   Class&lt;&gt; value();  // the source value
   * }
   * }</pre>
   *
   * Note that due to limitations of java annotations, we cannot
   * enforce that contract in code.
   * <br>
   * By providing your own auto-service, you can get away
   * with having zero hidden dependencies in your project.
   */
  protected Optional<Class<? extends Annotation>> getAutoServiceAnnotation() {
    return Optional.empty();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotationNames = getAllGeneratorsIncludingTurbine()
      .keySet()
      .stream()
      .map(Class::getCanonicalName)
      .collect(Collectors.toSet());

    // Trigger after immutables are generated
    // We don't do anything with this annotation but this tells javac to issue another compilation round when it sees
    // source code output by the immutables ProxyProcessor.
    annotationNames.add(IMMUTABLE_GENERATED_ANNOTATION);
    return annotationNames;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    injector =
      Guice.createInjector(
        new TurbineProcessorModule(processingEnv, getAutoServiceAnnotation()),
        getGuiceModule()
      );
    environment = processingEnv;

    TurbineLogBinder logBinder = injector.getInstance(TurbineLogBinder.class);
    logBinder.configureLogBack(getAllGeneratorsIncludingTurbine().values());

    messager = injector.getInstance(Messager.class);

    wallTimeStopwatch.start();
    LOG.info("Turbine initialization done; Beginning new compilation.");
  }

  @Override
  public boolean process(
    Set<? extends TypeElement> annotations,
    RoundEnvironment roundEnv
  ) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    LOG.info(
      "############### Processing round {} ###############",
      roundCounter.getAndIncrement()
    );

    Map<Class<? extends Annotation>, Class<? extends TurbineGenerator>> generators =
      getAllGeneratorsIncludingTurbine();

    if (!roundEnv.processingOver()) {
      for (Class<? extends Annotation> annotation : generators.keySet()) {
        Class<? extends TurbineGenerator> genClass = generators.get(annotation);
        DeferredElements deferredElements = deferredElementsByGenerator.computeIfAbsent(
          genClass,
          k -> new DeferredElements()
        );
        TurbineGenerator generator = injector.getInstance(genClass);

        Set<Element> elements = new HashSet<>(
          roundEnv.getElementsAnnotatedWith(annotation)
        );

        // Try again with the deferred elements that have a matching annotation
        Collection<Element> defer = new HashSet<>(deferredElements.all());
        for (Element de : defer) {
          if (de.getAnnotation(annotation) != null) {
            elements.add(de);
            deferredElements.remove(de);
          }
        }

        try {
          Collection<DeferProcessingException> deferrals = generator.process(
            elements,
            roundEnv
          );

          for (DeferProcessingException de : deferrals) {
            Optional<Element> elementToReprocess = findReprocessElement(de, elements);

            if (elementToReprocess.isPresent()) {
              deferredElements.put(elementToReprocess.get(), de);
            } else {
              for (Element element : elements) {
                deferredElements.put(element, de);
              }
            }
          }
        } catch (DeferProcessingException de) {
          // If the source OR root element is reprocessable only reprocess that, otherwise redo the whole round
          Optional<Element> elementToReprocess = findReprocessElement(de, elements);

          if (elementToReprocess.isPresent()) {
            deferredElements.put(elementToReprocess.get(), de);
          } else {
            for (Element element : elements) {
              deferredElements.put(element, de);
            }
          }
        } catch (Exception e) {
          LOG.error(
            "Caught exception in turbine processor {}",
            genClass.getCanonicalName(),
            e
          );

          StringWriter sw = new StringWriter();
          sw
            .append("Caught exception in turbine processor ")
            .append(genClass.getCanonicalName())
            .append(":\n");
          e.printStackTrace(new PrintWriter(sw));
          String message = sw.toString();
          messager.printMessage(Kind.ERROR, message);
        }
      }

      LOG.info("{} elements were deferred.", deferredElementsByGenerator.size());
    } else {
      LOG.info("Processing is over");
      // These elements will have just been retried and the compiler is indicating that no subsequent compilation round
      // will be executed. This means these elements are now in error.
      for (Entry<Class<? extends TurbineGenerator>, DeferredElements> deferred : deferredElementsByGenerator.entrySet()) {
        LOG.error(
          "Generator {} had {} elements deferred at end of processing",
          deferred.getKey().getCanonicalName(),
          deferred.getValue().size()
        );
        for (Entry<Element, DeferProcessingException> entry : deferred
          .getValue()
          .entrySet()) {
          DeferProcessingException exc = entry.getValue();
          Element bestSourceElement = exc.getSourceElement().orElse(entry.getKey());

          messager.printMessage(Kind.ERROR, exc.getMessage(), bestSourceElement);
        }
      }

      for (Class<? extends Annotation> annotation : generators.keySet()) {
        TurbineGenerator generator = injector.getInstance(generators.get(annotation));

        generator.afterProcessing();
      }

      LOG.info("Post processing actions are complete");
    }

    long roundTimeUs = stopwatch.elapsed(TimeUnit.MICROSECONDS);
    roundTimings.add(roundTimeUs);
    LOG.info("Round took {}uS", roundTimeUs);

    if (roundEnv.processingOver()) {
      wallTimeStopwatch.stop();
      outputTimings();
    }

    // NEVER claim annotations, there's really no benefit, and it can screw with other processors.
    return false;
  }

  private Map<Class<? extends Annotation>, Class<? extends TurbineGenerator>> getAllGeneratorsIncludingTurbine() {
    Map<Class<? extends Annotation>, Class<? extends TurbineGenerator>> result =
      new HashMap<>(getGenerators());
    getAutoServiceAnnotation()
      .ifPresent(annotation -> result.put(annotation, TurbineAutoServiceGenerator.class));
    return result;
  }

  private Optional<Element> findReprocessElement(
    DeferProcessingException de,
    Set<Element> elements
  ) {
    return de
      .getRootElement()
      .or(() ->
        de
          .getSourceElement()
          .flatMap(se -> {
            if (elements.contains(se)) {
              return Optional.of(se);
            }

            return Optional.empty();
          })
      );
  }

  private void outputTimings() {
    String processorName = getClass().getSimpleName();
    String fileName = processorName + ".json";

    long totalTime = roundTimings.stream().mapToLong(Long::longValue).sum();

    ProcessorTimings timings = new ProcessorTimings(
      processorName,
      "MICROSECONDS",
      totalTime,
      wallTimeStopwatch.elapsed(TimeUnit.MICROSECONDS),
      ImmutableList.copyOf(roundTimings)
    );

    try {
      String targetDir = System.getProperty("maven.build.directory");
      Path baseDir = targetDir != null ? Paths.get(targetDir) : Paths.get("target");

      if (!Files.exists(baseDir)) {
        Files.createDirectories(baseDir);
      }

      Path turbineTimingsDir = baseDir.resolve("turbine-timings");
      if (!Files.exists(turbineTimingsDir)) {
        Files.createDirectories(turbineTimingsDir);
      }

      Path localTimingFile = turbineTimingsDir.resolve(fileName);
      try (FileWriter writer = new FileWriter(localTimingFile.toFile())) {
        OBJECT_MAPPER.writeValue(writer, timings);
        LOG.debug("Timing data written to {}", localTimingFile);
      }
    } catch (Exception e) {
      LOG.debug(
        "Unable to write timing data to target/turbine-timings directory: {}",
        e.getMessage()
      );
    }

    String viewableBuildArtifactsDir = System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR");
    if (viewableBuildArtifactsDir != null) {
      try {
        Path vbaDir = Paths.get(viewableBuildArtifactsDir);
        Path turbineTimingsSubdir = vbaDir.resolve("turbine-timings");
        if (!Files.exists(turbineTimingsSubdir)) {
          Files.createDirectories(turbineTimingsSubdir);
        }

        Path timingFile = turbineTimingsSubdir.resolve(fileName);
        try (FileWriter writer = new FileWriter(timingFile.toFile())) {
          OBJECT_MAPPER.writeValue(writer, timings);
          LOG.debug("Timing data written to viewable build artifacts: {}", timingFile);
        }
      } catch (Exception e) {
        LOG.debug(
          "Unable to write timing data to viewable build artifacts directory: {}",
          e.getMessage()
        );
      }
    }
  }
}
