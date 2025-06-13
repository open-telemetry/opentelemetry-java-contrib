# Produced Metrics


## Metric `mq.message.retry.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.retry.count` | Gauge | `{messages}` | Number of message retries | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.retry.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.status` | Gauge | `1` | Channel status | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.max.sharing.conversations`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.max.sharing.conversations` | Gauge | `{conversations}` | Maximum number of conversations permitted on this channel instance. | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.max.sharing.conversations` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.current.sharing.conversations`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.current.sharing.conversations` | Gauge | `{conversations}` | Current number of conversations permitted on this channel instance. | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.current.sharing.conversations` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.byte.received`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.byte.received` | Gauge | `{bytes}` | Number of bytes received | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.byte.received` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.byte.sent`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.byte.sent` | Gauge | `{bytes}` | Number of bytes sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.byte.sent` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.buffers.received`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.buffers.received` | Gauge | `{buffers}` | Buffers received | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.buffers.received` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.buffers.sent`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.buffers.sent` | Gauge | `{buffers}` | Buffers sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.buffers.sent` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.message.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.count` | Gauge | `{messages}` | Message count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.open.input.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.open.input.count` | Gauge | `{applications}` | Count of applications sending messages to the queue | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.open.input.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.open.output.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.open.output.count` | Gauge | `{applications}` | Count of applications consuming messages from the queue | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.open.output.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.high.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.high.queue.depth` | Gauge | `{percent}` | The current high queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.high.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.service.interval`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.service.interval` | Gauge | `{percent}` | The queue service interval | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.service.interval` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.queue.depth.full.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.queue.depth.full.event` | Counter | `{events}` | The number of full queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.queue.depth.full.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.queue.depth.high.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.queue.depth.high.event` | Counter | `{events}` | The number of high queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.queue.depth.high.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.queue.depth.low.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.queue.depth.low.event` | Counter | `{events}` | The number of low queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.queue.depth.low.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.uncommitted.messages`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.uncommitted.messages` | Gauge | `{messages}` | Number of uncommitted messages | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.uncommitted.messages` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.oldest.msg.age`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.oldest.msg.age` | Gauge | `microseconds` | Queue message oldest age | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.oldest.msg.age` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.current.max.queue.filesize`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.current.max.queue.filesize` | Gauge | `mib` | Current maximum queue file size | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.current.max.queue.filesize` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.current.queue.filesize`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.current.queue.filesize` | Gauge | `mib` | Current queue file size | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.current.queue.filesize` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.instances.per.client`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.instances.per.client` | Gauge | `{instances}` | Instances per client | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.instances.per.client` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.message.deq.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.deq.count` | Gauge | `{messages}` | Message dequeue count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.deq.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.message.enq.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.enq.count` | Gauge | `{messages}` | Message enqueue count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.enq.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.queue.depth` | Gauge | `{messages}` | Current queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.service.interval.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.service.interval.event` | Gauge | `1` | Queue service interval event | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.service.interval.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.reusable.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.reusable.log.size` | Gauge | `mib` | The amount of space occupied, in megabytes, by log extents available to be reused. | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.reusable.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.manager.active.channels`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.manager.active.channels` | Gauge | `{channels}` | The queue manager active maximum channels limit | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.manager.active.channels` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.restart.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.restart.log.size` | Gauge | `mib` | Size of the log data required for restart recovery in megabytes. | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.restart.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.max.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.max.queue.depth` | Gauge | `{messages}` | Maximum queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.max.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.onqtime.1`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.onqtime.1` | Gauge | `microseconds` | Amount of time, in microseconds, that a message spent on the queue, over a short period | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.onqtime.1` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.onqtime.2`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.onqtime.2` | Gauge | `microseconds` | Amount of time, in microseconds, that a message spent on the queue, over a longer period | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.onqtime.2` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.message.received.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.received.count` | Gauge | `{messages}` | Number of messages received | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.received.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.message.sent.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.message.sent.count` | Gauge | `{messages}` | Number of messages sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.message.sent.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.max.instances`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.max.instances` | Gauge | `{instances}` | Max channel instances | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.max.instances` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.connection.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.connection.count` | Gauge | `{connections}` | Active connections count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.connection.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.manager.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.manager.status` | Gauge | `1` | Queue manager status | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.manager.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.heartbeat`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.heartbeat` | Gauge | `1` | Queue manager heartbeat | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.heartbeat` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.archive.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.archive.log.size` | Gauge | `mib` | Queue manager archive log size | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.archive.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.manager.max.active.channels`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.manager.max.active.channels` | Gauge | `{channels}` | Queue manager max active channels | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.manager.max.active.channels` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.manager.statistics.interval`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.manager.statistics.interval` | Gauge | `1` | Queue manager statistics interval | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.manager.statistics.interval` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.publish.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.publish.count` | Gauge | `{publications}` | Topic publication count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.publish.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `topic.name` | string | The name of the topic | `dev/` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.subscription.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.subscription.count` | Gauge | `{subscriptions}` | Topic subscription count | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.subscription.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `topic.name` | string | The name of the topic | `dev/` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.listener.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.listener.status` | Gauge | `1` | Listener status | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.listener.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `listener.name` | string | The listener name | `listener` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.unauthorized.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.unauthorized.event` | Counter | `{events}` | Number of authentication error events | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.unauthorized.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `application.name` | string | The application name | `Wordle`; `JMSService` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `user.name` | string | The user name | `foo`; `root` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `mq.manager.max.handles`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `mq.manager.max.handles` | Gauge | `{events}` | Max open handles | ![Development](https://img.shields.io/badge/-development-blue) |


### `mq.manager.max.handles` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |


