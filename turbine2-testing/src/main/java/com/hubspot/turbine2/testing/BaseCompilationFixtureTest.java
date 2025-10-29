package com.hubspot.turbine2.testing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.testing.compile.Compiler.javac;
import static com.hubspot.turbine2.testing.FixtureConstants.JAVA_FILES_FIXTURES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.common.io.Resources;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import com.hubspot.turbine2.testing.diff.DefaultFixtureFileHandler;
import com.hubspot.turbine2.testing.diff.FixtureFileTypeHandler;
import com.hubspot.turbine2.testing.diff.JavaFileHandler;
import com.hubspot.turbine2.testing.diff.ServiceLoaderFileHandler;
import com.hubspot.turbine2.testing.diff.SourceLocations;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A utility class for automatically generating and evaluating
 * test fixtures for generated source code.
 * </p>
 *
 * <p>
 * Implementers will subclass and call {@link #runFixtureTest(String...)}
 * with a list of expected input file names. The files are specified in paths
 * relative to the src/test/resources/java-files directory.
 * </p>
 *
 * <p>
 *   The overridden test class must also define {@link #getTestMode()}
 *   which indicates whether we are generating new fixtures, or
 *   just evaluating against what we have currently.
 * </p>
 *
 */
public abstract class BaseCompilationFixtureTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    BaseCompilationFixtureTest.class
  );
  private static final HashFunction MURMUR_HASH = Hashing.murmur3_32_fixed();
  private static final TypeReference<List<GeneratedSourceParameters>> PARAMETERS_TYPE_REFERENCE =
    new TypeReference<>() {};

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new Jdk8Module())
    .registerModule(new GuavaModule())
    .enable(SerializationFeature.INDENT_OUTPUT);

  private final Supplier<List<FixtureFileTypeHandler>> cachedTypeHandlers =
    Suppliers.memoize(this::getFixtureFileTypeHandlers);
  private final Class<?> testClass;
  private static Class<?> testClassForJarPath;
  private static final Map<String, Multimap<Set<String>, String>> GENERATED_SOURCES_ACCUMULATOR =
    new HashMap<>();
  private static File fixtureOutputTemporaryDirectory;
  private static boolean generatedSourceFiles = false;

  protected BaseCompilationFixtureTest() {
    this.testClass = getClass();
    testClassForJarPath = this.testClass;
  }

  protected final void runFixtureTest(Collection<String> sourceNames) throws IOException {
    runFixtureTest(sourceNames.toArray(String[]::new));
  }

  /**
   * Call this with the set of all sources you want to compile. The compilation will
   * use the processor(s) provided in an overridden {@link #getProcessors()} method.
   */
  protected final void runFixtureTest(String... sourceNames) throws IOException {
    String testName = getTestMethodName();
    ImmutableSet<String> sourceSet = ImmutableSet.copyOf(sourceNames);

    List<JavaFileObject> inputs = sourceSet
      .stream()
      .map(BaseCompilationFixtureTest::loadJavaFileObject)
      .collect(Collectors.toList());

    Compilation compilation = javac().withProcessors(getProcessors()).compile(inputs);
    if (getEnableDebugOutput()) {
      CompilationTestUtils.printSource(compilation, this::includeGeneratedSource);
      CompilationTestUtils.printResources(compilation);
      CompilationTestUtils.printDiagnostics(compilation);
    }
    CompilationSubject.assertThat(compilation).succeeded();

    switch (getActualTestMode()) {
      case GENERATE_FIXTURES:
        generatedSourceFiles = true;
        accumulateGeneratedData(testName, sourceSet, compilation);
        break;
      case EVALUATE:
        evaluateGeneratedData(testName, sourceSet, compilation);
        break;
      default:
        throw new IllegalArgumentException(
          "Unexpected test mode: " + getActualTestMode()
        );
    }
  }

  /**
   * Required override to specify the processors.
   */
  protected abstract Iterable<? extends Processor> getProcessors();

  /**
   * Define which test mode to be using. You probably want to
   * evaluate but when you're generating code locally you can use
   * {@link FixtureTestMode#GENERATE_FIXTURES}.
   */
  protected abstract FixtureTestMode getTestMode();

  protected boolean getEnableDebugOutput() {
    return false;
  }

  protected boolean includeGeneratedSource(String sourceContent) {
    // Ignore immutables generated code by default
    return !sourceContent.contains(
      "@javax.annotation.processing.Generated(\"org.immutables.processor.ProxyProcessor\")"
    );
  }

  /**
   * The list of "diff checkers" which will allow one to override
   * diff behavior for classes of files.
   */
  protected List<FixtureFileTypeHandler> getFixtureFileTypeHandlers() {
    return ImmutableList.of(
      new JavaFileHandler(),
      new ServiceLoaderFileHandler(),
      new DefaultFixtureFileHandler()
    );
  }

  /**
   * Override to dictate whether files should be suffixed
   * with a hash of the input file names to prevent clobbering.
   */
  protected boolean shouldDisambiguateFile(JavaFileObject javaFileObject) {
    return getFixtureFileHandler(javaFileObject)
      .shouldDisambiguateFile(javaFileObject.getName());
  }

  // useful for Arguments builders
  protected static List<String> list(String... elems) {
    return ImmutableList.copyOf(elems);
  }

  @BeforeAll
  static void baseCompilationBeforeClass() throws IOException {
    fixtureOutputTemporaryDirectory = Files.createTempDirectory("fixtures-").toFile();
    generatedSourceFiles = false;
    GENERATED_SOURCES_ACCUMULATOR.clear();
  }

  @AfterAll
  static void baseCompilationAfterClass() throws IOException {
    if (!generatedSourceFiles) {
      return;
    }

    String testClassPath = testClassForJarPath
      .getProtectionDomain()
      .getCodeSource()
      .getLocation()
      .getFile();

    checkArgument(
      testClassPath.contains("/target/"),
      "Could not find test source directory. Are we running in a JAR?"
    );

    String basePath = testClassPath.substring(0, testClassPath.indexOf("/target/"));
    File fixtureBaseDirectory = new File(
      basePath,
      "src/test/resources/" + JAVA_FILES_FIXTURES_DIR
    );

    for (String testName : GENERATED_SOURCES_ACCUMULATOR.keySet()) {
      File testTempDirectory = new File(fixtureOutputTemporaryDirectory, testName);
      Multimap<Set<String>, String> testGeneratedSourceFileNames =
        GENERATED_SOURCES_ACCUMULATOR.get(testName);
      List<GeneratedSourceParameters> parameters = new ArrayList<>();
      for (Entry<Set<String>, Collection<String>> generatedEntry : testGeneratedSourceFileNames
        .asMap()
        .entrySet()) {
        parameters.add(
          GeneratedSourceParameters.of(generatedEntry.getKey(), generatedEntry.getValue())
        );
      }
      parameters.sort(Comparator.comparing(x -> x.getInputFiles().toString()));

      File parametersFile = new File(testTempDirectory, EXPECTED_GENERATED_FILES_JSON);
      MoreFiles.createParentDirectories(parametersFile.toPath());
      prettyPrint(parametersFile, parameters);

      File testFixturePath = new File(fixtureBaseDirectory, testName);
      try {
        MoreFiles.deleteRecursively(
          testFixturePath.toPath(),
          RecursiveDeleteOption.ALLOW_INSECURE
        );
      } catch (Throwable ignored) {}
      FileUtils.moveDirectory(testTempDirectory, testFixturePath);
      LOGGER.info("Wrote fixtures for {} to {}", testName, testFixturePath);
    }
  }

  //
  // Private methods below
  //
  private void evaluateGeneratedData(
    String testName,
    ImmutableSet<String> sourceSet,
    Compilation compilation
  ) throws IOException {
    String hashSuffix = hashSuffix(sourceSet);
    Set<String> expectedOutputFileNames = getExpectedOutputFiles(testName, sourceSet);
    List<JavaFileObject> relevantOutputFiles = getRelevantFiles(compilation);
    Map<String, String> unHashedFileNames = new HashMap<>();

    Set<String> actualOutputFileNames = relevantOutputFiles
      .stream()
      .map(fileObject -> {
        String hashed = maybeAppendHashSuffix(fileObject, hashSuffix);
        unHashedFileNames.put(hashed, fileObject.getName());
        return hashed;
      })
      .collect(Collectors.toSet());
    assertThat(actualOutputFileNames)
      .containsExactlyInAnyOrderElementsOf(expectedOutputFileNames);

    FixtureLoader fixtureLoader = new FixtureLoader(testName);

    for (String fileName : actualOutputFileNames) {
      String actualSourceName = normalizeOutputName(unHashedFileNames.get(fileName));
      StandardLocation location = getLocation(fileName);
      Optional<JavaFileObject> javaFileObject = compilation.generatedFile(
        location,
        actualSourceName
      );
      assertThat(javaFileObject)
        .as("Expecting %s to have been generated during compilation.", fileName)
        .isPresent();
      FileDiffRequest diffRequest = FileDiffRequest
        .builder()
        .setCompilation(compilation)
        .setCompilationOutput(javaFileObject.get())
        .setFixtureLoader(fixtureLoader)
        .setFixtureFileName(fileName)
        .setSourceLocation(location)
        .setSourceName(actualSourceName)
        .build();
      getFixtureFileHandler(location, actualSourceName).checkFileDiff(diffRequest);
    }
  }

  private StandardLocation getLocation(String fileName) {
    String locationName = Splitter
      .on('/')
      .omitEmptyStrings()
      .split(fileName)
      .iterator()
      .next();
    return (StandardLocation) StandardLocation.locationFor(locationName);
  }

  private void accumulateGeneratedData(
    String testName,
    ImmutableSet<String> sourceSet,
    Compilation compilation
  ) {
    Multimap<Set<String>, String> testOutputs =
      GENERATED_SOURCES_ACCUMULATOR.computeIfAbsent(
        testName,
        ignored -> HashMultimap.create()
      );
    String hashSuffix = hashSuffix(sourceSet);
    for (JavaFileObject generatedFile : getRelevantFiles(compilation)) {
      testOutputs.put(sourceSet, writeOutputFile(testName, hashSuffix, generatedFile));
    }
  }

  private String hashSuffix(ImmutableSet<String> sourceSet) {
    Hasher hasher = MURMUR_HASH.newHasher();
    for (String source : ImmutableList.sortedCopyOf(sourceSet)) {
      hasher.putString(source, StandardCharsets.UTF_8);
    }
    return "_" + hasher.hash();
  }

  private String maybeAppendHashSuffix(JavaFileObject javaFileObject, String hashSuffix) {
    if (shouldDisambiguateFile(javaFileObject)) {
      return javaFileObject.getName() + hashSuffix;
    } else {
      return javaFileObject.getName();
    }
  }

  private FixtureFileTypeHandler getFixtureFileHandler(
    StandardLocation fileLocation,
    String sourceName
  ) {
    for (FixtureFileTypeHandler handler : cachedTypeHandlers.get()) {
      if (handler.isApplicable(fileLocation, sourceName)) {
        return handler;
      }
    }
    throw new IllegalArgumentException(
      "Invalid source: " + fileLocation + " " + sourceName
    );
  }

  private FixtureFileTypeHandler getFixtureFileHandler(JavaFileObject javaFileObject) {
    return getFixtureFileHandler(
      SourceLocations.getLocation(javaFileObject),
      javaFileObject.getName()
    );
  }

  private String writeOutputFile(
    String testName,
    String hashSuffix,
    JavaFileObject generatedSourceFile
  ) {
    try {
      String content = generatedSourceFile.getCharContent(false).toString();
      String name = maybeAppendHashSuffix(generatedSourceFile, hashSuffix);
      File outputFile = new File(fixtureOutputTemporaryDirectory, testName + "/" + name);
      try {
        com.google.common.io.Files.createParentDirs(outputFile);
      } catch (Throwable ignored) {}
      com.google.common.io.Files
        .asCharSink(outputFile, StandardCharsets.UTF_8)
        .write(content);
      getFixtureFileHandler(generatedSourceFile).postProcessGeneratedFile(outputFile);
      return name;
    } catch (IOException e) {
      throw new RuntimeException(
        String.format("Failed to write fixture %s", generatedSourceFile.getName()),
        e
      );
    }
  }

  private Set<String> getExpectedOutputFiles(
    String testName,
    ImmutableSet<String> sourceNames
  ) throws IOException {
    if (getActualTestMode() == FixtureTestMode.GENERATE_FIXTURES) {
      return ImmutableSet.of();
    } else {
      List<GeneratedSourceParameters> generatedSourceParameters = OBJECT_MAPPER.readValue(
        Resources.getResource(getExpectedResourceFileName(testName)),
        PARAMETERS_TYPE_REFERENCE
      );
      for (GeneratedSourceParameters parameters : generatedSourceParameters) {
        if (Objects.equals(parameters.getInputFiles(), sourceNames)) {
          return parameters.getExpectedOutputFiles();
        }
      }
      throw new IllegalArgumentException(
        "Could not find expected output files for " + sourceNames
      );
    }
  }

  private List<JavaFileObject> getRelevantFiles(Compilation compilation) {
    return compilation
      .generatedFiles()
      .stream()
      .filter(gf -> {
        if (gf.getLastModified() <= 0) {
          return false;
        }
        try {
          // ignore class files.
          if (gf.getKind() == Kind.CLASS) {
            return false;
          }
          return (
            (gf.getKind() != Kind.SOURCE) ||
            includeGeneratedSource(gf.getCharContent(true).toString())
          );
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .collect(Collectors.toList());
  }

  private static String getExpectedResourceFileName(String testName) {
    return Paths
      .get(JAVA_FILES_FIXTURES_DIR, testName, EXPECTED_GENERATED_FILES_JSON)
      .toString();
  }

  private static final String EXPECTED_GENERATED_FILES_JSON =
    "expected_generated_files.json";

  // removes base values out of the generated name like GENERATED_SOURCES/fixtures/ ...
  private static String normalizeOutputName(String outputFile) {
    return Iterators.getLast(Splitter.on('/').limit(3).split(outputFile).iterator());
  }

  private String getTestMethodName() {
    String candidate = "unknown";
    String testClassName = testClass.getName();
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      if (Objects.equals(testClassName, element.getClassName())) {
        String methodName = Splitter
          .on('(')
          .split(element.getMethodName())
          .iterator()
          .next();
        for (Method method : testClass.getDeclaredMethods()) {
          if (Objects.equals(methodName, method.getName())) {
            if ((method.getModifiers() & Modifier.PRIVATE) == 0) {
              candidate = element.getMethodName();
            }
          }
        }
      }
    }
    return candidate;
  }

  private FixtureTestMode getActualTestMode() {
    FixtureTestMode testMode = getTestMode();
    if (System.getenv("BLAZAR_COORDINATES") != null) {
      checkArgument(
        testMode != FixtureTestMode.GENERATE_FIXTURES,
        "Cannot generate fixtures in blazar!"
      );
    }
    return testMode;
  }

  private static JavaFileObject loadJavaFileObject(String fileName) {
    return JavaFileObjects.forResource("java-files/" + fileName);
  }

  private static void prettyPrint(File outputFile, Object obj) throws IOException {
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    OBJECT_MAPPER.writer(prettyPrinter).writeValue(outputFile, obj);
  }
}
