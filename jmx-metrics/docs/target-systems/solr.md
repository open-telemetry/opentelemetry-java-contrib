# Solr Metrics

The JMX Metric Gatherer provides built in Solr metric gathering capabilities.

## Metrics

### Core Metrics

* Name: `solr.document.count`
* Description: The total number of indexed documents.
* Unit: `{documents}`
* Labels: `core`
* Instrument Type: ObservableLongUpDownCounter


* Name: `solr.index.size`
* Description: The total index size.
* Unit: `by`
* Labels: `core`
* Instrument Type: ObservableLongUpDownCounter


* Name: `solr.request.count`
* Description: The number of queries made.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter


* Name: `solr.request.time.average`
* Description: The average time of a query. This average is based on the histogram configuration
    in Solr. By default, Solr uses an exponentially decaying reservoir.
* Unit: `ms`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableDoubleValue


* Name: `solr.request.error.count`
* Description: The number of queries resulting in an error.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter


* Name: `solr.request.timeout.count`
* Description: The number of queries resulting in a timeout.
* Unit: `{queries}`
* Labels: `core`, `type`, `handler`
* Instrument Type: ObservableLongCounter


* Name: `solr.cache.eviction.count`
* Description: The number of evictions from a cache.
* Unit: `{evictions}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter


* Name: `solr.cache.hit.count`
* Description: The number of hits from a cache.
* Unit: `{hits}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter


* Name: `solr.cache.insert.count`
* Description: The number of inserts from a cache.
* Unit: `{inserts}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter


* Name: `solr.cache.lookup.count`
* Description: The number of lookups from a cache.
* Unit: `{lookups}`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongCounter


* Name: `solr.cache.size`
* Description: The size of the cache occupied in memory.
* Unit: `by`
* Labels: `core`, `cache`
* Instrument Type: ObservableLongUpDownCounter
