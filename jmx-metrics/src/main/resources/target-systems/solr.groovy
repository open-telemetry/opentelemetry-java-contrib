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

def beanSolrCoreSearcherNumDocs = otel.mbeans("solr:dom1=core,dom2=*,category=SEARCHER,scope=searcher,name=numDocs")
otel.instrument(beanSolrCoreSearcherNumDocs, "solr.document.count", "The total number of indexed documents.", "{documents}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") }],
  "Value", otel.&longUpDownCounterCallback)

def beanSolrCoreIndexSize = otel.mbeans("solr:dom1=core,dom2=*,category=INDEX,name=sizeInBytes")
otel.instrument(beanSolrCoreIndexSize, "solr.index.size", "The total index size.", "by",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") }],
  "Value", otel.&longUpDownCounterCallback)

def beanSolrCoreRequests = otel.mbeans(["solr:dom1=core,dom2=*,category=QUERY,scope=*,name=requests",
                                        "solr:dom1=core,dom2=*,category=UPDATE,scope=*,name=requests"])
otel.instrument(beanSolrCoreRequests, "solr.request.count", "The number of queries made.", "{queries}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "type" : { mbean -> mbean.name().getKeyProperty("category") },
   "handler" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "Count", otel.&longCounterCallback)

def beanSolrCoreRequestTimes = otel.mbeans(["solr:dom1=core,dom2=*,category=QUERY,scope=*,name=requestTimes",
                                        "solr:dom1=core,dom2=*,category=UPDATE,scope=*,name=requestTimes"])
otel.instrument(beanSolrCoreRequestTimes, "solr.request.time.average",
  "The average time of a query, based on Solr's histogram configuration.",
  "ms",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "type" : { mbean -> mbean.name().getKeyProperty("category") },
   "handler" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "Mean", otel.&doubleValueCallback)

def beanSolrCoreErrors = otel.mbeans(["solr:dom1=core,dom2=*,category=QUERY,scope=*,name=errors",
                                            "solr:dom1=core,dom2=*,category=UPDATE,scope=*,name=errors"])
otel.instrument(beanSolrCoreErrors, "solr.request.error.count", "The number of queries resulting in an error.", "{queries}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "type" : { mbean -> mbean.name().getKeyProperty("category") },
   "handler" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "Count", otel.&longCounterCallback)

def beanSolrCoreTimeouts = otel.mbeans(["solr:dom1=core,dom2=*,category=QUERY,scope=*,name=timeouts",
                                      "solr:dom1=core,dom2=*,category=UPDATE,scope=*,name=timeouts"])
otel.instrument(beanSolrCoreTimeouts, "solr.request.timeout.count", "The number of queries resulting in a timeout.", "{queries}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "type" : { mbean -> mbean.name().getKeyProperty("category") },
   "handler" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "Count", otel.&longCounterCallback)

def beanSolrCoreQueryResultsCache = otel.mbeans("solr:dom1=core,dom2=*,category=CACHE,scope=*,name=queryResultCache")
otel.instrument(beanSolrCoreQueryResultsCache, "solr.cache.eviction.count", "The number of evictions from a cache.", "{evictions}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "cache" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "cumulative_evictions", otel.&longCounterCallback)
otel.instrument(beanSolrCoreQueryResultsCache, "solr.cache.hit.count", "The number of hits for a cache.", "{hits}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "cache" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "cumulative_hits", otel.&longCounterCallback)
otel.instrument(beanSolrCoreQueryResultsCache, "solr.cache.insert.count", "The number of inserts to a cache.", "{inserts}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "cache" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "cumulative_inserts", otel.&longCounterCallback)
otel.instrument(beanSolrCoreQueryResultsCache, "solr.cache.lookup.count", "The number of lookups to a cache.", "{lookups}",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "cache" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "cumulative_lookups", otel.&longCounterCallback)
otel.instrument(beanSolrCoreQueryResultsCache, "solr.cache.size", "The size of the cache occupied in memory.", "by",
  ["core" : { mbean -> mbean.name().getKeyProperty("dom2") },
   "cache" : { mbean -> mbean.name().getKeyProperty("scope") }],
  "ramBytesUsed", otel.&longUpDownCounterCallback)
