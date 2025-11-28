# JVM Metrics

The JMX Metric Gatherer provides built in JVM metric gathering capabilities.

## Metrics

### jvm.classes.loaded

* Name: `jvm.classes.loaded`
* Description: The number of loaded classes
* Unit: `1`
* Instrument Type: LongValueObserver

### jvm.gc.collections.count

* Name: `jvm.gc.collections.count`
* Description: The total number of garbage collections that have occurred
* Unit: `1`
* Instrument Type: LongSumObserver

### jvm.gc.collections.elapsed

* Name: `jvm.gc.collections.elapsed`
* Description: The approximate accumulated collection elapsed time
* Unit: `ms`
* Instrument Type: LongSumObserver

### jvm.memory.heap.init

* Name: `jvm.memory.heap.init`
* Description: The initial amount of memory that the JVM requests from the operating system for the heap
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.heap.max

* Name: `jvm.memory.heap.max`
* Description: The maximum amount of memory can be used for the heap
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.heap.used

* Name: `jvm.memory.heap.used`
* Description: The current heap memory usage
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.heap.committed

* Name: `jvm.memory.heap.committed`
* Description: The amount of memory that is guaranteed to be available for the heap
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.nonheap.init

* Name: `jvm.memory.nonheap.init`
* Description: The initial amount of memory that the JVM requests from the operating system for non-heap purposes
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.nonheap.max

* Name: `jvm.memory.nonheap.max`
* Description: The maximum amount of memory can be used for non-heap purposes
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.nonheap.used

* Name: `jvm.memory.nonheap.used`
* Description: The current non-heap memory usage
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.nonheap.committed

* Name: `jvm.memory.nonheap.committed`
* Description: The amount of memory that is guaranteed to be available for non-heap purposes
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.pool.init

* Name: `jvm.memory.pool.init`
* Description: The initial amount of memory that the JVM requests from the operating system for the memory pool
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.pool.max

* Name: `jvm.memory.pool.max`
* Description: The maximum amount of memory can be used for the memory pool
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.pool.used

* Name: `jvm.memory.pool.used`
* Description: The current memory pool memory usage
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.memory.pool.committed

* Name: `jvm.memory.pool.committed`
* Description: The amount of memory that is guaranteed to be available for the memory pool
* Unit: `by`
* Instrument Type: LongValueObserver

### jvm.threads.count

* Name: `jvm.threads.count`
* Description: The current number of threads
* Unit: `1`
* Instrument Type: LongValueObserver
