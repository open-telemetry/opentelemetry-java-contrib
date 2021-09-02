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

import io.opentelemetry.api.metrics.common.Labels

def helper = otel.mbeans([
    "org.apache.cassandra.metrics:type=ThreadPools,path=*,scope=*,name=PendingTasks",
    "org.apache.cassandra.metrics:type=ThreadPools,path=*,scope=*,name=ActiveTasks"
])
otel.instrument(helper, "cassandra.current_tasks", "Number of tasks in queue with the given task status.", "1",
        ["stage_name":{ mbean -> mbean.name().getKeyProperty("scope")},
            "task_status":{ mbean -> mbean.name().getKeyProperty("name")}],
        "Value", otel.&doubleValueObserver)
