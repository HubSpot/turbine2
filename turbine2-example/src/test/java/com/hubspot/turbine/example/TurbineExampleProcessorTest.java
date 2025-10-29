package com.hubspot.turbine.example;

import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import com.hubspot.turbine2.example.TurbineExampleProcessor;
import java.io.IOException;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

public class TurbineExampleProcessorTest {

  private final TurbineExampleProcessor exampleProcessor = new TurbineExampleProcessor();

  // This is using the fully qualified class name to avoid the 'BanHiddenImport' error prone check
  private final org.immutables.value.internal.$processor$.$Processor immutableProcessor =
    new org.immutables.value.internal.$processor$.$Processor();

  @Test
  public void itCompilesAGoodCaseProperly() throws IOException {
    Compilation compilation = javac()
      .withProcessors(immutableProcessor, exampleProcessor)
      .compile(
        loadJavaFileObject("ExampleAnnotatedClass.java"),
        loadJavaFileObject("ExampleImmutableIF.java"),
        loadJavaFileObject("OtherAnnotatedClass.java"),
        loadJavaFileObject("OtherImmutableIF.java")
      );
    CompilationSubject.assertThat(compilation).succeeded();

    System.out.println("Compiler Diagnostics:\n\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics()) {
      System.out.println(diagnostic.getMessage(Locale.ENGLISH));
    }

    System.out.println("\n\nEnd Compiler Diagnostics");

    printSource(compilation);
  }

  @Test
  public void itFailsWithoutImmutables() throws IOException {
    Compilation compilation = javac()
      .withProcessors(exampleProcessor)
      .compile(
        loadJavaFileObject("ExampleAnnotatedClass.java"),
        loadJavaFileObject("ExampleImmutableIF.java")
      );
    CompilationSubject.assertThat(compilation).failed();

    System.out.println("Compiler Diagnostics:\n\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics()) {
      System.out.println(diagnostic.getMessage(Locale.ENGLISH));
    }

    System.out.println("\n\nEnd Compiler Diagnostics");
  }

  private static void printSource(Compilation compilation) {
    for (JavaFileObject generatedSourceFile : compilation.generatedSourceFiles()) {
      System.out.println("File " + generatedSourceFile.getName());
      try {
        String content = generatedSourceFile.getCharContent(false).toString();

        if (
          content.contains(
            "@javax.annotation.processing.Generated(\"org.immutables.processor.ProxyProcessor\")"
          )
        ) {
          System.out.println(
            "Skipping generated immutable for " + generatedSourceFile.getName()
          );
          continue;
        }

        System.out.println(generatedSourceFile.getCharContent(false));
      } catch (IOException e) {
        System.out.println("Failed to print " + generatedSourceFile.getName());
      }
    }
  }

  private static JavaFileObject loadJavaFileObject(String filename) {
    return JavaFileObjects.forResource("java-files/" + filename);
  }
}
