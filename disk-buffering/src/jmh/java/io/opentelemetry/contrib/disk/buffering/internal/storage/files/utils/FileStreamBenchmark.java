/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class FileStreamBenchmark {

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void truncateTop(FileStreamState state) throws IOException {
    state.fileStream.truncateTop(state.offset);
  }

  @State(Scope.Benchmark)
  public static class FileStreamState {
    public FileStream fileStream;
    public int offset;
    private byte[] data;
    private File file;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
      file = File.createTempFile("input", ".txt");
      data = new byte[1024 * 1024]; // 1MB
      offset = 512;
    }

    // truncateTop modifies the file, so each invocation needs a fresh copy.
    @Setup(Level.Invocation)
    public void resetFile() throws IOException {
      Files.write(file.toPath(), data);
      fileStream = FileStream.create(file);
    }

    @TearDown(Level.Invocation)
    public void closeStream() throws IOException {
      fileStream.close();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      file.delete();
    }
  }
}
