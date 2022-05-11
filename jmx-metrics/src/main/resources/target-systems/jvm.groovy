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
otel.instrument(classLoading, "process.runtime.jvm.classes.loaded", "Number of classes currently loaded",
        "{classes}", "LoadedClassCount", otel.&longUpDownCounterCallback)
otel.instrument(classLoading, "process.runtime.jvm.classes.unloaded", "Number of classes unloaded since JVM start",
        "{classes}", "UnloadedClassCount", otel.&longUpDownCounterCallback)

def garbageCollector = otel.mbeans("java.lang:type=GarbageCollector,*")
otel.instrument(garbageCollector, "jvm.gc.collections.count", "total number of collections that have occurred",
        "1", ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "CollectionCount", otel.&longCounterCallback)
otel.instrument(garbageCollector, "jvm.gc.collections.elapsed",
        "the approximate accumulated collection elapsed time in milliseconds", "ms",
        ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "CollectionTime", otel.&longCounterCallback)

def memory = otel.mbean("java.lang:type=Memory")
otel.instrument(memory, "jvm.memory.heap", "current heap usage",
        "by", "HeapMemoryUsage", otel.&longValueCallback)
otel.instrument(memory, "jvm.memory.nonheap", "current non-heap usage",
        "by", "NonHeapMemoryUsage", otel.&longValueCallback)

def memoryPool = otel.mbeans("java.lang:type=MemoryPool,*")
otel.instrument(memoryPool, "jvm.memory.pool", "current memory pool usage",
        "by", ["name" : { mbean -> mbean.name().getKeyProperty("name") }],
        "Usage", otel.&longValueCallback)

def threading = otel.mbean("java.lang:type=Threading")
otel.instrument(threading, "process.runtime.jvm.threads.count", "Number of executing threads",
        "{threads}", "ThreadCount", otel.&longUpDownCounterCallback)
