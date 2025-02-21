/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

def classLoading = otel.mbean("java.lang:type=ClassLoading")
otel.instrument(classLoading, "jvm.classes.loaded", "number of loaded classes",
        "{class}", "LoadedClassCount", otel.&longValueCallback)

def garbageCollector = otel.mbeans("java.lang:type=GarbageCollector,*")
otel.instrument(garbageCollector, "jvm.gc.collections.count", "total number of collections that have occurred",
        "{collection}", ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "CollectionCount", otel.&longCounterCallback)
otel.instrument(garbageCollector, "jvm.gc.collections.elapsed",
        "the approximate accumulated collection elapsed time in milliseconds", "ms",
        ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "CollectionTime", otel.&longCounterCallback)

def memory = otel.mbean("java.lang:type=Memory")
otel.instrument(memory, "jvm.memory.heap", "current heap usage",
        "By", "HeapMemoryUsage", otel.&longValueCallback)
otel.instrument(memory, "jvm.memory.nonheap", "current non-heap usage",
        "By", "NonHeapMemoryUsage", otel.&longValueCallback)

def runtime = otel.mbean("java.lang:type=Runtime")
otel.instrument(runtime, "jvm.runtime.uptime", "uptime",
        "ms", "Uptime", otel.&longValueCallback)

def os = otel.mbean("java.lang:type=OperatingSystem")
otel.instrument(os, "jvm.fd.open", "open file descriptors",
        "1", "OpenFileDescriptorCount", otel.&longValueCallback)


def memoryPool = otel.mbeans("java.lang:type=MemoryPool,*")
otel.instrument(memoryPool, "jvm.memory.pool", "current memory pool usage",
        "By", ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "Usage", otel.&longValueCallback)

def threading = otel.mbean("java.lang:type=Threading")
otel.instrument(threading, "jvm.threads.count", "number of threads",
        "{thread}", "ThreadCount", otel.&longValueCallback)
