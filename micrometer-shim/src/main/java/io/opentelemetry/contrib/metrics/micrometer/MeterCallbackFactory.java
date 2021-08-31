package io.opentelemetry.contrib.metrics.micrometer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Super hacky way to tie into the export cycle of the MeterRegistry by registering a single sentinel instrument which reports
 * no measurements but can intercept when it is being iterated in order to execute registered callbacks.
 */
final class MeterCallbackFactory implements Consumer<Runnable> {
    private static final String SENTINEL_METER_NAME = "__otel_metrics_callback_sentinel";

    private final Object lock = new Object();
    private final Supplier<MeterRegistry> meterRegistrySupplier;
    private final List<Runnable> tasks;
    private volatile boolean initialized;

    MeterCallbackFactory(Supplier<MeterRegistry> meterRegistrySupplier) {
        this.meterRegistrySupplier = meterRegistrySupplier;
        this.tasks = new CopyOnWriteArrayList<>();
    }

    @Override
    public void accept(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        ensureInitialized();
        tasks.add(runnable);
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (lock) {
            if (initialized) {
                return;
            }
            MeterRegistry meterRegistry = meterRegistrySupplier.get();
            Meter.builder(SENTINEL_METER_NAME, Meter.Type.OTHER, new TaskIterable())
                    .tags(Tags.empty())
                    .description("Sentinel meter")
                    .register(meterRegistry);
            initialized = true;
        }
    }

    private class TaskIterable implements Iterable<Measurement> {
        @Override
        public Iterator<Measurement> iterator() {
            List<Exception> exceptions = null;
            for (Runnable task : tasks) {
                try {
                    task.run();
                } catch (Exception exception) {
                    if (exceptions == null) {
                        exceptions = new ArrayList<>();
                    }
                    exceptions.add(exception);
                }
            }
            if (exceptions != null) {
                RuntimeException exception = new RuntimeException("Exceptions observed in callback.");
                exceptions.forEach(exception::addSuppressed);
                throw exception;
            }
            return NoopIterator.INSTANCE;
        }
    }

    private enum NoopIterator implements Iterator<Measurement> {
        INSTANCE;

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Measurement next() {
            throw new NoSuchElementException();
        }
    }
}