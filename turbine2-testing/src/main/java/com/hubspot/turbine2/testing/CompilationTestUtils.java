package com.hubspot.turbine2.testing;

import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class CompilationTestUtils {

  private CompilationTestUtils() {
    throw new IllegalArgumentException("Static utility class.");
  }

  public static void printSource(
    Compilation compilation,
    Predicate<String> sourceFilter
  ) {
    if (!shouldPrintSource()) {
      System.out.println(
        "Not printing generated output, please set PRINT_GENERATED=TRUE if you want to see generated output."
      );
      return;
    }
    for (JavaFileObject generatedSourceFile : compilation.generatedSourceFiles()) {
      System.out.println("File " + generatedSourceFile.getName());
      try {
        String content = generatedSourceFile.getCharContent(false).toString();

        if (sourceFilter.test(content)) {
          System.out.println(generatedSourceFile.getCharContent(false));
        }
      } catch (IOException e) {
        System.out.println("Failed to print " + generatedSourceFile.getName());
      }
    }
  }

  public static void printResources(Compilation compilation) {
    if (!shouldPrintSource()) {
      System.out.println(
        "Not printing generated output, please set PRINT_GENERATED=TRUE if you want to see generated output."
      );
      return;
    }
    for (JavaFileObject generatedFile : compilation
      .generatedFiles()
      .stream()
      .filter(g -> g.getKind() != Kind.SOURCE)
      .collect(Collectors.toList())) {
      System.out.println("File " + generatedFile.getName());
      try {
        System.out.println(generatedFile.getCharContent(false));
      } catch (IOException e) {
        System.out.println("Failed to print " + generatedFile.getName());
      }
    }
  }

  public static void printDiagnostics(Compilation compilation) {
    System.out.println("Compiler Diagnostics:\n\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics()) {
      System.out.println(diagnostic.getMessage(Locale.ENGLISH));
    }

    System.out.println("\n\nEnd Compiler Diagnostics");
  }

  private static boolean shouldPrintSource() {
    return "true".equalsIgnoreCase(System.getenv("PRINT_GENERATED"));
  }
}
