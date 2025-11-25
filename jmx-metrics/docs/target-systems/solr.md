# Solr Metrics

The JMX Metric Gatherer provides built in Solr metric gathering capabilities.
Details about using JMX with Solr can be found here: <https://solr.apache.org/guide/6_6/using-jmx-with-solr.html>

## Metrics

### solr.document.count

* Name: `solr.document.count`
* Description: The total number of indexed documents.
* Unit: `{documents}`
* Labels: `core`
* Instrument Type: ObservableLongUpDownCounter

### solr.index.size

* Name: `solr.index.size`
* Description: The total index size.
* Unit: `by`
* Labels: `core`
* Instrument Type: ObservableLongUpDownCounter

### solr.request.count

* Name: `solr.request.count`
* Description: The number of queries made.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter

### solr.request.time.average

* Name: `solr.request.time.average`
* Description: The average time of a query, based on Solr's histogram configuration.
* Unit: `ms`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableDoubleValue

### solr.request.error.count

* Name: `solr.request.error.count`
* Description: The number of queries resulting in an error.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter

### solr.request.timeout.count

* Name: `solr.request.timeout.count`
* Description: The number of queries resulting in a timeout.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter

### solr.cache.eviction.count

* Name: `solr.cache.eviction.count`
* Description: The number of evictions from a cache.
* Unit: `{evictions}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter

### solr.cache.hit.count

* Name: `solr.cache.hit.count`
* Description: The number of hits from a cache.
* Unit: `{hits}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter

### solr.cache.insert.count

* Name: `solr.cache.insert.count`
* Description: The number of inserts from a cache.
* Unit: `{inserts}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter

### solr.cache.lookup.count

* Name: `solr.cache.lookup.count`
* Description: The number of lookups from a cache.
* Unit: `{lookups}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter

### solr.cache.size

* Name: `solr.cache.size`
* Description: The size of the cache occupied in memory.
* Unit: `by`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongUpDownCounter
