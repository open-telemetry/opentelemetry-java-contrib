/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class FileTransferUtilBenchmark {

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void fileTransfer(FileTransferState state) throws IOException {
    state.fileTransferUtil.transferBytes(state.offset, state.amountOfBytesToTransfer);
  }

  @State(Scope.Benchmark)
  public static class FileTransferState {
    public FileTransferUtil fileTransferUtil;
    public int offset;
    public int amountOfBytesToTransfer;
    private File inputFile;
    private File outputFile;

    @Setup
    public void setUp() throws IOException {
      outputFile = File.createTempFile("output", ".txt");
      inputFile = File.createTempFile("input", ".txt");
      int totalDataSize = 1024 * 1024; // 1MB
      byte[] data = new byte[totalDataSize];
      Files.write(inputFile.toPath(), data, StandardOpenOption.CREATE);
      fileTransferUtil = new FileTransferUtil(new FileInputStream(inputFile), outputFile);
      offset = 512;
      amountOfBytesToTransfer = totalDataSize - offset;
    }

    @TearDown
    public void tearDown() throws IOException {
      fileTransferUtil.close();
      inputFile.delete();
      outputFile.delete();
    }
  }
}
