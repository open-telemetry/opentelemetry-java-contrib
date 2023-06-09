package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FolderManager;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDiskExporter<EXPORT_DATA> {
  private final Storage storage;

  public AbstractDiskExporter(File rootDir, StorageConfiguration configuration) {
    this.storage = new Storage(new FolderManager(getSignalFolder(rootDir), configuration));
  }

  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    AtomicBoolean exportSucceeded = new AtomicBoolean(false);
    boolean foundDataToExport =
        storage.read(
            bytes -> {
              CompletableResultCode join =
                  doExport(getSerializer().deserialize(bytes)).join(timeout, unit);
              exportSucceeded.set(join.isSuccess());
              return exportSucceeded.get();
            });
    return foundDataToExport && exportSucceeded.get();
  }

  protected abstract String getStorageFolderName();

  protected abstract CompletableResultCode doExport(Collection<EXPORT_DATA> data);

  protected abstract SignalSerializer<EXPORT_DATA> getSerializer();

  protected void onShutDown() throws IOException {
    storage.close();
  }

  protected CompletableResultCode onExport(Collection<EXPORT_DATA> spans) {
    try {
      storage.write(getSerializer().serialize(spans));
      return CompletableResultCode.ofSuccess();
    } catch (IOException e) {
      return doExport(spans);
    }
  }

  private File getSignalFolder(File rootDir) {
    File folder = new File(rootDir, getStorageFolderName());
    if (!folder.exists()) {
      if (!folder.mkdirs()) {
        throw new IllegalStateException("Could not create the signal folder");
      }
    }
    return folder;
  }
}
