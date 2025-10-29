package com.hubspot.turbine2.testing.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.hubspot.turbine2.testing.FileDiffRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.StandardLocation;

public class ServiceLoaderFileHandler implements FixtureFileTypeHandler {

  @Override
  public boolean isApplicable(StandardLocation fileLocation, String sourceName) {
    return (
      StandardLocation.CLASS_OUTPUT.equals(fileLocation) &&
      sourceName.contains("META-INF/services/")
    );
  }

  @Override
  public void checkFileDiff(FileDiffRequest request) throws IOException {
    // we have to load the lines as sets and assert that they are equivalent.
    List<String> compilationLines;
    List<String> fixtureLines;

    try (
      InputStream compilationInputStream = request
        .getCompilationOutput()
        .openInputStream();
      InputStreamReader reader = new InputStreamReader(compilationInputStream)
    ) {
      compilationLines =
        CharStreams
          .readLines(reader)
          .stream()
          .filter(x -> !x.isBlank())
          .collect(Collectors.toList());
    }

    fixtureLines =
      request
        .loadFixtureAsByteSource()
        .asCharSource(StandardCharsets.UTF_8)
        .readLines()
        .stream()
        .filter(x -> !x.isBlank())
        .collect(Collectors.toList());

    assertThat(ImmutableSet.copyOf(compilationLines))
      .as("Service Loader contains same classes: %s", request.getSourceName())
      .hasSameElementsAs(ImmutableSet.copyOf(fixtureLines));
  }

  @Override
  public boolean shouldDisambiguateFile(String name) {
    return true;
  }

  @Override
  public void postProcessGeneratedFile(File outputFile) throws IOException {
    // just sort in memory. should be efficient enough since service load files are pretty small
    List<String> lines = ImmutableList.sortedCopyOf(
      Files.readLines(outputFile, StandardCharsets.UTF_8)
    );
    try (
      FileWriter fileWriter = new FileWriter(outputFile);
      BufferedWriter writer = new BufferedWriter(fileWriter)
    ) {
      for (String line : lines) {
        writer.write(line);
        writer.write('\n');
      }
    }
  }
}
